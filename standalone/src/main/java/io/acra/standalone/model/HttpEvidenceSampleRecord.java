package io.acra.standalone.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record HttpEvidenceSampleRecord(
        UUID sampleId,
        UUID evidenceId,
        UUID projectId,
        UUID targetId,
        String method,
        String requestUrl,
        int responseStatus,
        String responseContentType,
        String requestBodyRedacted,
        String responseBodyRedacted,
        Instant createdAt
) {
    public HttpEvidenceSampleRecord {
        Objects.requireNonNull(sampleId, "sampleId");
        Objects.requireNonNull(evidenceId, "evidenceId");
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(targetId, "targetId");
        method = normalize(method, 32);
        requestUrl = normalize(requestUrl, 4096);
        if (responseStatus < 0 || responseStatus > 599) throw new IllegalArgumentException("responseStatus must be 0 or 100-599");
        responseContentType = normalize(responseContentType, 240);
        requestBodyRedacted = normalize(requestBodyRedacted, 262144);
        responseBodyRedacted = normalize(responseBodyRedacted, 262144);
        Objects.requireNonNull(createdAt, "createdAt");
    }

    public boolean hasResponse() {
        return responseStatus >= 100;
    }

    private static String normalize(String value, int max) {
        String normalized = value == null ? "" : value;
        if (normalized.length() > max) throw new IllegalArgumentException("value exceeds " + max + " characters");
        return normalized;
    }
}
