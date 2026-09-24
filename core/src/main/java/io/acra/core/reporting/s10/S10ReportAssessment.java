package io.acra.core.reporting.s10;

import io.acra.core.coverage.S10CoverageFamily;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.PolicyValidationState;
import java.util.List;

public record S10ReportAssessment(
        S10CoverageFamily family,
        String assessmentId,
        String endpoint,
        String resourceId,
        String action,
        String policyReference,
        AuthorizationDecision expectedDecision,
        AuthorizationDecision observedDecision,
        PolicyValidationState state,
        String confidence,
        List<String> evidenceIds,
        List<String> reasons) {

    public S10ReportAssessment {
        if (family == null) throw new IllegalArgumentException("family required");
        assessmentId = required(assessmentId, "assessmentId");
        endpoint = required(endpoint, "endpoint");
        resourceId = required(resourceId, "resourceId");
        action = required(action, "action");
        policyReference = required(policyReference, "policyReference");
        expectedDecision = expectedDecision == null ? AuthorizationDecision.UNKNOWN : expectedDecision;
        observedDecision = observedDecision == null ? AuthorizationDecision.UNKNOWN : observedDecision;
        state = state == null ? PolicyValidationState.INCONCLUSIVE : state;
        confidence = confidence == null || confidence.isBlank() ? "INSUFFICIENT" : confidence.strip();
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
        reasons = List.copyOf(reasons == null ? List.of() : reasons);
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " required");
        return value.strip();
    }
}
