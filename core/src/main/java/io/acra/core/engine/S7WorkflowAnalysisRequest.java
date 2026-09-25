package io.acra.core.engine;

import io.acra.core.domain.authorization.AuthorizationPolicySnapshot;
import io.acra.core.domain.finding.AuthorizationImpactProfile;
import io.acra.core.domain.workflow.WorkflowAuthorizationRequest;
import io.acra.core.domain.workflow.WorkflowPolicySnapshot;

public record S7WorkflowAnalysisRequest(
        WorkflowPolicySnapshot workflowPolicy,
        AuthorizationPolicySnapshot authorizationPolicy,
        WorkflowAuthorizationRequest workflowRequest,
        String projectId,
        String testId,
        String executionId,
        String observationId,
        String endpoint,
        AuthorizationImpactProfile impactProfile) {

    public S7WorkflowAnalysisRequest {
        if (workflowPolicy == null) throw new IllegalArgumentException("workflowPolicy required");
        if (workflowRequest == null) throw new IllegalArgumentException("workflowRequest required");
        projectId = required(projectId, "projectId");
        testId = required(testId, "testId");
        executionId = required(executionId, "executionId");
        observationId = required(observationId, "observationId");
        endpoint = required(endpoint, "endpoint");
        impactProfile = impactProfile == null ? AuthorizationImpactProfile.none() : impactProfile;
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " required");
        return value;
    }
}
