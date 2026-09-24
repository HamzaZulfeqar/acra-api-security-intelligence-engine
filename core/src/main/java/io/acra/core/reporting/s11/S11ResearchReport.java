package io.acra.core.reporting.s11;

import java.util.List;

public record S11ResearchReport(
        String reportId,
        String reportVersion,
        String protocolId,
        String protocolFingerprint,
        String datasetId,
        String datasetFingerprint,
        String predictionExecutionId,
        String evaluationId,
        String evaluationFingerprint,
        int caseCount,
        int positiveCaseCount,
        int negativeCaseCount,
        List<S11ResearchVariantMetric> variants,
        List<String> limitations) {

    public S11ResearchReport {
        if (reportId == null || reportId.isBlank()) throw new IllegalArgumentException("reportId required");
        if (reportVersion == null || reportVersion.isBlank()) throw new IllegalArgumentException("reportVersion required");
        if (protocolId == null || protocolId.isBlank() || protocolFingerprint == null || protocolFingerprint.isBlank()) {
            throw new IllegalArgumentException("protocol provenance required");
        }
        if (datasetId == null || datasetId.isBlank() || datasetFingerprint == null || datasetFingerprint.isBlank()) {
            throw new IllegalArgumentException("dataset provenance required");
        }
        if (predictionExecutionId == null || predictionExecutionId.isBlank()
                || evaluationId == null || evaluationId.isBlank()
                || evaluationFingerprint == null || evaluationFingerprint.isBlank()) {
            throw new IllegalArgumentException("evaluation provenance required");
        }
        if (caseCount <= 0 || positiveCaseCount < 0 || negativeCaseCount < 0
                || positiveCaseCount + negativeCaseCount != caseCount) {
            throw new IllegalArgumentException("valid case counts required");
        }
        variants = List.copyOf(variants == null ? List.of() : variants);
        if (variants.size() != 8) throw new IllegalArgumentException("A0-A7 metrics required");
        limitations = List.copyOf(limitations == null ? List.of() : limitations);
        if (limitations.isEmpty()) throw new IllegalArgumentException("report limitations required");
    }
}
