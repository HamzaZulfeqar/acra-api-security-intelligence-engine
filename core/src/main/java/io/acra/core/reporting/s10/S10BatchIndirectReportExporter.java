package io.acra.core.reporting.s10;

import io.acra.core.security.TokenFingerprint;
import io.acra.core.security.UniversalRedactor;
import io.acra.core.serialization.DomainSerializer;

public final class S10BatchIndirectReportExporter {
    private final DomainSerializer serializer = new DomainSerializer();
    private final UniversalRedactor redactor = new UniversalRedactor();

    public S10BatchIndirectExportArtifact json(S10BatchIndirectReport report) {
        require(report);
        String content = redactor.redactText(serializer.serialize(report));
        return artifact("JSON", "application/json", report.reportId() + ".json", content);
    }

    public S10BatchIndirectExportArtifact markdown(S10BatchIndirectReport report) {
        require(report);
        StringBuilder text = new StringBuilder("# ACRA Sprint 10 Batch & Indirect Authorization Report\n\n");
        text.append("Report ID: ").append(s(report.reportId())).append("\n")
                .append("Version: ").append(s(report.reportVersion())).append("\n")
                .append("Status: ").append(report.status()).append("\n")
                .append("Generated: ").append(report.generatedAt()).append("\n");

        S10BatchIndirectReportSummary summary = report.summary();
        text.append("\n## Summary\n\n")
                .append("Policy contexts: ").append(summary.policyContextCount()).append("\n")
                .append("Batch policy contexts: ").append(summary.batchPolicyContextCount()).append("\n")
                .append("Indirect policy contexts: ").append(summary.indirectPolicyContextCount()).append("\n")
                .append("Observed contexts: ").append(summary.observedContextCount()).append("\n")
                .append("Assessed contexts: ").append(summary.assessedContextCount()).append("\n")
                .append("Unobserved contexts: ").append(summary.unobservedContextCount()).append("\n")
                .append("Observed / unassessed contexts: ").append(summary.observedUnassessedContextCount()).append("\n")
                .append("Assessments: ").append(summary.assessmentCount()).append("\n")
                .append("Finding candidates: ").append(summary.findingCandidateCount()).append("\n")
                .append("Rejected controls: ").append(summary.rejectedControlCount()).append("\n")
                .append("Inconclusive projections: ").append(summary.inconclusiveProjectionCount()).append("\n")
                .append("Confirmed findings: ").append(summary.confirmedFindingCount()).append("\n");

        text.append("\n## Policies\n");
        report.policies().forEach(value -> text.append("- ")
                .append(value.family()).append(" ")
                .append(s(value.policyReference()))
                .append(" endpoint=").append(s(value.endpoint()))
                .append(" resource=").append(s(value.resourceId()))
                .append(" action=").append(s(value.action()))
                .append(" expected=").append(value.expectedDecision())
                .append("\n"));

        text.append("\n## Observations\n");
        report.observations().forEach(value -> {
            text.append("- ").append(value.family()).append(" ")
                    .append(s(value.observationId()))
                    .append(" endpoint=").append(s(value.endpoint()))
                    .append(" resource=").append(s(value.resourceId()))
                    .append(" action=").append(s(value.action()))
                    .append(" observed=").append(value.observedDecision());
            if (!value.referenceFingerprint().isBlank()) {
                text.append(" referenceFingerprint=").append(s(value.referenceFingerprint()));
            }
            text.append("\n");
        });

        text.append("\n## Assessments\n");
        report.assessments().forEach(value -> text.append("- ")
                .append(value.family()).append(" ")
                .append(s(value.assessmentId()))
                .append(" policy=").append(s(value.policyReference()))
                .append(" expected=").append(value.expectedDecision())
                .append(" observed=").append(value.observedDecision())
                .append(" state=").append(value.state())
                .append("\n"));

        text.append("\n## Finding Candidates - Review Only\n");
        report.findingCandidates().forEach(value -> text.append("- ")
                .append(s(value.candidateId()))
                .append(" state=").append(value.state())
                .append(" expected=").append(value.expectedDecision())
                .append(" observed=").append(value.observedDecision())
                .append(" dimensions=").append(value.dimensions())
                .append("\n"));

        text.append("\n## Coverage\n");
        report.coverageEntries().forEach(value -> text.append("- ")
                .append(value.family()).append(" ")
                .append(s(value.coverageId()))
                .append(" policy=").append(s(value.policyReference()))
                .append(" resource=").append(s(value.resourceId()))
                .append(" action=").append(s(value.action()))
                .append(" disposition=").append(value.disposition())
                .append("\n"));

        text.append("\n## Limitations\n");
        report.limitations().forEach(value -> text.append("- ").append(s(value)).append("\n"));
        return artifact("MARKDOWN", "text/markdown", report.reportId() + ".md", text.toString());
    }

    private S10BatchIndirectExportArtifact artifact(
            String format,
            String mediaType,
            String fileName,
            String content) {
        String safe = redactor.redactText(content);
        return new S10BatchIndirectExportArtifact(
                format, mediaType, fileName, TokenFingerprint.sha256(safe), safe);
    }

    private String s(String value) {
        return redactor.redactText(value == null ? "" : value);
    }

    private static void require(S10BatchIndirectReport report) {
        if (report == null) throw new IllegalArgumentException("report required");
    }
}
