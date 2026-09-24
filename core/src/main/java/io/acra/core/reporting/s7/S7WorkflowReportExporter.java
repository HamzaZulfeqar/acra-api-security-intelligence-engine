package io.acra.core.reporting.s7;

import io.acra.core.security.TokenFingerprint;
import io.acra.core.security.UniversalRedactor;
import io.acra.core.serialization.DomainSerializer;
import java.util.Locale;

public final class S7WorkflowReportExporter {
    private final DomainSerializer serializer = new DomainSerializer();
    private final UniversalRedactor redactor = new UniversalRedactor();

    public S7WorkflowExportArtifact json(S7WorkflowReport report) {
        require(report);
        String content = redactor.redactText(serializer.serialize(report));
        return artifact("JSON", "application/json", report.reportId() + ".json", content);
    }

    public S7WorkflowExportArtifact markdown(S7WorkflowReport report) {
        require(report);
        StringBuilder text = new StringBuilder("# ACRA Sprint 7 Workflow Authorization Report\n\n");
        text.append("Report ID: ").append(s(report.reportId())).append("\n")
                .append("Version: ").append(s(report.reportVersion())).append("\n")
                .append("Status: ").append(report.status()).append("\n")
                .append("Generated: ").append(report.generatedAt()).append("\n");
        if (report.policy() == null) {
            text.append("Workflow policy: NOT LOADED\n");
        } else {
            text.append("Workflow policy: ").append(s(report.policy().policyId()))
                    .append(" / ").append(s(report.policy().version())).append("\n")
                    .append("Policy fingerprint: ").append(s(report.policy().fingerprint())).append("\n");
        }

        S7WorkflowReportSummary summary = report.summary();
        text.append("\n## Summary\n\n")
                .append("Resolutions: ").append(summary.resolutionCount()).append("\n")
                .append("Expected ALLOW: ").append(summary.expectedAllowCount()).append("\n")
                .append("Expected DENY: ").append(summary.expectedDenyCount()).append("\n")
                .append("Policy conflicts: ").append(summary.conflictCount()).append("\n")
                .append("Workflow assessment candidates: ").append(summary.assessmentCandidateCount()).append("\n")
                .append("Finding candidates: ").append(summary.findingCandidateCount()).append("\n")
                .append("Risk assessments: ").append(summary.riskAssessmentCount()).append("\n")
                .append("Confirmed findings: ").append(summary.confirmedFindingCount()).append("\n")
                .append("Coverage contexts: ").append(summary.coverageContextCount()).append("\n")
                .append("Resolved coverage: ").append(summary.resolvedCoverageCount()).append("\n")
                .append("Planned coverage: ").append(summary.plannedCoverageCount()).append("\n")
                .append("Attempted coverage: ").append(summary.attemptedCoverageCount()).append("\n")
                .append("Observed coverage: ").append(summary.observedCoverageCount()).append("\n")
                .append("Observation coverage ratio: ")
                .append(String.format(Locale.ROOT, "%.4f", summary.observationCoverageRatio())).append("\n");

        text.append("\n## Workflow Coverage\n");
        report.coverageEntries().forEach(value -> text.append("- ")
                .append(s(value.coverageId())).append(" ")
                .append(s(value.fromState())).append(" --").append(s(value.action())).append("--> ")
                .append(s(value.toState())).append(" expected=").append(value.expectedDecision())
                .append(" resolution=").append(value.resolutionState())
                .append(" lifecycle=").append(value.stage()).append("\n"));

        text.append("\n## Finding Candidates - Review Only\n");
        report.findingCandidates().forEach(value -> text.append("- ")
                .append(s(value.candidateId())).append(" state=").append(value.state())
                .append(" expected=").append(value.expectedDecision())
                .append(" observed=").append(value.observedDecision()).append("\n"));

        text.append("\n## Limitations\n");
        report.limitations().forEach(value -> text.append("- ").append(s(value)).append("\n"));
        return artifact("MARKDOWN", "text/markdown", report.reportId() + ".md", text.toString());
    }

    private S7WorkflowExportArtifact artifact(String format, String mediaType, String fileName, String content) {
        String safe = redactor.redactText(content);
        return new S7WorkflowExportArtifact(format, mediaType, fileName, TokenFingerprint.sha256(safe), safe);
    }

    private String s(String value) {
        return redactor.redactText(value == null ? "" : value);
    }

    private static void require(S7WorkflowReport report) {
        if (report == null) throw new IllegalArgumentException("report required");
    }
}
