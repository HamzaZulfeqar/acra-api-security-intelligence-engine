package io.acra.standalone.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CandidateReviewRecord(
        UUID projectId,
        String candidateId,
        CandidateReviewState state,
        String note,
        Instant updatedAt
) {
    public CandidateReviewRecord {
        Objects.requireNonNull(projectId, "projectId");
        candidateId = required(candidateId, "candidateId", 180);
        state = state == null ? CandidateReviewState.NEW : state;
        note = normalize(note, 1000);
        Objects.requireNonNull(updatedAt, "updatedAt");
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
