package io.acra.core.domain.authorization;

import java.util.List;

public record RoleEscalationAssessment(
        String assessmentId,
        boolean privilegedAction,
        String requiredRoleId,
        List<String> effectiveRoleIds,
        AuthorizationDecision expectedDecision,
        AuthorizationDecision observedDecision,
        RoleEscalationAssessmentState state,
        List<String> evidenceIds,
        String rationale) {

    public RoleEscalationAssessment {
        requiredRoleId = requiredRoleId == null ? "" : requiredRoleId;
        effectiveRoleIds = List.copyOf(effectiveRoleIds == null ? List.of() : effectiveRoleIds);
        expectedDecision = expectedDecision == null ? AuthorizationDecision.UNKNOWN : expectedDecision;
        observedDecision = observedDecision == null ? AuthorizationDecision.UNKNOWN : observedDecision;
        state = state == null ? RoleEscalationAssessmentState.INCONCLUSIVE : state;
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
        rationale = rationale == null ? "" : rationale;
    }
}
