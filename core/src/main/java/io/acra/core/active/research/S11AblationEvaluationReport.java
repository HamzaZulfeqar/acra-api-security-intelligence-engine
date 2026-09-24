package io.acra.core.active.research;

import io.acra.core.domain.common.Validation;
import io.acra.core.security.TokenFingerprint;
import java.util.List;

public record S11AblationEvaluationReport(
        String evaluationId,
        String datasetId,
        String predictionExecutionId,
        List<ResearchExecutionRecord> records,
        List<S11VariantEvaluation> variants,
        String fingerprint) {

    public S11AblationEvaluationReport {
        datasetId = Validation.requireNonBlank(datasetId, "datasetId");
        predictionExecutionId = Validation.requireNonBlank(predictionExecutionId, "predictionExecutionId");
        records = List.copyOf(records == null ? List.of() : records);
        variants = List.copyOf(variants == null ? List.of() : variants);
        if (records.isEmpty() || variants.size() != 8) {
            throw new IllegalArgumentException("complete A0-A7 evaluation required");
        }

        String material = canonical(datasetId, predictionExecutionId, records, variants);
        String expectedId = "s11-evaluation-" + TokenFingerprint.sha256(material).substring(0, 24);
        evaluationId = evaluationId == null || evaluationId.isBlank()
                ? expectedId : Validation.requireNonBlank(evaluationId, "evaluationId");
        if (!evaluationId.equals(expectedId)) {
            throw new IllegalArgumentException("evaluation identity mismatch");
        }

        String calculated = TokenFingerprint.sha256(evaluationId + "|" + material);
        fingerprint = fingerprint == null || fingerprint.isBlank() ? calculated : fingerprint;
        if (!fingerprint.equals(calculated)) {
            throw new IllegalArgumentException("evaluation fingerprint mismatch");
        }
    }

    private static String canonical(
            String datasetId,
            String predictionExecutionId,
            List<ResearchExecutionRecord> records,
            List<S11VariantEvaluation> variants) {
        StringBuilder out = new StringBuilder(datasetId)
                .append('|').append(predictionExecutionId).append('\n');
        for (ResearchExecutionRecord record : records) {
            out.append(record.caseId()).append('|')
                    .append(record.testId()).append('|')
                    .append(record.observationId()).append('|')
                    .append(record.groundTruth()).append('|')
                    .append(record.prediction()).append('|')
                    .append(record.evidenceIds()).append('\n');
        }
        for (S11VariantEvaluation variant : variants) {
            out.append(variant.variantId()).append('|')
                    .append(variant.evaluatedCases()).append('|')
                    .append(variant.inconclusiveCases()).append('|')
                    .append(variant.metrics()).append('|')
                    .append(variant.evidenceCompleteness()).append('\n');
        }
        return out.toString();
    }
}
