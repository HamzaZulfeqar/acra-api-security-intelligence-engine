package io.acra.core.property;

import io.acra.core.engine.PolicyValidationEvaluator;
import java.util.List;

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

    public static PropertyAuthorizationCoverageSummary fromEntries(
            List<PropertyAuthorizationCoverageEntry> values) {
        List<PropertyAuthorizationCoverageEntry> entries =
                List.copyOf(values == null ? List.of() : values);
        int read = 0;
        int update = 0;
        int observed = 0;
        int assessed = 0;
        int candidate = 0;
        int rejected = 0;
        int inconclusive = 0;
        int unobserved = 0;
        int observedUnassessed = 0;

        for (PropertyAuthorizationCoverageEntry entry : entries) {
            if (entry.operation() == PolicyValidationEvaluator.PropertyOperation.READ) read++;
            else update++;
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
        return new PropertyAuthorizationCoverageSummary(
                entries.size(), read, update, observed, assessed, candidate, rejected,
                inconclusive, unobserved, observedUnassessed);
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
