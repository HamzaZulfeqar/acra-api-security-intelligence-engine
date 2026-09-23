package io.acra.core.domain.authorization;

import java.util.List;

public record WorkflowAuthorizationAssessment(
        String assessmentId,
        String fromState,
        String toState,
        String requiredRole,
        String policyReference,
        AuthorizationDecision expectedDecision,
        AuthorizationDecision observedDecision,
        PolicyValidationState state,
        String confidence,
        List<String> evidenceIds,
        String rationale,
        List<String> reasons) {

    public WorkflowAuthorizationAssessment {
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
        reasons = List.copyOf(reasons == null ? List.of() : reasons);
    }

    public boolean violationCandidate() {
        return expectedDecision == AuthorizationDecision.DENY
                && observedDecision == AuthorizationDecision.ALLOW
                && state == PolicyValidationState.CONFLICTING;
    }
}
