package io.acra.core.reporting.s9;

import io.acra.core.security.TokenFingerprint;
import io.acra.core.security.UniversalRedactor;
import io.acra.core.serialization.DomainSerializer;

public final class S9PropertyReportExporter {
    private final DomainSerializer serializer = new DomainSerializer();
    private final UniversalRedactor redactor = new UniversalRedactor();

    public S9PropertyExportArtifact json(S9PropertyReport report) {
        require(report);
        String content = redactor.redactText(serializer.serialize(report));
        return artifact("JSON", "application/json", report.reportId() + ".json", content);
    }

    public S9PropertyExportArtifact markdown(S9PropertyReport report) {
        require(report);
        StringBuilder text = new StringBuilder("# ACRA Sprint 9 Property Authorization Report\n\n");
        text.append("Report ID: ").append(s(report.reportId())).append("\n")
                .append("Version: ").append(s(report.reportVersion())).append("\n")
                .append("Status: ").append(report.status()).append("\n")
                .append("Generated: ").append(report.generatedAt()).append("\n");

        S9PropertyReportSummary summary = report.summary();
        text.append("\n## Summary\n\n")
                .append("Policy contexts: ").append(summary.policyContextCount()).append("\n")
                .append("READ policy contexts: ").append(summary.readPolicyContextCount()).append("\n")
                .append("UPDATE policy contexts: ").append(summary.updatePolicyContextCount()).append("\n")
                .append("Observed contexts: ").append(summary.observedContextCount()).append("\n")
                .append("Assessed contexts: ").append(summary.assessedContextCount()).append("\n")
                .append("Unobserved contexts: ").append(summary.unobservedContextCount()).append("\n")
                .append("Observed / unassessed contexts: ").append(summary.observedUnassessedContextCount()).append("\n")
                .append("Assessments: ").append(summary.assessmentCount()).append("\n")
                .append("Finding candidates: ").append(summary.findingCandidateCount()).append("\n")
                .append("Rejected controls: ").append(summary.rejectedControlCount()).append("\n")
                .append("Inconclusive projections: ").append(summary.inconclusiveProjectionCount()).append("\n")
                .append("Confirmed findings: ").append(summary.confirmedFindingCount()).append("\n");

        text.append("\n## Property Policies\n");
        report.policies().forEach(value -> text.append("- ")
                .append(s(value.policyReference()))
                .append(" endpoint=").append(s(value.endpoint()))
                .append(" property=").append(s(value.property()))
                .append(" operation=").append(value.operation())
                .append(" expected=").append(value.expectedDecision())
                .append("\n"));

        text.append("\n## Property Observations\n");
        report.observations().forEach(value -> text.append("- ")
                .append(s(value.observationId()))
                .append(" endpoint=").append(s(value.endpoint()))
                .append(" property=").append(s(value.property()))
                .append(" operation=").append(value.operation())
                .append(" observed=").append(value.observedDecision())
                .append("\n"));

        text.append("\n## Finding Candidates - Review Only\n");
        report.findingCandidates().forEach(value -> text.append("- ")
                .append(s(value.candidateId()))
                .append(" state=").append(value.state())
                .append(" expected=").append(value.expectedDecision())
                .append(" observed=").append(value.observedDecision())
                .append("\n"));

        text.append("\n## Coverage\n");
        report.coverageEntries().forEach(value -> text.append("- ")
                .append(s(value.coverageId()))
                .append(" property=").append(s(value.property()))
                .append(" operation=").append(value.operation())
                .append(" disposition=").append(value.disposition())
                .append("\n"));

        text.append("\n## Limitations\n");
        report.limitations().forEach(value -> text.append("- ").append(s(value)).append("\n"));
        return artifact("MARKDOWN", "text/markdown", report.reportId() + ".md", text.toString());
    }

    private S9PropertyExportArtifact artifact(String format, String mediaType, String fileName, String content) {
        String safe = redactor.redactText(content);
        return new S9PropertyExportArtifact(
                format, mediaType, fileName, TokenFingerprint.sha256(safe), safe);
    }

    private String s(String value) {
        return redactor.redactText(value == null ? "" : value);
    }

    private static void require(S9PropertyReport report) {
        if (report == null) throw new IllegalArgumentException("report required");
    }
}
