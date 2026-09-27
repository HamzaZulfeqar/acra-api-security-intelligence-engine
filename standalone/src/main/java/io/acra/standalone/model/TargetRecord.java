package io.acra.standalone.model;

import java.net.URI;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record TargetRecord(
        UUID id,
        UUID projectId,
        String displayName,
        URI baseUri,
        String environment,
        String authorizationReference,
        String testingMode,
        Instant createdAt
) {
    private static final int MAX_TEXT = 240;

    public TargetRecord {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(projectId, "projectId");
        displayName = requireText(displayName, "displayName", 120);
        baseUri = validateUri(baseUri);
        environment = enumValue(environment, "environment",
                "LAB", "DEVELOPMENT", "STAGING", "PRODUCTION");
        authorizationReference = requireText(authorizationReference, "authorizationReference", MAX_TEXT);
        testingMode = enumValue(testingMode, "testingMode",
                "PASSIVE", "IMPORT_ONLY", "SAFE_ACTIVE", "CONTROLLED_LAB");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    public static TargetRecord create(
            UUID projectId,
            String displayName,
            String baseUrl,
            String environment,
            String authorizationReference,
            String testingMode
    ) {
        final URI uri;
        try {
            uri = URI.create(requireText(baseUrl, "baseUrl", 2048));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("baseUrl must be a valid absolute HTTP(S) URL", ex);
        }
        return new TargetRecord(
                UUID.randomUUID(),
                projectId,
                displayName,
                uri,
                environment,
                authorizationReference,
                testingMode,
                Instant.now()
        );
    }

    private static URI validateUri(URI uri) {
        Objects.requireNonNull(uri, "baseUri");
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new IllegalArgumentException("target scheme must be http or https");
        }
        if (!uri.isAbsolute() || uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalArgumentException("target must be an absolute URL with a hostname or IP address");
        }
        if (uri.getUserInfo() != null) {
            throw new IllegalArgumentException("credentials must not be embedded in target URLs");
        }
        if (uri.getFragment() != null) {
            throw new IllegalArgumentException("target URL fragments are not supported");
        }
        return uri.normalize();
    }

    private static String enumValue(String value, String field, String... allowed) {
        String normalized = requireText(value, field, 40).toUpperCase(Locale.ROOT);
        for (String candidate : allowed) if (candidate.equals(normalized)) return normalized;
        throw new IllegalArgumentException(field + " is not supported");
    }

    private static String requireText(String value, String field, int max) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) throw new IllegalArgumentException(field + " is required");
        if (normalized.length() > max) throw new IllegalArgumentException(field + " exceeds " + max + " characters");
        return normalized;
    }
}
