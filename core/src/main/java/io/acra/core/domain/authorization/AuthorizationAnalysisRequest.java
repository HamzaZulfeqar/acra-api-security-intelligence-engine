package io.acra.core.domain.authorization;

import io.acra.core.active.evidence.Observation;
import io.acra.core.domain.finding.AuthorizationImpact;
import io.acra.core.engine.PolicyValidationEvaluator;
import io.acra.core.security.UniversalRedactor;

/** Immutable input for the S5 observation-to-authorization-analysis pipeline. */
public record AuthorizationAnalysisRequest(
        Observation observation,
        String projectId,
        String endpoint,
        PolicyValidationEvaluator.TenantPolicy tenantPolicy,
        PolicyValidationEvaluator.WorkflowPolicy workflowPolicy,
        String currentWorkflowState,
        String requestedWorkflowState,
        boolean approvalProvided,
        boolean roleSeparationSatisfied,
        PolicyValidationEvaluator.PropertyPolicy propertyPolicy,
        String property,
        PolicyValidationEvaluator.PropertyOperation propertyOperation,
        String propertyEndpoint,
        AuthorizationImpact impact) {
    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public AuthorizationAnalysisRequest {
        projectId = safe(projectId);
        endpoint = safe(endpoint);
        currentWorkflowState = safe(currentWorkflowState);
        requestedWorkflowState = safe(requestedWorkflowState);
        property = safe(property);
        propertyEndpoint = safe(propertyEndpoint);
    }

    private static String safe(String value) {
        return REDACTOR.redactText(value == null ? "" : value);
    }
}
