package io.acra.core.reporting.s7;

public record S7WorkflowReportSummary(
        int resolutionCount,
        int expectedAllowCount,
        int expectedDenyCount,
        int conflictCount,
        int assessmentCandidateCount,
        int findingCandidateCount,
        int riskAssessmentCount,
        int confirmedFindingCount,
        int coverageContextCount,
        int resolvedCoverageCount,
        int plannedCoverageCount,
        int attemptedCoverageCount,
        int observedCoverageCount,
        double observationCoverageRatio) {

    public S7WorkflowReportSummary {
        if (resolutionCount < 0 || expectedAllowCount < 0 || expectedDenyCount < 0 || conflictCount < 0
                || assessmentCandidateCount < 0 || findingCandidateCount < 0 || riskAssessmentCount < 0
                || confirmedFindingCount < 0 || coverageContextCount < 0 || resolvedCoverageCount < 0
                || plannedCoverageCount < 0 || attemptedCoverageCount < 0 || observedCoverageCount < 0) {
            throw new IllegalArgumentException("negative report count");
        }
        if (!Double.isFinite(observationCoverageRatio)
                || observationCoverageRatio < 0.0 || observationCoverageRatio > 1.0) {
            throw new IllegalArgumentException("observationCoverageRatio");
        }
    }
}
