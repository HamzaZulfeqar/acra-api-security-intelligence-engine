package io.acra.core.reporting.s10;

public record S10SessionReportSummary(
        int coverageTargetCount,
        int baselineTargetCount,
        int continuityTargetCount,
        int rotationTargetCount,
        int observedTargetCount,
        int correlatedTargetCount,
        int assessedTargetCount,
        int unobservedTargetCount,
        int observedUncorrelatedTargetCount,
        int correlatedUnassessedTargetCount,
        int sessionObservationCount,
        int correlationCount,
        int assessmentCount,
        int findingCandidateCount,
        int rejectedControlCount,
        int inconclusiveProjectionCount,
        int confirmedFindingCount) {

    public S10SessionReportSummary {
        if (coverageTargetCount < 0 || baselineTargetCount < 0 || continuityTargetCount < 0
                || rotationTargetCount < 0 || observedTargetCount < 0 || correlatedTargetCount < 0
                || assessedTargetCount < 0 || unobservedTargetCount < 0
                || observedUncorrelatedTargetCount < 0 || correlatedUnassessedTargetCount < 0
                || sessionObservationCount < 0 || correlationCount < 0 || assessmentCount < 0
                || findingCandidateCount < 0 || rejectedControlCount < 0
                || inconclusiveProjectionCount < 0 || confirmedFindingCount < 0) {
            throw new IllegalArgumentException("negative session report count");
        }
        if (baselineTargetCount + continuityTargetCount + rotationTargetCount != coverageTargetCount) {
            throw new IllegalArgumentException("coverage objective report counts must equal coverage target count");
        }
        if (observedTargetCount + unobservedTargetCount != coverageTargetCount) {
            throw new IllegalArgumentException("observed/unobserved report counts must equal coverage target count");
        }
        if (correlatedTargetCount > observedTargetCount || assessedTargetCount > correlatedTargetCount) {
            throw new IllegalArgumentException("session report coverage lifecycle counts are inconsistent");
        }
        if (confirmedFindingCount != 0) {
            throw new IllegalArgumentException("Sprint 10 report cannot auto-confirm findings");
        }
    }
}
