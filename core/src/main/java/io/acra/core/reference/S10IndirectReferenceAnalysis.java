package io.acra.core.reference;

import io.acra.core.domain.authorization.PolicyValidationState;
import java.util.List;

public record S10IndirectReferenceAnalysis(
        String analysisId,
        List<IndirectReferenceAuthorizationAssessment> assessments,
        List<String> evidenceIds,
        List<String> reasons) {

    public S10IndirectReferenceAnalysis {
        if (analysisId == null || analysisId.isBlank()) throw new IllegalArgumentException("analysisId required");
        assessments = List.copyOf(assessments == null ? List.of() : assessments);
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
        reasons = List.copyOf(reasons == null ? List.of() : reasons);
    }

    public long candidateCount() {
        return assessments.stream().filter(IndirectReferenceAuthorizationAssessment::violationCandidate).count();
    }

    public long inconclusiveCount() {
        return assessments.stream()
                .filter(value -> value.state() == PolicyValidationState.INCONCLUSIVE)
                .count();
    }

    public boolean resolutionConflict() {
        return reasons.contains("INDIRECT_REFERENCE_RESOLUTION_CONFLICT");
    }

    public boolean complete() {
        return !assessments.isEmpty() && inconclusiveCount() == 0 && !resolutionConflict();
    }
}
