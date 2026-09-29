package io.acra.core.imports;

import io.acra.core.domain.http.HttpMethod;

import java.net.URI;
import java.util.Locale;
import java.util.Objects;

public record ImportedEndpointObservation(
        HttpMethod method,
        URI uri,
        int responseStatus,
        String sourceType,
        String sourceReference
) {
    public ImportedEndpointObservation {
        Objects.requireNonNull(method, "method");
        uri = validateUri(uri);
        if (responseStatus < 0 || responseStatus > 599) {
            throw new IllegalArgumentException("responseStatus must be 0 or 100-599");
        }
        sourceType = requireText(sourceType, "sourceType", 40).toUpperCase(Locale.ROOT);
        sourceReference = normalize(sourceReference, 500);
    }

    public String rawPath() {
        String path = uri.getRawPath();
        return path == null || path.isBlank() ? "/" : path;
    }

    public int effectivePort() {
        if (uri.getPort() > 0) return uri.getPort();
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private static URI validateUri(URI value) {
        Objects.requireNonNull(value, "uri");
        if (!value.isAbsolute() || value.getHost() == null || value.getHost().isBlank()) {
            throw new IllegalArgumentException("imported endpoint URI must be absolute and include a host");
        }
        String scheme = value.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new IllegalArgumentException("imported endpoint URI scheme must be http or https");
        }
        if (value.getUserInfo() != null) {
            throw new IllegalArgumentException("imported endpoint URI must not embed credentials");
        }
        return value.normalize();
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
}
