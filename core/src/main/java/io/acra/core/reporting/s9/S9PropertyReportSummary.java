package io.acra.core.reporting.s9;

public record S9PropertyReportSummary(
        int policyContextCount,
        int readPolicyContextCount,
        int updatePolicyContextCount,
        int observedContextCount,
        int assessedContextCount,
        int unobservedContextCount,
        int observedUnassessedContextCount,
        int assessmentCount,
        int findingCandidateCount,
        int rejectedControlCount,
        int inconclusiveProjectionCount,
        int confirmedFindingCount) {

    public S9PropertyReportSummary {
        if (policyContextCount < 0 || readPolicyContextCount < 0 || updatePolicyContextCount < 0
                || observedContextCount < 0 || assessedContextCount < 0 || unobservedContextCount < 0
                || observedUnassessedContextCount < 0 || assessmentCount < 0 || findingCandidateCount < 0
                || rejectedControlCount < 0 || inconclusiveProjectionCount < 0 || confirmedFindingCount < 0) {
            throw new IllegalArgumentException("negative property report count");
        }
        if (readPolicyContextCount + updatePolicyContextCount != policyContextCount) {
            throw new IllegalArgumentException("READ/UPDATE report counts must equal policy contexts");
        }
        if (observedContextCount + unobservedContextCount != policyContextCount) {
            throw new IllegalArgumentException("observed/unobserved report counts must equal policy contexts");
        }
        if (assessedContextCount > observedContextCount) {
            throw new IllegalArgumentException("assessed contexts cannot exceed observed contexts");
        }
    }
}
