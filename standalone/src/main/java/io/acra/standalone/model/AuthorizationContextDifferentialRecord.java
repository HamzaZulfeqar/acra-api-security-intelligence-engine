package io.acra.standalone.model;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record AuthorizationContextDifferentialRecord(
        UUID leftExpectationId,
        UUID rightExpectationId,
        boolean equivalent,
        List<String> changedFields
) {
    public AuthorizationContextDifferentialRecord {
        Objects.requireNonNull(leftExpectationId, "leftExpectationId");
        Objects.requireNonNull(rightExpectationId, "rightExpectationId");
        changedFields = List.copyOf(changedFields == null ? List.of() : changedFields);
    }
}
