package io.acra.core.engine;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.PolicyResolutionState;
import io.acra.core.domain.workflow.WorkflowAuthorizationResolution;
import io.acra.core.domain.workflow.WorkflowTransitionAssessment;
import io.acra.core.domain.workflow.WorkflowTransitionAssessmentState;
import io.acra.core.security.TokenFingerprint;

public final class S7WorkflowAssessmentEvaluator {

    public WorkflowTransitionAssessment evaluate(WorkflowAuthorizationResolution resolution) {
        if (resolution == null) throw new IllegalArgumentException("resolution required");

        WorkflowTransitionAssessmentState state;
        String rationale;

        if (resolution.state() == PolicyResolutionState.CONFLICTING) {
            state = WorkflowTransitionAssessmentState.CONFLICTING;
            rationale = "Workflow transition policy remains conflicting; no violation candidate is promoted";
        } else if (resolution.state() == PolicyResolutionState.INCOMPLETE
                || resolution.state() == PolicyResolutionState.UNKNOWN
                || resolution.expectedDecision() == AuthorizationDecision.UNKNOWN
                || resolution.observedDecision() == AuthorizationDecision.UNKNOWN) {
            state = WorkflowTransitionAssessmentState.INCONCLUSIVE;
            rationale = "Workflow transition expectation or observation is incomplete";
        } else if (resolution.expectedDecision() == AuthorizationDecision.DENY
                && resolution.observedDecision() == AuthorizationDecision.ALLOW) {
            state = WorkflowTransitionAssessmentState.CANDIDATE;
            rationale = "Observed workflow transition ALLOW conflicts with resolved expected DENY";
        } else {
            state = WorkflowTransitionAssessmentState.NO_VIOLATION;
            rationale = "Observed workflow transition is compatible with resolved policy";
        }

        String material = resolution.resolutionId() + "|" + state;
        return new WorkflowTransitionAssessment(
                "s7-workflow-assessment-" + TokenFingerprint.sha256(material).substring(0, 24),
                resolution.workflowId(),
                resolution.action(),
                resolution.fromState(),
                resolution.toState(),
                resolution.expectedDecision(),
                resolution.observedDecision(),
                state,
                resolution.evidenceIds(),
                resolution.reasons(),
                rationale);
    }
}
