package io.acra.core.domain.authorization;

import java.util.List;

public record PropertyAuthorizationAssessment(
        String assessmentId,
        String endpoint,
        String property,
        String operation,
        String roleId,
        String tenantId,
        String policyReference,
        AuthorizationDecision expectedDecision,
        AuthorizationDecision observedDecision,
        PolicyValidationState state,
        String confidence,
        List<String> evidenceIds,
        String rationale,
        List<String> reasons) {

    public PropertyAuthorizationAssessment {
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
        reasons = List.copyOf(reasons == null ? List.of() : reasons);
    }

    public boolean violationCandidate() {
        return expectedDecision == AuthorizationDecision.DENY
                && observedDecision == AuthorizationDecision.ALLOW
                && state == PolicyValidationState.CONFLICTING;
    }
}
