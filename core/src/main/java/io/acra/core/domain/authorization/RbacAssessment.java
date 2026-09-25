package io.acra.core.domain.authorization;

import java.util.List;

public record RbacAssessment(
        String assessmentId,
        List<String> effectiveRoleIds,
        List<String> permissionIds,
        AuthorizationDecision expectedDecision,
        AuthorizationDecision observedDecision,
        RbacAssessmentState state,
        List<String> evidenceIds,
        String rationale) {

    public RbacAssessment {
        effectiveRoleIds = List.copyOf(effectiveRoleIds == null ? List.of() : effectiveRoleIds);
        permissionIds = List.copyOf(permissionIds == null ? List.of() : permissionIds);
        expectedDecision = expectedDecision == null ? AuthorizationDecision.UNKNOWN : expectedDecision;
        observedDecision = observedDecision == null ? AuthorizationDecision.UNKNOWN : observedDecision;
        state = state == null ? RbacAssessmentState.INCONCLUSIVE : state;
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
        rationale = rationale == null ? "" : rationale;
    }
}
