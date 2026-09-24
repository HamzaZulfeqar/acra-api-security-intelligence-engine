package io.acra.core.batch;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.PolicyValidationState;
import java.util.List;

public record S10BatchAuthorizationAnalysis(
        String analysisId,
        List<BatchItemAuthorizationAssessment> assessments,
        List<String> evidenceIds,
        List<String> reasons) {

    public S10BatchAuthorizationAnalysis {
        if (analysisId == null || analysisId.isBlank()) throw new IllegalArgumentException("analysisId required");
        assessments = List.copyOf(assessments == null ? List.of() : assessments);
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
        reasons = List.copyOf(reasons == null ? List.of() : reasons);
    }

    public long candidateCount() {
        return assessments.stream().filter(BatchItemAuthorizationAssessment::violationCandidate).count();
    }

    public long inconclusiveCount() {
        return assessments.stream()
                .filter(value -> value.state() == PolicyValidationState.INCONCLUSIVE)
                .count();
    }

    public boolean mixedObservedDecisions() {
        boolean allow = assessments.stream()
                .anyMatch(value -> value.observedDecision() == AuthorizationDecision.ALLOW);
        boolean deny = assessments.stream()
                .anyMatch(value -> value.observedDecision() == AuthorizationDecision.DENY);
        return allow && deny;
    }

    public boolean complete() {
        return !assessments.isEmpty()
                && inconclusiveCount() == 0
                && reasons.stream().noneMatch(value -> value.contains("AMBIGUOUS"));
    }
}
