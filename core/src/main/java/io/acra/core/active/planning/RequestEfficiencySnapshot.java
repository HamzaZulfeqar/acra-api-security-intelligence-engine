package io.acra.core.active.planning;

public record RequestEfficiencySnapshot(
        int candidateTests,
        int deduplicatedTests,
        int scopeFilteredTests,
        int budgetFilteredTests,
        int executedTests) {
    public RequestEfficiencySnapshot {
        if (candidateTests < 0 || deduplicatedTests < 0 || scopeFilteredTests < 0
                || budgetFilteredTests < 0 || executedTests < 0) {
            throw new IllegalArgumentException("negative request-efficiency metric");
        }
    }
}
