package io.acra.core.domain.authorization;

import java.util.List;

public record TenantAuthorizationAssessment(
        String assessmentId,
        String subjectTenantId,
        String resourceTenantId,
        String roleId,
        String policyReference,
        AuthorizationDecision expectedDecision,
        AuthorizationDecision observedDecision,
        PolicyValidationState state,
        String confidence,
        List<String> evidenceIds,
        String rationale,
        List<String> reasons) {

    public TenantAuthorizationAssessment {
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
        reasons = List.copyOf(reasons == null ? List.of() : reasons);
    }

    public boolean violationCandidate() {
        return expectedDecision == AuthorizationDecision.DENY
                && observedDecision == AuthorizationDecision.ALLOW
                && state == PolicyValidationState.CONFLICTING;
    }
}
