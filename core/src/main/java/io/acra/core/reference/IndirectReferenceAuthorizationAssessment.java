package io.acra.core.reference;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.PolicyValidationState;
import java.util.List;

public record IndirectReferenceAuthorizationAssessment(
        String assessmentId,
        String endpoint,
        String referenceFingerprint,
        String referenceKind,
        String resolvedResourceId,
        String action,
        String policyReference,
        AuthorizationDecision expectedDecision,
        AuthorizationDecision observedDecision,
        PolicyValidationState state,
        String confidence,
        List<String> evidenceIds,
        String rationale,
        List<String> reasons) {

    public IndirectReferenceAuthorizationAssessment {
        if (assessmentId == null || assessmentId.isBlank()) throw new IllegalArgumentException("assessmentId required");
        endpoint = endpoint == null ? "" : endpoint;
        referenceFingerprint = referenceFingerprint == null ? "" : referenceFingerprint;
        referenceKind = referenceKind == null ? "" : referenceKind;
        resolvedResourceId = resolvedResourceId == null ? "" : resolvedResourceId;
        action = action == null ? "" : action;
        policyReference = policyReference == null ? "" : policyReference;
        expectedDecision = expectedDecision == null ? AuthorizationDecision.UNKNOWN : expectedDecision;
        observedDecision = observedDecision == null ? AuthorizationDecision.UNKNOWN : observedDecision;
        state = state == null ? PolicyValidationState.INCONCLUSIVE : state;
        confidence = confidence == null ? "INSUFFICIENT" : confidence;
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
        rationale = rationale == null ? "" : rationale;
        reasons = List.copyOf(reasons == null ? List.of() : reasons);
    }

    public boolean violationCandidate() {
        return expectedDecision == AuthorizationDecision.DENY
                && observedDecision == AuthorizationDecision.ALLOW
                && state == PolicyValidationState.CONFLICTING;
    }
}
