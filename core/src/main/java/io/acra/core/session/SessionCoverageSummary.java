package io.acra.core.session;

import java.util.List;

public record SessionCoverageSummary(
        int totalTargets,
        int baselineTargets,
        int continuityTargets,
        int rotationTargets,
        int observedTargets,
        int correlatedTargets,
        int assessedTargets,
        int candidateTargets,
        int rejectedTargets,
        int inconclusiveTargets,
        int unobservedTargets,
        int observedUncorrelatedTargets,
        int correlatedUnassessedTargets) {

    public SessionCoverageSummary {
        if (totalTargets < 0 || baselineTargets < 0 || continuityTargets < 0 || rotationTargets < 0
                || observedTargets < 0 || correlatedTargets < 0 || assessedTargets < 0
                || candidateTargets < 0 || rejectedTargets < 0 || inconclusiveTargets < 0
                || unobservedTargets < 0 || observedUncorrelatedTargets < 0
                || correlatedUnassessedTargets < 0) {
            throw new IllegalArgumentException("coverage counts cannot be negative");
        }
        if (baselineTargets + continuityTargets + rotationTargets != totalTargets) {
            throw new IllegalArgumentException("coverage objective counts must equal total targets");
        }
        if (observedTargets + unobservedTargets != totalTargets) {
            throw new IllegalArgumentException("observed/unobserved counts must equal total targets");
        }
        if (correlatedTargets > observedTargets || assessedTargets > correlatedTargets) {
            throw new IllegalArgumentException("coverage lifecycle counts are inconsistent");
        }
        if (candidateTargets + rejectedTargets + inconclusiveTargets != assessedTargets) {
            throw new IllegalArgumentException("assessed disposition counts must equal assessed targets");
        }
        if (observedUncorrelatedTargets != observedTargets - correlatedTargets) {
            throw new IllegalArgumentException("observed-uncorrelated count is inconsistent");
        }
        if (correlatedUnassessedTargets != correlatedTargets - assessedTargets) {
            throw new IllegalArgumentException("correlated-unassessed count is inconsistent");
        }
    }

    public static SessionCoverageSummary fromEntries(List<SessionCoverageEntry> values) {
        List<SessionCoverageEntry> entries = List.copyOf(values == null ? List.of() : values);
        int baseline = 0;
        int continuity = 0;
        int rotation = 0;
        int observed = 0;
        int correlated = 0;
        int assessed = 0;
        int candidate = 0;
        int rejected = 0;
        int inconclusive = 0;
        int unobserved = 0;
        int observedUncorrelated = 0;
        int correlatedUnassessed = 0;

        for (SessionCoverageEntry entry : entries) {
            switch (entry.target().objective()) {
                case BASELINE_CONTEXT -> baseline++;
                case SESSION_CONTINUITY -> continuity++;
                case ROTATION_CONTEXT_STABILITY -> rotation++;
            }
            switch (entry.disposition()) {
                case UNOBSERVED -> unobserved++;
                case OBSERVED_UNCORRELATED -> {
                    observed++;
                    observedUncorrelated++;
                }
                case CORRELATED_UNASSESSED -> {
                    observed++;
                    correlated++;
                    correlatedUnassessed++;
                }
                case CANDIDATE -> {
                    observed++;
                    correlated++;
                    assessed++;
                    candidate++;
                }
                case REJECTED -> {
                    observed++;
                    correlated++;
                    assessed++;
                    rejected++;
                }
                case INCONCLUSIVE -> {
                    observed++;
                    correlated++;
                    assessed++;
                    inconclusive++;
                }
            }
        }

        return new SessionCoverageSummary(
                entries.size(), baseline, continuity, rotation, observed, correlated, assessed,
                candidate, rejected, inconclusive, unobserved, observedUncorrelated, correlatedUnassessed);
    }

    public double observationRatio() {
        return ratio(observedTargets, totalTargets);
    }

    public double correlationRatio() {
        return ratio(correlatedTargets, totalTargets);
    }

    public double assessmentRatio() {
        return ratio(assessedTargets, totalTargets);
    }

    private static double ratio(int numerator, int denominator) {
        return denominator == 0 ? 0.0 : numerator / (double) denominator;
    }
}
