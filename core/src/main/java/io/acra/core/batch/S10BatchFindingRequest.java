package io.acra.core.batch;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.security.UniversalRedactor;
import java.util.LinkedHashSet;
import java.util.List;

public record S10BatchFindingRequest(
        String projectId,
        String testId,
        String executionId,
        String observationId,
        String endpoint,
        String batchId,
        String itemKey,
        String resourceId,
        String action,
        String principalId,
        String tenantId,
        String policyReference,
        AuthorizationDecision expectedDecision,
        AuthorizationDecision observedDecision,
        List<String> evidenceIds) {

    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public S10BatchFindingRequest {
        projectId = required(projectId, "projectId");
        testId = required(testId, "testId");
        executionId = required(executionId, "executionId");
        observationId = required(observationId, "observationId");
        endpoint = requiredPath(endpoint);
        batchId = required(batchId, "batchId");
        itemKey = required(itemKey, "itemKey");
        resourceId = required(resourceId, "resourceId");
        action = required(action, "action");
        principalId = required(principalId, "principalId");
        tenantId = required(tenantId, "tenantId");
        policyReference = required(policyReference, "policyReference");
        expectedDecision = expectedDecision == null ? AuthorizationDecision.UNKNOWN : expectedDecision;
        observedDecision = observedDecision == null ? AuthorizationDecision.UNKNOWN : observedDecision;
        evidenceIds = safeEvidence(evidenceIds);
    }

    private static List<String> safeEvidence(List<String> values) {
        List<String> safe = List.copyOf(values == null ? List.of() : values).stream()
                .map(value -> required(value, "evidenceId"))
                .toList();
        if (safe.isEmpty()) throw new IllegalArgumentException("evidenceIds required");
        if (new LinkedHashSet<>(safe).size() != safe.size()) {
            throw new IllegalArgumentException("duplicate evidenceIds");
        }
        return safe;
    }

    private static String requiredPath(String value) {
        String path = required(value, "endpoint");
        if (!path.startsWith("/") || path.contains("?") || path.contains("#")) {
            throw new IllegalArgumentException("endpoint must be an absolute path without query or fragment");
        }
        return path;
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " required");
        String normalized = value.strip();
        if (!normalized.equals(REDACTOR.redactText(normalized))) {
            throw new IllegalArgumentException(name + " contains sensitive material");
        }
        return normalized;
    }
}
