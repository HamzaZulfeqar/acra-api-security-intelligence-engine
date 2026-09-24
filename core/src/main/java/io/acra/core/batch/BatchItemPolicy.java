package io.acra.core.batch;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.security.UniversalRedactor;
import java.util.List;

public record BatchItemPolicy(
        String policyReference,
        String policySource,
        String endpoint,
        String resourceId,
        String action,
        String roleId,
        String tenantId,
        AuthorizationDecision expectedDecision,
        List<String> evidenceIds) {

    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public BatchItemPolicy {
        policyReference = requiredSafe(policyReference, "policyReference");
        policySource = requiredSafe(policySource, "policySource");
        endpoint = requiredPath(endpoint);
        resourceId = requiredSafe(resourceId, "resourceId");
        action = requiredSafe(action, "action");
        roleId = normalized(roleId);
        tenantId = normalized(tenantId);
        expectedDecision = expectedDecision == null ? AuthorizationDecision.UNKNOWN : expectedDecision;
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds).stream()
                .map(value -> requiredSafe(value, "evidenceId"))
                .distinct()
                .sorted()
                .toList();
        if (evidenceIds.isEmpty()) throw new IllegalArgumentException("evidenceIds required");
    }

    private static String requiredPath(String value) {
        String path = requiredSafe(value, "endpoint");
        if (!path.startsWith("/") || path.contains("?") || path.contains("#")) {
            throw new IllegalArgumentException("endpoint must be an absolute path without query or fragment");
        }
        return path;
    }

    private static String normalized(String value) {
        return value == null || value.isBlank() ? "UNKNOWN" : requiredSafe(value, "context");
    }

    private static String requiredSafe(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " required");
        String normalized = value.strip();
        if (!normalized.equals(REDACTOR.redactText(normalized))) {
            throw new IllegalArgumentException(name + " contains sensitive material");
        }
        return normalized;
    }
}
