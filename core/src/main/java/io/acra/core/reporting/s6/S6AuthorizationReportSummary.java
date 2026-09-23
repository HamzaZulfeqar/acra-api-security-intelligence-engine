package io.acra.core.reporting.s6;

public record S6AuthorizationReportSummary(
        int analysisCount,
        int expectedAllowCount,
        int expectedDenyCount,
        int conflictCount,
        int findingCandidateCount,
        int riskAssessmentCount,
        int confirmedFindingCount,
        int completeCoverageCount,
        double averageCoverage) {

    public S6AuthorizationReportSummary {
        if (analysisCount < 0 || expectedAllowCount < 0 || expectedDenyCount < 0 || conflictCount < 0
                || findingCandidateCount < 0 || riskAssessmentCount < 0 || confirmedFindingCount < 0
                || completeCoverageCount < 0) {
            throw new IllegalArgumentException("negative report count");
        }
        if (!Double.isFinite(averageCoverage) || averageCoverage < 0.0 || averageCoverage > 1.0) {
            throw new IllegalArgumentException("averageCoverage");
        }
    }
}
