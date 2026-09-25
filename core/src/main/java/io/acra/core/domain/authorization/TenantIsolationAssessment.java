package io.acra.core.domain.authorization;

import java.util.List;

public record TenantIsolationAssessment(
        String assessmentId,
        TenantRelationship relationship,
        AuthorizationDecision expectedDecision,
        AuthorizationDecision observedDecision,
        TenantIsolationAssessmentState state,
        List<String> evidenceIds,
        String rationale) {

    public TenantIsolationAssessment {
        relationship = relationship == null ? TenantRelationship.UNKNOWN : relationship;
        expectedDecision = expectedDecision == null ? AuthorizationDecision.UNKNOWN : expectedDecision;
        observedDecision = observedDecision == null ? AuthorizationDecision.UNKNOWN : observedDecision;
        state = state == null ? TenantIsolationAssessmentState.INCONCLUSIVE : state;
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
        rationale = rationale == null ? "" : rationale;
    }
}
