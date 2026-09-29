package io.acra.standalone.model;

import java.util.Objects;
import java.util.UUID;

public record StandaloneCoverageRecord(
        String coverageId,
        UUID targetId,
        String method,
        String endpoint,
        StandaloneCoverageDisposition disposition,
        int expectationCount,
        int passiveObservationCount,
        String reason
) {
    public StandaloneCoverageRecord {
        coverageId = required(coverageId, "coverageId");
        Objects.requireNonNull(targetId, "targetId");
        method = required(method, "method");
        endpoint = required(endpoint, "endpoint");
        disposition = disposition == null ? StandaloneCoverageDisposition.UNTESTED : disposition;
        if (expectationCount < 0 || passiveObservationCount < 0) {
            throw new IllegalArgumentException("coverage counts must be non-negative");
        }
        reason = reason == null ? "" : reason;
    }

    private static String required(String value, String field) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) throw new IllegalArgumentException(field + " is required");
        return normalized;
    }
}
