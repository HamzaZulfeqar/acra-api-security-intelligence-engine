package io.acra.core.reporting.s11;

import io.acra.core.domain.common.Validation;

public record S11ResearchVariantMetric(
        String variantId,
        int truePositive,
        int trueNegative,
        int falsePositive,
        int falseNegative,
        String precision,
        String recall,
        String f1,
        double evidenceCompleteness) {

    public S11ResearchVariantMetric {
        variantId = Validation.requireNonBlank(variantId, "variantId");
        if (truePositive < 0 || trueNegative < 0 || falsePositive < 0 || falseNegative < 0) {
            throw new IllegalArgumentException("metric counts cannot be negative");
        }
        precision = Validation.requireNonBlank(precision, "precision");
        recall = Validation.requireNonBlank(recall, "recall");
        f1 = Validation.requireNonBlank(f1, "f1");
        if (Double.isNaN(evidenceCompleteness)
                || evidenceCompleteness < 0.0 || evidenceCompleteness > 1.0) {
            throw new IllegalArgumentException("evidence completeness must be in [0,1]");
        }
    }
}
