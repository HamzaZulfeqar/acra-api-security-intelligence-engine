package io.acra.core.active.research;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class S11AblationEvaluator {

    public S11AblationEvaluationReport evaluate(
            S11PredictionExecutionSnapshot predictions,
            S11EvaluationDatasetManifest dataset,
            List<AblationEvidenceBundle> evidenceBundles) {
        if (predictions == null || dataset == null) {
            throw new IllegalArgumentException("predictions/dataset required");
        }
        if (predictions.campaignPlan().coverage().executedCells()
                != predictions.campaignPlan().cells().size()) {
            throw new IllegalArgumentException("prediction campaign must be fully executed");
        }

        Map<String, ResearchGroundTruth> labels = new HashMap<>();
        for (ResearchDatasetCase item : dataset.cases()) {
            if (labels.put(item.caseId(), item.groundTruth()) != null) {
                throw new IllegalArgumentException("duplicate dataset case");
            }
        }

        Map<String, AblationEvidenceBundle> bundles = new HashMap<>();
        for (AblationEvidenceBundle bundle :
                evidenceBundles == null ? List.<AblationEvidenceBundle>of() : evidenceBundles) {
            if (bundle == null) throw new IllegalArgumentException("evidence bundle required");
            if (bundles.put(bundle.caseId(), bundle) != null) {
                throw new IllegalArgumentException("duplicate evidence bundle");
            }
        }

        S11AblationProtocol protocol = S11AblationProtocol.canonical();
        List<ResearchExecutionRecord> records = new ArrayList<>();
        List<S11VariantEvaluation> variantEvaluations = new ArrayList<>();

        for (AblationVariant variant : protocol.variants()) {
            List<ResearchExecutionRecord> variantRecords = new ArrayList<>();
            int inconclusive = 0;
            int completeEvidence = 0;

            for (ResearchDatasetCase item : dataset.cases()) {
                AblationPredictionResult result = find(
                        predictions.results(), item.caseId(), variant.variantId());
                if (result.state() != AblationPredictionState.RESOLVED || result.prediction() == null) {
                    inconclusive++;
                    continue;
                }

                AblationEvidenceBundle bundle = bundles.get(item.caseId());
                if (bundle != null && bundle.completeFor(variant.enabledDimensions())) {
                    completeEvidence++;
                }

                ResearchExecutionRecord record = new ResearchExecutionRecord(
                        item.caseId(),
                        predictions.executionId(),
                        "S11-" + variant.variantId() + "-" + item.caseId(),
                        result.resultId(),
                        result.evidenceIds(),
                        labels.get(item.caseId()),
                        result.prediction());
                variantRecords.add(record);
                records.add(record);
            }

            double evidenceCompleteness = dataset.cases().isEmpty()
                    ? 0.0 : completeEvidence / (double) dataset.cases().size();
            variantEvaluations.add(new S11VariantEvaluation(
                    variant.variantId(),
                    variantRecords.size(),
                    inconclusive,
                    ResearchMetrics.from(variantRecords),
                    evidenceCompleteness));
        }

        if (records.size() + variantEvaluations.stream()
                .mapToInt(S11VariantEvaluation::inconclusiveCases).sum()
                != dataset.cases().size() * protocol.variants().size()) {
            throw new IllegalArgumentException("evaluation denominator mismatch");
        }

        return new S11AblationEvaluationReport(
                "",
                dataset.datasetId(),
                predictions.executionId(),
                records,
                variantEvaluations,
                "");
    }

    private static AblationPredictionResult find(
            List<AblationPredictionResult> results,
            String caseId,
            String variantId) {
        List<AblationPredictionResult> matches = results.stream()
                .filter(value -> value.caseId().equals(caseId)
                        && value.variantId().equals(variantId))
                .toList();
        if (matches.size() != 1) {
            throw new IllegalArgumentException(
                    "expected exactly one prediction for " + caseId + "/" + variantId);
        }
        return matches.getFirst();
    }
}
