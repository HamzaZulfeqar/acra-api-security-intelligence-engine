package io.acra.core.property;

public record PropertyAuthorizationCoverageSummary(
        int totalPolicyContexts,
        int readPolicyContexts,
        int updatePolicyContexts,
        int observedContexts,
        int assessedContexts,
        int candidateContexts,
        int rejectedContexts,
        int inconclusiveContexts,
        int unobservedContexts,
        int observedUnassessedContexts) {

    public PropertyAuthorizationCoverageSummary {
        if (totalPolicyContexts < 0 || readPolicyContexts < 0 || updatePolicyContexts < 0
                || observedContexts < 0 || assessedContexts < 0 || candidateContexts < 0
                || rejectedContexts < 0 || inconclusiveContexts < 0 || unobservedContexts < 0
                || observedUnassessedContexts < 0) {
            throw new IllegalArgumentException("coverage counts cannot be negative");
        }
        if (readPolicyContexts + updatePolicyContexts != totalPolicyContexts) {
            throw new IllegalArgumentException("READ/UPDATE counts must equal total property-policy contexts");
        }
        if (observedContexts + unobservedContexts != totalPolicyContexts) {
            throw new IllegalArgumentException("observed/unobserved counts must equal total property-policy contexts");
        }
        if (assessedContexts > observedContexts) {
            throw new IllegalArgumentException("assessed contexts cannot exceed observed contexts");
        }
        if (candidateContexts + rejectedContexts + inconclusiveContexts != assessedContexts) {
            throw new IllegalArgumentException("assessed disposition counts must equal assessed contexts");
        }
        if (observedUnassessedContexts != observedContexts - assessedContexts) {
            throw new IllegalArgumentException("observed-unassessed count is inconsistent");
        }
    }

    public double observationRatio() {
        return ratio(observedContexts, totalPolicyContexts);
    }

    public double assessmentRatio() {
        return ratio(assessedContexts, totalPolicyContexts);
    }

    public double readShare() {
        return ratio(readPolicyContexts, totalPolicyContexts);
    }

    public double updateShare() {
        return ratio(updatePolicyContexts, totalPolicyContexts);
    }

    private static double ratio(int numerator, int denominator) {
        return denominator == 0 ? 0.0 : numerator / (double) denominator;
    }
}
