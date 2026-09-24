package io.acra.core.reporting.s8;

public record S8RoutingReportSummary(
        int normalizationTraceCount,
        int completeTraceCount,
        int boundaryTraceCount,
        int routingDivergenceCount,
        int authorizationBoundaryChangeCount,
        int combinedDivergenceCount,
        int inconclusiveTransitionCount,
        int assessmentCount,
        int assessmentCandidateCount,
        int findingCandidateCount,
        int confirmedFindingCount) {

    public S8RoutingReportSummary {
        if (normalizationTraceCount < 0 || completeTraceCount < 0 || boundaryTraceCount < 0
                || routingDivergenceCount < 0 || authorizationBoundaryChangeCount < 0
                || combinedDivergenceCount < 0 || inconclusiveTransitionCount < 0
                || assessmentCount < 0 || assessmentCandidateCount < 0
                || findingCandidateCount < 0 || confirmedFindingCount < 0) {
            throw new IllegalArgumentException("negative routing report count");
        }
    }
}
