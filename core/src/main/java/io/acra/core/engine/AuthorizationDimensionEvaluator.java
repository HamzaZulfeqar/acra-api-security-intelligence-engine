package io.acra.core.engine;

import io.acra.core.domain.authorization.AuthorizationContext;
import io.acra.core.domain.authorization.PolicyValidationResult;
import io.acra.core.domain.authorization.PropertyAuthorizationAssessment;
import io.acra.core.domain.authorization.TenantAuthorizationAssessment;
import io.acra.core.domain.authorization.WorkflowAuthorizationAssessment;

public final class AuthorizationDimensionEvaluator {
    private final PolicyValidationEvaluator policyEvaluator;

    public AuthorizationDimensionEvaluator(PolicyValidationEvaluator policyEvaluator) {
        if (policyEvaluator == null) throw new IllegalArgumentException("policyEvaluator required");
        this.policyEvaluator = policyEvaluator;
    }

    public TenantAuthorizationAssessment evaluateTenant(AuthorizationContext context,
            PolicyValidationEvaluator.TenantPolicy policy, String observationId, String executionId,
            String testId, String projectId) {
        PolicyValidationResult result = policyEvaluator.reviewTenant(
                context, policy, observationId, executionId, testId, projectId);
        return new TenantAuthorizationAssessment(result.reviewId(), policy.subjectTenantId(),
                policy.resourceTenantId(), policy.roleId(), result.policyReference(), result.expectedDecision(),
                result.observedDecision(), result.state(), result.confidence(), result.evidenceIds(),
                result.rationale(), result.reasons());
    }

    public WorkflowAuthorizationAssessment evaluateWorkflow(AuthorizationContext context,
            PolicyValidationEvaluator.WorkflowPolicy policy, String currentState, String requestedState,
            boolean approvalProvided, boolean roleSeparationSatisfied, String observationId, String executionId,
            String testId, String projectId) {
        PolicyValidationResult result = policyEvaluator.reviewWorkflow(context, policy, currentState, requestedState,
                approvalProvided, roleSeparationSatisfied, observationId, executionId, testId, projectId);
        return new WorkflowAuthorizationAssessment(result.reviewId(), policy.fromState(), policy.toState(),
                policy.requiredRole(), result.policyReference(), result.expectedDecision(), result.observedDecision(),
                result.state(), result.confidence(), result.evidenceIds(), result.rationale(), result.reasons());
    }

    public PropertyAuthorizationAssessment evaluateProperty(AuthorizationContext context,
            PolicyValidationEvaluator.PropertyPolicy policy, String property,
            PolicyValidationEvaluator.PropertyOperation operation, String endpoint, String observationId,
            String executionId, String testId, String projectId) {
        PolicyValidationResult result = policyEvaluator.reviewProperty(context, policy, property, operation, endpoint,
                observationId, executionId, testId, projectId);
        return new PropertyAuthorizationAssessment(result.reviewId(), policy.endpoint(), policy.property(),
                policy.operation().name(), policy.roleId(), policy.tenantId(), result.policyReference(),
                result.expectedDecision(), result.observedDecision(), result.state(), result.confidence(),
                result.evidenceIds(), result.rationale(), result.reasons());
    }
}
