package io.acra.core.property;

import io.acra.core.domain.authorization.PolicyValidationState;
import io.acra.core.domain.authorization.PropertyAuthorizationAssessment;
import java.util.List;

public record S9PropertyAuthorizationAnalysis(
        String analysisId,
        List<PropertyAuthorizationAssessment> assessments,
        List<String> evidenceIds,
        List<String> reasons) {

    public S9PropertyAuthorizationAnalysis {
        if (analysisId == null || analysisId.isBlank()) throw new IllegalArgumentException("analysisId required");
        assessments = List.copyOf(assessments == null ? List.of() : assessments);
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
        reasons = List.copyOf(reasons == null ? List.of() : reasons);
    }

    public long candidateCount() {
        return assessments.stream().filter(PropertyAuthorizationAssessment::violationCandidate).count();
    }

    public long inconclusiveCount() {
        return assessments.stream()
                .filter(value -> value.state() == PolicyValidationState.INCONCLUSIVE)
                .count();
    }

    public long policyConflictCount() {
        return assessments.stream()
                .filter(value -> value.reasons().contains("PROPERTY_POLICY_AMBIGUOUS"))
                .count();
    }

    public boolean complete() {
        return !assessments.isEmpty()
                && inconclusiveCount() == 0
                && policyConflictCount() == 0;
    }
}
