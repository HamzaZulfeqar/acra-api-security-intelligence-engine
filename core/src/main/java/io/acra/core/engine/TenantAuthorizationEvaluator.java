package io.acra.core.engine;

import io.acra.core.domain.authorization.AuthorizationContext;
import io.acra.core.domain.authorization.PolicyValidationResult;
import io.acra.core.domain.authorization.TenantAuthorizationAssessment;

/** Tenant-specific adapter over the shared policy-validation engine. */
public final class TenantAuthorizationEvaluator {
    private final PolicyValidationEvaluator policyEvaluator;

    public TenantAuthorizationEvaluator(PolicyValidationEvaluator policyEvaluator) {
        if (policyEvaluator == null) throw new IllegalArgumentException("policyEvaluator required");
        this.policyEvaluator = policyEvaluator;
    }

    public TenantAuthorizationAssessment evaluate(
            AuthorizationContext context,
            PolicyValidationEvaluator.TenantPolicy policy,
            String observationId,
            String executionId,
            String testId,
            String projectId) {
        PolicyValidationResult result = policyEvaluator.reviewTenant(
                context, policy, observationId, executionId, testId, projectId);
        return new TenantAuthorizationAssessment(
                result.reviewId(),
                result.observationId(),
                result.executionId(),
                result.testId(),
                context == null || context.principal() == null ? "" : context.principal().principalId(),
                context == null || context.role() == null ? "" : context.role().roleId(),
                policy == null ? "" : policy.subjectTenantId(),
                policy == null ? "" : policy.resourceTenantId(),
                policy != null && policy.globalAdministrator(),
                policy != null && policy.delegatedTenantPrivilege(),
                result.expectedDecision(),
                result.observedDecision(),
                result.state(),
                result.confidence(),
                result.evidenceIds(),
                result.rationale(),
                result.reasons());
    }
}
