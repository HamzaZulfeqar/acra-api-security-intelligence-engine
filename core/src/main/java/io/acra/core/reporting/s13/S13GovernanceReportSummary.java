package io.acra.core.reporting.s13;

public record S13GovernanceReportSummary(
        int findingCount,
        long reviewRequiredCount,
        long confirmedCount,
        long remediationCount,
        long retestCount,
        long terminalCount,
        long confirmedHistoryCount,
        int lifecycleEventCount) {

    public S13GovernanceReportSummary {
        if (findingCount < 0 || reviewRequiredCount < 0 || confirmedCount < 0
                || remediationCount < 0 || retestCount < 0 || terminalCount < 0
                || confirmedHistoryCount < 0 || lifecycleEventCount < 0) {
            throw new IllegalArgumentException("negative governance report summary count");
        }
        if (reviewRequiredCount + confirmedCount + remediationCount + retestCount + terminalCount
                != findingCount) {
            throw new IllegalArgumentException("governance queue counts must equal finding count");
        }
        if (confirmedHistoryCount > findingCount) {
            throw new IllegalArgumentException("confirmed history cannot exceed finding count");
        }
    }
}
