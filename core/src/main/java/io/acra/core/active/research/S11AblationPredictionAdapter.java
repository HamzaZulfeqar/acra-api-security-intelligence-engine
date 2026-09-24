package io.acra.core.active.research;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public final class S11AblationPredictionAdapter {

    public AblationPredictionResult predict(
            AblationVariant variant,
            AblationCaseEvidence evidence) {
        if (variant == null || evidence == null) {
            throw new IllegalArgumentException("variant/evidence required");
        }

        ResearchPrediction prediction = evidence.baselinePrediction();
        List<AblationDimension> used = new ArrayList<>();
        TreeSet<String> evidenceIds = new TreeSet<>(evidence.baselineEvidenceIds());
        List<String> reasons = new ArrayList<>();

        for (AblationDimension dimension : variant.enabledDimensions()) {
            AblationDimensionEvidence value = evidence.evidenceFor(dimension);
            if (value == null) {
                reasons.add("MISSING_DIMENSION_EVIDENCE:" + dimension.name());
                return result(
                        evidence.caseId(),
                        variant.variantId(),
                        AblationPredictionState.INCONCLUSIVE,
                        null,
                        used,
                        List.copyOf(evidenceIds),
                        reasons);
            }
            used.add(dimension);
            evidenceIds.addAll(value.evidenceIds());
            prediction = value.prediction();
        }

        return result(
                evidence.caseId(),
                variant.variantId(),
                AblationPredictionState.RESOLVED,
                prediction,
                used,
                List.copyOf(evidenceIds),
                List.of());
    }

    private static AblationPredictionResult result(
            String caseId,
            String variantId,
            AblationPredictionState state,
            ResearchPrediction prediction,
            List<AblationDimension> usedDimensions,
            List<String> evidenceIds,
            List<String> reasons) {
        String id = AblationPredictionResult.deterministicId(
                caseId, variantId, state, prediction, usedDimensions, evidenceIds, reasons);
        return new AblationPredictionResult(
                id, caseId, variantId, state, prediction,
                usedDimensions, evidenceIds, reasons, "");
    }
}
