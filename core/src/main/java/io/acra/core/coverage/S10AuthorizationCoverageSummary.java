package io.acra.core.coverage;

import java.util.List;

public record S10AuthorizationCoverageSummary(
        int totalPolicyContexts,
        int batchPolicyContexts,
        int indirectPolicyContexts,
        int observedContexts,
        int assessedContexts,
        int candidateContexts,
        int rejectedContexts,
        int inconclusiveContexts,
        int unobservedContexts,
        int observedUnassessedContexts) {

    public S10AuthorizationCoverageSummary {
        if (totalPolicyContexts < 0 || batchPolicyContexts < 0 || indirectPolicyContexts < 0
                || observedContexts < 0 || assessedContexts < 0 || candidateContexts < 0
                || rejectedContexts < 0 || inconclusiveContexts < 0 || unobservedContexts < 0
                || observedUnassessedContexts < 0) {
            throw new IllegalArgumentException("coverage counts cannot be negative");
        }
        if (batchPolicyContexts + indirectPolicyContexts != totalPolicyContexts) {
            throw new IllegalArgumentException("family counts must equal total S10 policy contexts");
        }
        if (observedContexts + unobservedContexts != totalPolicyContexts) {
            throw new IllegalArgumentException("observed/unobserved counts must equal total S10 policy contexts");
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

    public static S10AuthorizationCoverageSummary fromEntries(List<S10AuthorizationCoverageEntry> values) {
        List<S10AuthorizationCoverageEntry> entries =
                List.copyOf(values == null ? List.of() : values);
        int batch = 0;
        int indirect = 0;
        int observed = 0;
        int assessed = 0;
        int candidate = 0;
        int rejected = 0;
        int inconclusive = 0;
        int unobserved = 0;
        int observedUnassessed = 0;

        for (S10AuthorizationCoverageEntry entry : entries) {
            if (entry.family() == S10CoverageFamily.BATCH_ITEM) batch++;
            else indirect++;
            switch (entry.disposition()) {
                case UNOBSERVED -> unobserved++;
                case OBSERVED_UNASSESSED -> {
                    observed++;
                    observedUnassessed++;
                }
                case CANDIDATE -> {
                    observed++;
                    assessed++;
                    candidate++;
                }
                case REJECTED -> {
                    observed++;
                    assessed++;
                    rejected++;
                }
                case INCONCLUSIVE -> {
                    observed++;
                    assessed++;
                    inconclusive++;
                }
            }
        }
        return new S10AuthorizationCoverageSummary(
                entries.size(), batch, indirect, observed, assessed, candidate, rejected,
                inconclusive, unobserved, observedUnassessed);
    }

    public double observationRatio() {
        return ratio(observedContexts, totalPolicyContexts);
    }

    public double assessmentRatio() {
        return ratio(assessedContexts, totalPolicyContexts);
    }

    private static double ratio(int numerator, int denominator) {
        return denominator == 0 ? 0.0 : numerator / (double) denominator;
    }
}
