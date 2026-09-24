package io.acra.core.active.research;

import io.acra.core.domain.common.Validation;
import io.acra.core.security.TokenFingerprint;
import java.util.List;

public record AblationPredictionResult(
        String resultId,
        String caseId,
        String variantId,
        AblationPredictionState state,
        ResearchPrediction prediction,
        List<AblationDimension> usedDimensions,
        List<String> evidenceIds,
        List<String> reasons,
        String fingerprint) {

    public AblationPredictionResult {
        resultId = Validation.requireNonBlank(resultId, "resultId");
        caseId = Validation.requireNonBlank(caseId, "caseId");
        variantId = Validation.requireNonBlank(variantId, "variantId");
        if (state == null) throw new IllegalArgumentException("state required");
        if (state == AblationPredictionState.RESOLVED && prediction == null) {
            throw new IllegalArgumentException("resolved prediction required");
        }
        if (state == AblationPredictionState.INCONCLUSIVE && prediction != null) {
            throw new IllegalArgumentException("inconclusive result cannot carry binary prediction");
        }
        usedDimensions = List.copyOf(usedDimensions == null ? List.of() : usedDimensions);
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds).stream()
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .sorted()
                .toList();
        reasons = List.copyOf(reasons == null ? List.of() : reasons).stream()
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .sorted()
                .toList();
        if (evidenceIds.isEmpty()) throw new IllegalArgumentException("prediction evidenceIds required");
        String calculated = TokenFingerprint.sha256(canonical(
                caseId, variantId, state, prediction, usedDimensions, evidenceIds, reasons));
        fingerprint = fingerprint == null || fingerprint.isBlank() ? calculated : fingerprint;
        if (!fingerprint.equals(calculated)) {
            throw new IllegalArgumentException("prediction fingerprint mismatch");
        }
    }

    public static String deterministicId(
            String caseId,
            String variantId,
            AblationPredictionState state,
            ResearchPrediction prediction,
            List<AblationDimension> usedDimensions,
            List<String> evidenceIds,
            List<String> reasons) {
        return "s11-prediction-" + TokenFingerprint.sha256(canonical(
                caseId, variantId, state, prediction, usedDimensions, evidenceIds, reasons)).substring(0, 24);
    }

    private static String canonical(
            String caseId,
            String variantId,
            AblationPredictionState state,
            ResearchPrediction prediction,
            List<AblationDimension> dimensions,
            List<String> evidence,
            List<String> reasons) {
        return caseId + "|" + variantId + "|" + state + "|"
                + (prediction == null ? "NONE" : prediction.name()) + "|"
                + dimensions + "|" + evidence + "|" + reasons;
    }
}
