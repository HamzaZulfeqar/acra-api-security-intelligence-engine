package io.acra.core.reporting.s10;

import io.acra.core.security.TokenFingerprint;
import io.acra.core.security.UniversalRedactor;
import io.acra.core.serialization.DomainSerializer;

public final class S10SessionReportExporter {
    private final DomainSerializer serializer = new DomainSerializer();
    private final UniversalRedactor redactor = new UniversalRedactor();

    public S10SessionExportArtifact json(S10SessionReport report) {
        require(report);
        String content = redactor.redactText(serializer.serialize(report));
        return artifact("JSON", "application/json", report.reportId() + ".json", content);
    }

    public S10SessionExportArtifact markdown(S10SessionReport report) {
        require(report);
        StringBuilder text = new StringBuilder("# ACRA Sprint 10 Authentication & Session Report\n\n");
        text.append("Report ID: ").append(s(report.reportId())).append("\n")
                .append("Version: ").append(s(report.reportVersion())).append("\n")
                .append("Status: ").append(report.status()).append("\n")
                .append("Generated: ").append(report.generatedAt()).append("\n");

        S10SessionReportSummary summary = report.summary();
        text.append("\n## Summary\n\n")
                .append("Coverage targets: ").append(summary.coverageTargetCount()).append("\n")
                .append("Baseline targets: ").append(summary.baselineTargetCount()).append("\n")
                .append("Continuity targets: ").append(summary.continuityTargetCount()).append("\n")
                .append("Rotation targets: ").append(summary.rotationTargetCount()).append("\n")
                .append("Observed targets: ").append(summary.observedTargetCount()).append("\n")
                .append("Correlated targets: ").append(summary.correlatedTargetCount()).append("\n")
                .append("Assessed targets: ").append(summary.assessedTargetCount()).append("\n")
                .append("Unobserved targets: ").append(summary.unobservedTargetCount()).append("\n")
                .append("Observed / uncorrelated targets: ").append(summary.observedUncorrelatedTargetCount()).append("\n")
                .append("Correlated / unassessed targets: ").append(summary.correlatedUnassessedTargetCount()).append("\n")
                .append("Session observations: ").append(summary.sessionObservationCount()).append("\n")
                .append("Correlations: ").append(summary.correlationCount()).append("\n")
                .append("Assessments: ").append(summary.assessmentCount()).append("\n")
                .append("Finding candidates: ").append(summary.findingCandidateCount()).append("\n")
                .append("Rejected controls: ").append(summary.rejectedControlCount()).append("\n")
                .append("Inconclusive projections: ").append(summary.inconclusiveProjectionCount()).append("\n")
                .append("Confirmed findings: ").append(summary.confirmedFindingCount()).append("\n");

        text.append("\n## Session Observations - Secret-Minimized\n");
        report.observations().forEach(value -> text.append("- ")
                .append(s(value.observationId()))
                .append(" context=").append(s(value.authContextRef()))
                .append(" principal=").append(s(value.principalId()))
                .append(" role=").append(s(value.roleId()))
                .append(" tenant=").append(s(value.tenantId()))
                .append(" identity=").append(s(value.identityState()))
                .append(" scopes=").append(value.scopes())
                .append("\n"));

        text.append("\n## Correlations\n");
        report.correlations().forEach(value -> text.append("- ")
                .append(s(value.correlationId()))
                .append(" context=").append(s(value.authContextRef()))
                .append(" state=").append(s(value.state()))
                .append(" rotated=").append(value.tokenRotated())
                .append(" drift=").append(value.driftDimensions())
                .append("\n"));

        text.append("\n## Assessments\n");
        report.assessments().forEach(value -> text.append("- ")
                .append(s(value.assessmentId()))
                .append(" context=").append(s(value.authContextRef()))
                .append(" state=").append(s(value.state()))
                .append(" drift=").append(value.driftDimensions())
                .append("\n"));

        text.append("\n## Finding Candidates - Review Only\n");
        report.findingCandidates().forEach(value -> text.append("- ")
                .append(s(value.candidateId()))
                .append(" context=").append(s(value.authContextRef()))
                .append(" state=").append(s(value.state()))
                .append(" rules=").append(value.ruleReferences())
                .append(" dimensions=").append(value.dimensions())
                .append("\n"));

        text.append("\n## Coverage\n");
        report.coverageEntries().forEach(value -> text.append("- ")
                .append(s(value.coverageId()))
                .append(" target=").append(s(value.targetId()))
                .append(" context=").append(s(value.authContextRef()))
                .append(" objective=").append(s(value.objective()))
                .append(" disposition=").append(s(value.disposition()))
                .append("\n"));

        text.append("\n## Limitations\n");
        report.limitations().forEach(value -> text.append("- ").append(s(value)).append("\n"));
        return artifact("MARKDOWN", "text/markdown", report.reportId() + ".md", text.toString());
    }

    private S10SessionExportArtifact artifact(
            String format,
            String mediaType,
            String fileName,
            String content) {
        String safe = redactor.redactText(content);
        return new S10SessionExportArtifact(
                format, mediaType, fileName, TokenFingerprint.sha256(safe), safe);
    }

    private String s(String value) {
        return redactor.redactText(value == null ? "" : value);
    }

    private static void require(S10SessionReport report) {
        if (report == null) throw new IllegalArgumentException("report required");
    }
}
