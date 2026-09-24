package io.acra.core.reporting.s11;

import io.acra.core.active.research.ResearchMetrics;
import io.acra.core.active.research.S11AblationEvaluationReport;
import io.acra.core.active.research.S11AblationProtocol;
import io.acra.core.active.research.S11EvaluationDatasetManifest;
import io.acra.core.security.TokenFingerprint;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;

public final class S11ResearchReportGenerator {
    public static final String REPORT_VERSION = "s11-research-evaluation-report-v1";

    public S11ResearchReport generate(
            S11AblationProtocol protocol,
            S11EvaluationDatasetManifest dataset,
            S11AblationEvaluationReport evaluation) {
        if (protocol == null || dataset == null || evaluation == null) {
            throw new IllegalArgumentException("protocol/dataset/evaluation required");
        }
        if (!dataset.datasetId().equals(evaluation.datasetId())) {
            throw new IllegalArgumentException("evaluation dataset mismatch");
        }

        List<S11ResearchVariantMetric> variants = new ArrayList<>();
        for (var value : evaluation.variants()) {
            ResearchMetrics metrics = value.metrics();
            variants.add(new S11ResearchVariantMetric(
                    value.variantId(),
                    metrics.truePositive(),
                    metrics.trueNegative(),
                    metrics.falsePositive(),
                    metrics.falseNegative(),
                    decimal(metrics.precision()),
                    decimal(metrics.recall()),
                    decimal(metrics.f1()),
                    value.evidenceCompleteness()));
        }

        List<String> limitations = List.of(
                "Measurements apply only to the registered 15-case controlled synthetic localhost dataset.",
                "The report does not establish real-world API scanner accuracy or production vulnerability prevalence.",
                "The report does not establish external-target safety or authorize external scanning.",
                "The report does not prove research novelty.",
                "Real Burp desktop runtime validation remains a separate unverified lane.",
                "Raw HTTP bodies, credentials and synthetic token material are excluded from this report schema.");

        String material = REPORT_VERSION + "|"
                + protocol.protocolId() + "|" + protocol.fingerprint() + "|"
                + dataset.datasetId() + "|" + dataset.fingerprint() + "|"
                + evaluation.predictionExecutionId() + "|"
                + evaluation.evaluationId() + "|" + evaluation.fingerprint() + "|"
                + variants;
        String reportId = "s11-research-report-" + TokenFingerprint.sha256(material).substring(0, 24);

        return new S11ResearchReport(
                reportId,
                REPORT_VERSION,
                protocol.protocolId(),
                protocol.fingerprint(),
                dataset.datasetId(),
                dataset.fingerprint(),
                evaluation.predictionExecutionId(),
                evaluation.evaluationId(),
                evaluation.fingerprint(),
                dataset.cases().size(),
                (int) dataset.positiveCount(),
                (int) dataset.negativeCount(),
                variants,
                limitations);
    }

    private static String decimal(OptionalDouble value) {
        return value.isPresent()
                ? String.format(Locale.ROOT, "%.6f", value.getAsDouble())
                : "N/A";
    }
}
