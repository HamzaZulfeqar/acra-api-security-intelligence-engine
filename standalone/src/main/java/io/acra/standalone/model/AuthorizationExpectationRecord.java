package io.acra.standalone.model;

import io.acra.core.domain.authorization.ActionType;
import io.acra.core.domain.authorization.AuthorizationDecision;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AuthorizationExpectationRecord(
        UUID id,
        UUID projectId,
        UUID targetId,
        String endpoint,
        ActionType action,
        String principalId,
        String roleId,
        String tenantId,
        String resourceId,
        AuthorizationDecision expectedDecision,
        String rationale,
        Instant createdAt
) {
    public AuthorizationExpectationRecord {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(targetId, "targetId");
        endpoint = required(endpoint, "endpoint", 1024);
        action = action == null ? ActionType.UNKNOWN : action;
        if (action == ActionType.UNKNOWN) throw new IllegalArgumentException("action must be explicit");
        principalId = required(principalId, "principalId", 160);
        roleId = normalize(roleId, 160);
        tenantId = normalize(tenantId, 160);
        resourceId = normalize(resourceId, 180);
        expectedDecision = expectedDecision == null ? AuthorizationDecision.UNKNOWN : expectedDecision;
        if (expectedDecision == AuthorizationDecision.ERROR || expectedDecision == AuthorizationDecision.AMBIGUOUS) {
            throw new IllegalArgumentException("expectedDecision must be ALLOW, DENY, CONDITIONAL, or UNKNOWN");
        }
        rationale = normalize(rationale, 1000);
        Objects.requireNonNull(createdAt, "createdAt");
    }

    private static String required(String value, String field, int max) {
        String normalized = normalize(value, max);
        if (normalized.isBlank()) throw new IllegalArgumentException(field + " is required");
        return normalized;
    }

    private static String normalize(String value, int max) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.length() > max) throw new IllegalArgumentException(field + " exceeds " + max + " characters");
        return normalized;
    }
}
