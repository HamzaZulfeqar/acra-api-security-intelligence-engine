package io.acra.core.active.planning;

public record PlanningMetrics(
        int candidateTests,
        int eligibleTests,
        int deduplicatedTests,
        int scopeFilteredTests,
        int selectionFilteredTests,
        int budgetFilteredTests,
        int plannedTests,
        int estimatedRequests) {
    public PlanningMetrics {
        if (candidateTests < 0 || eligibleTests < 0 || deduplicatedTests < 0 || scopeFilteredTests < 0
                || selectionFilteredTests < 0 || budgetFilteredTests < 0 || plannedTests < 0
                || estimatedRequests < 0) throw new IllegalArgumentException("negative planning metric");
    }

    public int filteredTests() {
        return scopeFilteredTests + selectionFilteredTests + budgetFilteredTests;
    }
}
