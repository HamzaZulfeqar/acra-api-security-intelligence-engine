package io.acra.core.batch;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.PolicyValidationState;
import java.util.List;

public record BatchItemAuthorizationAssessment(
        String assessmentId,
        String batchId,
        String itemKey,
        String endpoint,
        String resourceId,
        String action,
        String policyReference,
        AuthorizationDecision expectedDecision,
        AuthorizationDecision observedDecision,
        PolicyValidationState state,
        String confidence,
        List<String> evidenceIds,
        String rationale,
        List<String> reasons) {

    public BatchItemAuthorizationAssessment {
        if (assessmentId == null || assessmentId.isBlank()) throw new IllegalArgumentException("assessmentId required");
        if (batchId == null || batchId.isBlank()) throw new IllegalArgumentException("batchId required");
        if (itemKey == null || itemKey.isBlank()) throw new IllegalArgumentException("itemKey required");
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
        reasons = List.copyOf(reasons == null ? List.of() : reasons);
        expectedDecision = expectedDecision == null ? AuthorizationDecision.UNKNOWN : expectedDecision;
        observedDecision = observedDecision == null ? AuthorizationDecision.UNKNOWN : observedDecision;
        state = state == null ? PolicyValidationState.INCONCLUSIVE : state;
        confidence = confidence == null ? "INSUFFICIENT" : confidence;
        rationale = rationale == null ? "" : rationale;
        endpoint = endpoint == null ? "" : endpoint;
        resourceId = resourceId == null ? "" : resourceId;
        action = action == null ? "" : action;
        policyReference = policyReference == null ? "" : policyReference;
    }

    public boolean violationCandidate() {
        return expectedDecision == AuthorizationDecision.DENY
                && observedDecision == AuthorizationDecision.ALLOW
                && state == PolicyValidationState.CONFLICTING;
    }
}
