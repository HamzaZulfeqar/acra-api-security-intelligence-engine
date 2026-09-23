package io.acra.core.engine;

import io.acra.core.domain.authorization.AuthorizationContext;
import io.acra.core.domain.authorization.PolicyValidationResult;
import io.acra.core.domain.authorization.PropertyAuthorizationAssessment;

/** Property-specific adapter over the shared policy-validation engine. */
public final class PropertyAuthorizationEvaluator {
    private final PolicyValidationEvaluator policyEvaluator;

    public PropertyAuthorizationEvaluator(PolicyValidationEvaluator policyEvaluator) {
        if (policyEvaluator == null) throw new IllegalArgumentException("policyEvaluator required");
        this.policyEvaluator = policyEvaluator;
    }

    public PropertyAuthorizationAssessment evaluate(
            AuthorizationContext context,
            PolicyValidationEvaluator.PropertyPolicy policy,
            String property,
            PolicyValidationEvaluator.PropertyOperation operation,
            String endpoint,
            String observationId,
            String executionId,
            String testId,
            String projectId) {
        PolicyValidationResult result = policyEvaluator.reviewProperty(
                context, policy, property, operation, endpoint, observationId, executionId, testId, projectId);
        return new PropertyAuthorizationAssessment(
                result.reviewId(),
                result.observationId(),
                result.executionId(),
                result.testId(),
                context == null || context.principal() == null ? "" : context.principal().principalId(),
                context == null || context.role() == null ? "" : context.role().roleId(),
                context == null || context.tenant() == null ? "" : context.tenant().tenantId(),
                endpoint,
                property,
                operation,
                result.expectedDecision(),
                result.observedDecision(),
                result.state(),
                result.confidence(),
                result.evidenceIds(),
                result.rationale(),
                result.reasons());
    }
}
