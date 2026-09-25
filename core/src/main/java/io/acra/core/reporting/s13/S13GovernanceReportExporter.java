package io.acra.core.reporting.s13;

import io.acra.core.security.TokenFingerprint;
import io.acra.core.security.UniversalRedactor;
import io.acra.core.serialization.DomainSerializer;

public final class S13GovernanceReportExporter {
    private final DomainSerializer serializer = new DomainSerializer();
    private final UniversalRedactor redactor = new UniversalRedactor();

    public S13GovernanceExportArtifact json(S13GovernanceReport report) {
        require(report);
        String content = redactor.redactText(serializer.serialize(report));
        return artifact("JSON", "application/json", report.reportId() + ".json", content);
    }

    public S13GovernanceExportArtifact markdown(S13GovernanceReport report) {
        require(report);
        StringBuilder out = new StringBuilder("# ACRA Sprint 13 Finding Governance Report\n\n");
        out.append("Report ID: ").append(s(report.reportId())).append("\n")
                .append("Version: ").append(s(report.reportVersion())).append("\n")
                .append("Status: ").append(report.status()).append("\n")
                .append("Generated: ").append(report.generatedAt()).append("\n");

        S13GovernanceReportSummary summary = report.summary();
        out.append("\n## Summary\n\n")
                .append("Governed findings: ").append(summary.findingCount()).append("\n")
                .append("Review required: ").append(summary.reviewRequiredCount()).append("\n")
                .append("Confirmed: ").append(summary.confirmedCount()).append("\n")
                .append("Remediation: ").append(summary.remediationCount()).append("\n")
                .append("Retest: ").append(summary.retestCount()).append("\n")
                .append("Terminal: ").append(summary.terminalCount()).append("\n")
                .append("Historically confirmed: ").append(summary.confirmedHistoryCount()).append("\n")
                .append("Lifecycle events: ").append(summary.lifecycleEventCount()).append("\n");

        out.append("\n## Findings\n\n")
                .append("| Finding | Candidate | State | Severity | Confidence | Confirmed history | Evidence | Events |\n")
                .append("|---|---|---|---|---|---:|---:|---:|\n");
        for (S13GovernanceFindingProjection value : report.findings()) {
            out.append("| ").append(s(value.findingId()))
                    .append(" | ").append(s(value.sourceCandidateId()))
                    .append(" | ").append(value.state())
                    .append(" | ").append(value.severity())
                    .append(" | ").append(value.confidence())
                    .append(" | ").append(value.confirmedHistory())
                    .append(" | ").append(value.evidenceIds().size())
                    .append(" | ").append(value.eventCount())
                    .append(" |\n");
        }

        out.append("\n## Lifecycle History\n\n")
                .append("| Finding | Seq | From | To | Action | Reviewer ref | Decision ref | Evidence | Event |\n")
                .append("|---|---:|---|---|---|---|---|---:|---|\n");
        for (S13GovernanceEventProjection value : report.events()) {
            out.append("| ").append(s(value.findingId()))
                    .append(" | ").append(value.sequence())
                    .append(" | ").append(value.fromState())
                    .append(" | ").append(value.toState())
                    .append(" | ").append(value.action())
                    .append(" | ").append(s(value.reviewerReference()))
                    .append(" | ").append(s(value.decisionReference()))
                    .append(" | ").append(value.evidenceIds().size())
                    .append(" | ").append(s(value.eventId()))
                    .append(" |\n");
        }

        out.append("\n## Limitations\n\n");
        report.limitations().forEach(value -> out.append("- ").append(s(value)).append("\n"));
        return artifact("MARKDOWN", "text/markdown", report.reportId() + ".md", out.toString());
    }

    private S13GovernanceExportArtifact artifact(
            String format, String mediaType, String fileName, String content) {
        String safe = redactor.redactText(content);
        return new S13GovernanceExportArtifact(
                format, mediaType, fileName, TokenFingerprint.sha256(safe), safe);
    }

    private String s(String value) {
        return redactor.redactText(value == null ? "" : value);
    }

    private static void require(S13GovernanceReport report) {
        if (report == null) throw new IllegalArgumentException("report required");
    }
}
