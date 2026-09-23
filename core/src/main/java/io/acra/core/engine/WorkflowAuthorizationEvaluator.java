package io.acra.core.engine;

import io.acra.core.domain.authorization.AuthorizationContext;
import io.acra.core.domain.authorization.PolicyValidationResult;
import io.acra.core.domain.authorization.WorkflowAuthorizationAssessment;

/** Workflow-specific adapter over the shared policy-validation engine. */
public final class WorkflowAuthorizationEvaluator {
    private final PolicyValidationEvaluator policyEvaluator;

    public WorkflowAuthorizationEvaluator(PolicyValidationEvaluator policyEvaluator) {
        if (policyEvaluator == null) throw new IllegalArgumentException("policyEvaluator required");
        this.policyEvaluator = policyEvaluator;
    }

    public WorkflowAuthorizationAssessment evaluate(
            AuthorizationContext context,
            PolicyValidationEvaluator.WorkflowPolicy policy,
            String currentState,
            String requestedState,
            boolean approvalProvided,
            boolean roleSeparationSatisfied,
            String observationId,
            String executionId,
            String testId,
            String projectId) {
        PolicyValidationResult result = policyEvaluator.reviewWorkflow(
                context, policy, currentState, requestedState, approvalProvided, roleSeparationSatisfied,
                observationId, executionId, testId, projectId);
        return new WorkflowAuthorizationAssessment(
                result.reviewId(),
                result.observationId(),
                result.executionId(),
                result.testId(),
                context == null || context.principal() == null ? "" : context.principal().principalId(),
                context == null || context.role() == null ? "" : context.role().roleId(),
                currentState,
                requestedState,
                approvalProvided,
                roleSeparationSatisfied,
                result.expectedDecision(),
                result.observedDecision(),
                result.state(),
                result.confidence(),
                result.evidenceIds(),
                result.rationale(),
                result.reasons());
    }
}
