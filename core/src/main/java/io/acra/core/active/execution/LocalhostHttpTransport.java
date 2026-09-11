package io.acra.core.active.execution;

import io.acra.core.active.evidence.RequestSnapshot;
import io.acra.core.active.model.ExecutionEnvironment;
import io.acra.core.active.model.TargetDescriptor;
import io.acra.core.domain.http.HttpHeader;
import io.acra.core.domain.http.HttpProtocol;
import io.acra.core.domain.http.HttpResponse;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Concrete Sprint 4 transport restricted to one configured loopback ACRA-Lab target.
 * Redirects are deliberately disabled so a localhost response cannot move execution
 * outside the configured authority.
 */
public final class LocalhostHttpTransport implements HttpTransport {
    private static final Set<String> LOOPBACK_HOSTS = Set.of("localhost", "127.0.0.1", "::1");
    private static final Set<String> SYNTHESIZED_OR_HOP_BY_HOP = Set.of(
            "host", "content-length", "connection", "transfer-encoding", "upgrade", "expect");

    private final TargetDescriptor target;
    private final HttpClient client;

    public LocalhostHttpTransport(TargetDescriptor target) {
        this(target, HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .version(HttpClient.Version.HTTP_1_1)
                .build());
    }

    LocalhostHttpTransport(TargetDescriptor target, HttpClient client) {
        if (target == null || client == null) throw new IllegalArgumentException("target and HTTP client required");
        if (target.environment() != ExecutionEnvironment.LAB) {
            throw new IllegalArgumentException("localhost transport requires LAB environment");
        }
        if (!target.authorized()) throw new IllegalArgumentException("localhost transport requires an authorized target");
        if (!LOOPBACK_HOSTS.contains(target.host().toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("localhost transport requires a loopback host");
        }
        this.target = target;
        this.client = client;
    }

    @Override
    public TransportResult send(RequestSnapshot snapshot, Duration timeout, CancellationToken cancellation) throws Exception {
        if (snapshot == null || timeout == null || timeout.isZero() || timeout.isNegative() || cancellation == null) {
            throw new IllegalArgumentException("request snapshot, positive timeout and cancellation token required");
        }
        cancellation.throwIfCancelled();
        var request = snapshot.request();
        validateRequest(request);

        URI uri = URI.create(authority() + request.rawTarget());
        var builder = java.net.http.HttpRequest.newBuilder(uri).timeout(timeout);
        boolean hasCookieHeader = false;
        for (HttpHeader header : request.headers()) {
            String lower = header.name().toLowerCase(Locale.ROOT);
            if (SYNTHESIZED_OR_HOP_BY_HOP.contains(lower)) continue;
            if (lower.equals("cookie")) {
                hasCookieHeader = true;
                if (!request.cookies().isEmpty()) continue;
            }
            builder.header(header.name(), header.value());
        }
        if (!request.cookies().isEmpty()) {
            builder.header("Cookie", cookieHeader(request.cookies()));
        } else if (hasCookieHeader) {
            // The original Cookie header was already emitted above when no canonical cookie map exists.
        }

        byte[] body = request.body();
        builder.method(request.method().name(), body.length == 0 ? BodyPublishers.noBody() : BodyPublishers.ofByteArray(body));

        long started = System.nanoTime();
        java.net.http.HttpResponse<byte[]> live = client.send(builder.build(), BodyHandlers.ofByteArray());
        Duration timing = Duration.ofNanos(Math.max(0L, System.nanoTime() - started));
        cancellation.throwIfCancelled();

        List<HttpHeader> headers = new ArrayList<>();
        live.headers().map().forEach((name, values) -> values.forEach(value -> headers.add(new HttpHeader(name, value))));
        String contentType = live.headers().firstValue("Content-Type").orElse("");
        HttpResponse response = new HttpResponse(live.statusCode(), headers, live.body(), contentType,
                protocol(live.version()), new byte[0]);
        return new TransportResult(response, responseCookies(live.headers().allValues("Set-Cookie")), timing);
    }

    private void validateRequest(io.acra.core.domain.http.HttpRequest request) {
        if (!request.scheme().equalsIgnoreCase(target.scheme())
                || !request.host().equalsIgnoreCase(target.host())
                || request.port() != target.port()) {
            throw new IllegalArgumentException("request authority does not match configured ACRA-Lab target");
        }
        if (!LOOPBACK_HOSTS.contains(request.host().toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("external hosts are forbidden by localhost transport");
        }
        String rawTarget = request.rawTarget();
        if (!rawTarget.startsWith("/") || rawTarget.contains("://") || rawTarget.indexOf('#') >= 0) {
            throw new IllegalArgumentException("request target must remain local and origin-form");
        }
    }

    private String authority() {
        String host = target.host().contains(":") ? '[' + target.host() + ']' : target.host();
        return target.scheme() + "://" + host + ':' + target.port();
    }

    private static String cookieHeader(Map<String, String> cookies) {
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            if (out.length() > 0) out.append("; ");
            out.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return out.toString();
    }

    private static Map<String, String> responseCookies(List<String> setCookieHeaders) {
        Map<String, String> cookies = new LinkedHashMap<>();
        for (String header : setCookieHeaders) {
            int semicolon = header.indexOf(';');
            String pair = semicolon >= 0 ? header.substring(0, semicolon) : header;
            int equals = pair.indexOf('=');
            if (equals > 0) cookies.put(pair.substring(0, equals).trim(), pair.substring(equals + 1).trim());
        }
        return cookies;
    }

    private static HttpProtocol protocol(HttpClient.Version version) {
        return version == HttpClient.Version.HTTP_2 ? HttpProtocol.HTTP_2 : HttpProtocol.HTTP_1_1;
    }
}
