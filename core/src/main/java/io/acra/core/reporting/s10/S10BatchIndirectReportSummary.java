package io.acra.core.reporting.s10;

public record S10BatchIndirectReportSummary(
        int policyContextCount,
        int batchPolicyContextCount,
        int indirectPolicyContextCount,
        int observedContextCount,
        int assessedContextCount,
        int unobservedContextCount,
        int observedUnassessedContextCount,
        int assessmentCount,
        int findingCandidateCount,
        int rejectedControlCount,
        int inconclusiveProjectionCount,
        int confirmedFindingCount) {

    public S10BatchIndirectReportSummary {
        if (policyContextCount < 0 || batchPolicyContextCount < 0 || indirectPolicyContextCount < 0
                || observedContextCount < 0 || assessedContextCount < 0 || unobservedContextCount < 0
                || observedUnassessedContextCount < 0 || assessmentCount < 0 || findingCandidateCount < 0
                || rejectedControlCount < 0 || inconclusiveProjectionCount < 0 || confirmedFindingCount < 0) {
            throw new IllegalArgumentException("negative Sprint 10 report count");
        }
        if (batchPolicyContextCount + indirectPolicyContextCount != policyContextCount) {
            throw new IllegalArgumentException("batch/indirect report counts must equal policy contexts");
        }
        if (observedContextCount + unobservedContextCount != policyContextCount) {
            throw new IllegalArgumentException("observed/unobserved report counts must equal policy contexts");
        }
        if (assessedContextCount > observedContextCount) {
            throw new IllegalArgumentException("assessed contexts cannot exceed observed contexts");
        }
    }
}
