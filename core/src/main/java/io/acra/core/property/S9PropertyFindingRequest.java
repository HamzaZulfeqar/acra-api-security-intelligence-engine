package io.acra.core.property;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.security.UniversalRedactor;
import java.util.List;

public record S9PropertyFindingRequest(
        String projectId,
        String testId,
        String executionId,
        String observationId,
        String endpoint,
        String resourceId,
        String principalId,
        String tenantId,
        String policyReference,
        AuthorizationDecision expectedDecision,
        AuthorizationDecision observedDecision,
        List<String> evidenceIds) {

    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public S9PropertyFindingRequest {
        projectId = required(projectId, "projectId");
        testId = required(testId, "testId");
        executionId = required(executionId, "executionId");
        observationId = required(observationId, "observationId");
        endpoint = required(endpoint, "endpoint");
        resourceId = required(resourceId, "resourceId");
        principalId = required(principalId, "principalId");
        tenantId = required(tenantId, "tenantId");
        policyReference = required(policyReference, "policyReference");
        expectedDecision = expectedDecision == null ? AuthorizationDecision.UNKNOWN : expectedDecision;
        observedDecision = observedDecision == null ? AuthorizationDecision.UNKNOWN : observedDecision;
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds).stream()
                .map(value -> required(value, "evidenceId"))
                .toList();
        if (evidenceIds.isEmpty()) throw new IllegalArgumentException("evidenceIds required");
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
