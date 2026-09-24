package io.acra.core.active.research;

import io.acra.core.domain.common.Validation;

public record S11VariantEvaluation(
        String variantId,
        int evaluatedCases,
        int inconclusiveCases,
        ResearchMetrics metrics,
        double evidenceCompleteness) {

    public S11VariantEvaluation {
        variantId = Validation.requireNonBlank(variantId, "variantId");
        if (evaluatedCases < 0 || inconclusiveCases < 0 || metrics == null) {
            throw new IllegalArgumentException("valid variant evaluation required");
        }
        if (evidenceCompleteness < 0.0 || evidenceCompleteness > 1.0
                || Double.isNaN(evidenceCompleteness)) {
            throw new IllegalArgumentException("evidence completeness must be in [0,1]");
        }
    }
}
