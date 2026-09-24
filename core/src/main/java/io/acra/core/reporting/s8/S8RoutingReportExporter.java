package io.acra.core.reporting.s8;

import io.acra.core.security.TokenFingerprint;
import io.acra.core.security.UniversalRedactor;
import io.acra.core.serialization.DomainSerializer;

public final class S8RoutingReportExporter {
    private final DomainSerializer serializer = new DomainSerializer();
    private final UniversalRedactor redactor = new UniversalRedactor();

    public S8RoutingExportArtifact json(S8RoutingReport report) {
        require(report);
        String content = redactor.redactText(serializer.serialize(report));
        return artifact("JSON", "application/json", report.reportId() + ".json", content);
    }

    public S8RoutingExportArtifact markdown(S8RoutingReport report) {
        require(report);
        StringBuilder text = new StringBuilder("# ACRA Sprint 8 Routing Normalization Report\n\n");
        text.append("Report ID: ").append(s(report.reportId())).append("\n")
                .append("Version: ").append(s(report.reportVersion())).append("\n")
                .append("Status: ").append(report.status()).append("\n")
                .append("Generated: ").append(report.generatedAt()).append("\n");

        S8RoutingReportSummary summary = report.summary();
        text.append("\n## Summary\n\n")
                .append("Normalization traces: ").append(summary.normalizationTraceCount()).append("\n")
                .append("Complete traces: ").append(summary.completeTraceCount()).append("\n")
                .append("Boundary traces: ").append(summary.boundaryTraceCount()).append("\n")
                .append("Routing divergences: ").append(summary.routingDivergenceCount()).append("\n")
                .append("Authorization boundary changes: ").append(summary.authorizationBoundaryChangeCount()).append("\n")
                .append("Combined divergences: ").append(summary.combinedDivergenceCount()).append("\n")
                .append("Inconclusive transitions: ").append(summary.inconclusiveTransitionCount()).append("\n")
                .append("Assessments: ").append(summary.assessmentCount()).append("\n")
                .append("Assessment candidates: ").append(summary.assessmentCandidateCount()).append("\n")
                .append("Finding candidates: ").append(summary.findingCandidateCount()).append("\n")
                .append("Confirmed findings: ").append(summary.confirmedFindingCount()).append("\n");

        text.append("\n## Boundary Differentials\n");
        report.boundaryTraces().forEach(trace -> trace.transitions().forEach(value -> text.append("- ")
                .append(s(trace.traceId())).append(" ")
                .append(value.fromStage()).append(" → ").append(value.toStage())
                .append(" path=").append(value.pathDivergence())
                .append(" authorizationChanged=").append(value.authorizationChanged())
                .append(" state=").append(value.state()).append("\n")));

        text.append("\n## Finding Candidates - Review Only\n");
        report.findingCandidates().forEach(value -> text.append("- ")
                .append(s(value.candidateId())).append(" state=").append(value.state())
                .append(" expected=").append(value.expectedDecision())
                .append(" observed=").append(value.observedDecision()).append("\n"));

        text.append("\n## Limitations\n");
        report.limitations().forEach(value -> text.append("- ").append(s(value)).append("\n"));
        return artifact("MARKDOWN", "text/markdown", report.reportId() + ".md", text.toString());
    }

    private S8RoutingExportArtifact artifact(String format, String mediaType, String fileName, String content) {
        String safe = redactor.redactText(content);
        return new S8RoutingExportArtifact(format, mediaType, fileName, TokenFingerprint.sha256(safe), safe);
    }

    private String s(String value) {
        return redactor.redactText(value == null ? "" : value);
    }

    private static void require(S8RoutingReport report) {
        if (report == null) throw new IllegalArgumentException("report required");
    }
}
