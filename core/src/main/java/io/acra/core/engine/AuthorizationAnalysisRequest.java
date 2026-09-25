package io.acra.core.engine;

import io.acra.core.domain.authorization.AuthorizationContext;
import io.acra.core.domain.finding.AuthorizationImpactProfile;

public record AuthorizationAnalysisRequest(
        AuthorizationContext context,
        String endpoint,
        String policyReference,
        String projectId,
        String observationId,
        String executionId,
        String testId,
        PolicyValidationEvaluator.TenantPolicy tenantPolicy,
        PolicyValidationEvaluator.WorkflowPolicy workflowPolicy,
        String currentWorkflowState,
        String requestedWorkflowState,
        boolean approvalProvided,
        boolean roleSeparationSatisfied,
        PolicyValidationEvaluator.PropertyPolicy propertyPolicy,
        String property,
        PolicyValidationEvaluator.PropertyOperation propertyOperation,
        AuthorizationImpactProfile impactProfile) {

    public AuthorizationAnalysisRequest {
        impactProfile = impactProfile == null ? AuthorizationImpactProfile.none() : impactProfile;
    }
}
