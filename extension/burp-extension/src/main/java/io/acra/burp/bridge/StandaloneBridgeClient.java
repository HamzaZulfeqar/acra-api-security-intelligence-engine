package io.acra.burp.bridge;

import io.acra.core.openapi.MiniJson;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class StandaloneBridgeClient {
    private static final Set<String> LOOPBACK = Set.of("localhost", "127.0.0.1", "::1", "[::1]");
    private static final int MAX_RAW_REQUEST_CHARS = 2 * 1024 * 1024;

    private final HttpClient http;
    private final URI baseUri;

    public StandaloneBridgeClient(String baseUrl) {
        this.baseUri = validateBase(baseUrl);
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public URI baseUri() {
        return baseUri;
    }

    public StandaloneBridgeResult probe() {
        try {
            Health health = health();
            return new StandaloneBridgeResult(
                    true,
                    "ACRA standalone is reachable; Core linked=" + health.coreLinked(),
                    0, 0, 0);
        } catch (Exception ex) {
            return new StandaloneBridgeResult(false, safeMessage(ex), 0, 0, 0);
        }
    }

    public StandaloneBridgeResult sendRawRequest(
            String projectId,
            String targetId,
            String sourceReference,
            String rawRequest
    ) {
        requireText(projectId, "projectId", 128);
        requireText(targetId, "targetId", 128);
        sourceReference = normalize(sourceReference, 240);
        rawRequest = requireText(rawRequest, "rawRequest", MAX_RAW_REQUEST_CHARS);

        try {
            Health health = health();
            String form = form(
                    "projectId", projectId,
                    "targetId", targetId,
                    "importType", "RAW_HTTP",
                    "sourceReference", sourceReference,
                    "content", rawRequest);

            HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("api/import"))
                    .timeout(Duration.ofSeconds(6))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("X-ACRA-CSRF", health.csrfToken())
                    .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return new StandaloneBridgeResult(false,
                        "Standalone rejected handoff: HTTP " + response.statusCode() + " " + responseError(response.body()),
                        0, 0, 0);
            }

            Map<?, ?> json = object(MiniJson.parse(response.body()), "import response");
            return new StandaloneBridgeResult(
                    true,
                    "Transaction imported into standalone ACRA.",
                    integer(json.get("observations")),
                    integer(json.get("uniqueEndpoints")),
                    integer(json.get("inventorySize")));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new StandaloneBridgeResult(false, "Bridge interrupted.", 0, 0, 0);
        } catch (Exception ex) {
            return new StandaloneBridgeResult(false, safeMessage(ex), 0, 0, 0);
        }
    }

    private Health health() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("api/health"))
                .timeout(Duration.ofSeconds(4))
                .GET()
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IllegalStateException("health probe returned HTTP " + response.statusCode());
        }
        Map<?, ?> json = object(MiniJson.parse(response.body()), "health response");
        if (!"UP".equals(String.valueOf(json.get("status")))) {
            throw new IllegalStateException("standalone health status is not UP");
        }
        String token = String.valueOf(json.get("csrfToken"));
        if (token.isBlank() || "null".equals(token)) throw new IllegalStateException("standalone CSRF token unavailable");
        return new Health(token, Boolean.TRUE.equals(json.get("coreLinked")));
    }

    private static URI validateBase(String value) {
        String normalized = requireText(value, "standaloneUrl", 2048);
        URI uri;
        try {
            uri = URI.create(normalized.endsWith("/") ? normalized : normalized + "/").normalize();
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("standaloneUrl is invalid", ex);
        }
        if (!"http".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("standalone bridge requires local HTTP management URL");
        }
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if (!LOOPBACK.contains(host)) {
            throw new IllegalArgumentException("standalone bridge URL must be loopback");
        }
        if (uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException("standalone bridge URL must not contain credentials, query, or fragment");
        }
        return uri;
    }

    private static String form(String... values) {
        if (values.length % 2 != 0) throw new IllegalArgumentException("form key/value pairs required");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < values.length; i += 2) {
            if (out.length() > 0) out.append('&');
            out.append(enc(values[i])).append('=').append(enc(values[i + 1]));
        }
        return out.toString();
    }

    private static String enc(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static Map<?, ?> object(Object value, String label) {
        if (!(value instanceof Map<?, ?> map)) throw new IllegalArgumentException(label + " must be a JSON object");
        return map;
    }

    private static int integer(Object value) {
        if (value instanceof Number number) return number.intValue();
        return Integer.parseInt(String.valueOf(value));
    }

    private static String responseError(String body) {
        try {
            Map<?, ?> json = object(MiniJson.parse(body), "error response");
            Object error = json.get("error");
            return error == null ? "" : String.valueOf(error);
        } catch (RuntimeException ignored) {
            return "request rejected";
        }
    }

    private static String safeMessage(Exception ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? ex.getClass().getSimpleName() : message;
    }

    private static String requireText(String value, String field, int max) {
        String normalized = normalize(value, max);
        if (normalized.isBlank()) throw new IllegalArgumentException(field + " is required");
        return normalized;
    }

    private static String normalize(String value, int max) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.length() > max) throw new IllegalArgumentException("value exceeds " + max + " characters");
        return normalized;
    }

    private record Health(String csrfToken, boolean coreLinked) {}
}
