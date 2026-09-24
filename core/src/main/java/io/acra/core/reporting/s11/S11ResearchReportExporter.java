package io.acra.core.reporting.s11;

import io.acra.core.security.TokenFingerprint;
import io.acra.core.security.UniversalRedactor;
import io.acra.core.serialization.DomainSerializer;

public final class S11ResearchReportExporter {
    private final DomainSerializer serializer = new DomainSerializer();
    private final UniversalRedactor redactor = new UniversalRedactor();

    public S11ResearchExportArtifact json(S11ResearchReport report) {
        require(report);
        String content = redactor.redactText(serializer.serialize(report));
        return artifact("JSON", "application/json", report.reportId() + ".json", content);
    }

    public S11ResearchExportArtifact markdown(S11ResearchReport report) {
        require(report);
        StringBuilder out = new StringBuilder("# ACRA Sprint 11 Research Evaluation Report\n\n");
        out.append("Report ID: ").append(s(report.reportId())).append("\n")
                .append("Version: ").append(s(report.reportVersion())).append("\n")
                .append("Protocol: ").append(s(report.protocolId())).append("\n")
                .append("Protocol fingerprint: ").append(s(report.protocolFingerprint())).append("\n")
                .append("Dataset: ").append(s(report.datasetId())).append("\n")
                .append("Dataset fingerprint: ").append(s(report.datasetFingerprint())).append("\n")
                .append("Prediction execution: ").append(s(report.predictionExecutionId())).append("\n")
                .append("Evaluation: ").append(s(report.evaluationId())).append("\n")
                .append("Evaluation fingerprint: ").append(s(report.evaluationFingerprint())).append("\n");

        out.append("\n## Dataset\n\n")
                .append("Cases: ").append(report.caseCount()).append("\n")
                .append("Positive: ").append(report.positiveCaseCount()).append("\n")
                .append("Negative: ").append(report.negativeCaseCount()).append("\n");

        out.append("\n## Ablation metrics\n\n")
                .append("| Variant | TP | TN | FP | FN | Precision | Recall | F1 | Evidence completeness |\n")
                .append("|---|---:|---:|---:|---:|---:|---:|---:|---:|\n");
        for (S11ResearchVariantMetric value : report.variants()) {
            out.append("| ").append(value.variantId())
                    .append(" | ").append(value.truePositive())
                    .append(" | ").append(value.trueNegative())
                    .append(" | ").append(value.falsePositive())
                    .append(" | ").append(value.falseNegative())
                    .append(" | ").append(value.precision())
                    .append(" | ").append(value.recall())
                    .append(" | ").append(value.f1())
                    .append(" | ").append(String.format(java.util.Locale.ROOT, "%.6f", value.evidenceCompleteness()))
                    .append(" |\n");
        }

        out.append("\n## Limitations\n\n");
        report.limitations().forEach(value -> out.append("- ").append(s(value)).append("\n"));

        return artifact("MARKDOWN", "text/markdown", report.reportId() + ".md", out.toString());
    }

    private S11ResearchExportArtifact artifact(
            String format,
            String mediaType,
            String fileName,
            String content) {
        String safe = redactor.redactText(content);
        return new S11ResearchExportArtifact(
                format, mediaType, fileName, TokenFingerprint.sha256(safe), safe);
    }

    private String s(String value) {
        return redactor.redactText(value == null ? "" : value);
    }

    private static void require(S11ResearchReport report) {
        if (report == null) throw new IllegalArgumentException("report required");
    }
}
