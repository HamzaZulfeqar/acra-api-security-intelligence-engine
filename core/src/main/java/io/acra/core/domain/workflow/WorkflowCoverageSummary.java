package io.acra.core.domain.workflow;

public record WorkflowCoverageSummary(
        int totalContexts,
        int resolvedContexts,
        int unresolvedContexts,
        int plannedContexts,
        int attemptedContexts,
        int observedContexts) {

    public WorkflowCoverageSummary {
        if (totalContexts < 0 || resolvedContexts < 0 || unresolvedContexts < 0
                || plannedContexts < 0 || attemptedContexts < 0 || observedContexts < 0) {
            throw new IllegalArgumentException("coverage counts cannot be negative");
        }
        if (resolvedContexts + unresolvedContexts != totalContexts) {
            throw new IllegalArgumentException("resolved/unresolved counts must equal total contexts");
        }
        if (plannedContexts > resolvedContexts || attemptedContexts > plannedContexts
                || observedContexts > attemptedContexts) {
            throw new IllegalArgumentException("coverage lifecycle counts are inconsistent");
        }
    }

    public double policyResolutionRatio() {
        return ratio(resolvedContexts, totalContexts);
    }

    public double planningRatio() {
        return ratio(plannedContexts, resolvedContexts);
    }

    public double attemptRatio() {
        return ratio(attemptedContexts, resolvedContexts);
    }

    public double observationRatio() {
        return ratio(observedContexts, resolvedContexts);
    }

    private static double ratio(int numerator, int denominator) {
        return denominator == 0 ? 0.0 : numerator / (double) denominator;
    }
}
