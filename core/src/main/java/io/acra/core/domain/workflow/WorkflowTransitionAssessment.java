package io.acra.core.domain.workflow;

import io.acra.core.domain.authorization.AuthorizationDecision;
import java.util.List;

public record WorkflowTransitionAssessment(
        String assessmentId,
        String workflowId,
        String action,
        String fromState,
        String toState,
        AuthorizationDecision expectedDecision,
        AuthorizationDecision observedDecision,
        WorkflowTransitionAssessmentState state,
        List<String> evidenceIds,
        List<String> reasons,
        String rationale) {

    public WorkflowTransitionAssessment {
        assessmentId = assessmentId == null ? "" : assessmentId;
        workflowId = workflowId == null ? "" : workflowId;
        action = action == null ? "" : action;
        fromState = fromState == null ? "" : fromState;
        toState = toState == null ? "" : toState;
        expectedDecision = expectedDecision == null ? AuthorizationDecision.UNKNOWN : expectedDecision;
        observedDecision = observedDecision == null ? AuthorizationDecision.UNKNOWN : observedDecision;
        state = state == null ? WorkflowTransitionAssessmentState.INCONCLUSIVE : state;
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds).stream()
                .filter(value -> value != null && !value.isBlank()).distinct().sorted().toList();
        reasons = List.copyOf(reasons == null ? List.of() : reasons).stream()
                .filter(value -> value != null && !value.isBlank()).distinct().sorted().toList();
        rationale = rationale == null ? "" : rationale;
    }

    public boolean violationCandidate() {
        return state == WorkflowTransitionAssessmentState.CANDIDATE
                && expectedDecision == AuthorizationDecision.DENY
                && observedDecision == AuthorizationDecision.ALLOW;
    }
}
