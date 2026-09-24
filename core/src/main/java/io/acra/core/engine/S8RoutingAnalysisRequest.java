package io.acra.core.engine;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.route.RouteBoundaryTransition;
import java.util.List;

public record S8RoutingAnalysisRequest(
        String projectId,
        String testId,
        String executionId,
        String observationId,
        String endpoint,
        String resourceId,
        String principalId,
        String tenantId,
        String policyReference,
        RouteBoundaryTransition transition,
        AuthorizationDecision expectedDecision,
        AuthorizationDecision observedDecision,
        List<String> evidenceIds) {

    public S8RoutingAnalysisRequest {
        projectId = required(projectId, "projectId");
        testId = required(testId, "testId");
        executionId = required(executionId, "executionId");
        observationId = required(observationId, "observationId");
        endpoint = required(endpoint, "endpoint");
        resourceId = required(resourceId, "resourceId");
        principalId = required(principalId, "principalId");
        tenantId = required(tenantId, "tenantId");
        policyReference = required(policyReference, "policyReference");
        if (transition == null) throw new IllegalArgumentException("transition required");
        expectedDecision = expectedDecision == null ? AuthorizationDecision.UNKNOWN : expectedDecision;
        observedDecision = observedDecision == null ? AuthorizationDecision.UNKNOWN : observedDecision;
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds).stream()
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .sorted()
                .toList();
        if (evidenceIds.isEmpty()) throw new IllegalArgumentException("evidenceIds required");
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " required");
        return value;
    }
}
