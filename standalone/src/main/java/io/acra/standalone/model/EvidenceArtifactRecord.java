package io.acra.standalone.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record EvidenceArtifactRecord(
        UUID evidenceId,
        UUID projectId,
        UUID targetId,
        String evidenceType,
        String sourceReference,
        String originalSha256,
        boolean redactionApplied,
        int originalLength,
        int storedLength,
        int httpSampleCount,
        Instant createdAt
) {
    public EvidenceArtifactRecord {
        Objects.requireNonNull(evidenceId, "evidenceId");
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(targetId, "targetId");
        evidenceType = required(evidenceType, "evidenceType", 40);
        sourceReference = normalize(sourceReference, 500);
        originalSha256 = required(originalSha256, "originalSha256", 128);
        if (originalLength < 0 || storedLength < 0 || httpSampleCount < 0) {
            throw new IllegalArgumentException("evidence lengths/counts must be non-negative");
        }
        Objects.requireNonNull(createdAt, "createdAt");
    }

    private static String required(String value, String field, int max) {
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
