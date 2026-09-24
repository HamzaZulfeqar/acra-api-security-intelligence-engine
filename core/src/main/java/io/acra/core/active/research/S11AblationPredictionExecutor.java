package io.acra.core.active.research;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class S11AblationPredictionExecutor {
    private final S11AblationPredictionAdapter adapter = new S11AblationPredictionAdapter();

    public S11PredictionExecutionSnapshot execute(
            S11AblationCampaignPlan evidenceReadyPlan,
            List<AblationCaseEvidence> caseEvidence) {
        if (evidenceReadyPlan == null) throw new IllegalArgumentException("evidenceReadyPlan required");
        if (evidenceReadyPlan.coverage().evidenceReadyCells() != evidenceReadyPlan.cells().size()) {
            throw new IllegalArgumentException("all campaign cells must be EVIDENCE_READY before prediction execution");
        }

        Map<String, AblationCaseEvidence> evidenceByCase = new HashMap<>();
        for (AblationCaseEvidence evidence :
                caseEvidence == null ? List.<AblationCaseEvidence>of() : caseEvidence) {
            if (evidence == null) throw new IllegalArgumentException("case evidence required");
            if (evidenceByCase.put(evidence.caseId(), evidence) != null) {
                throw new IllegalArgumentException("duplicate case prediction evidence");
            }
        }

        S11AblationProtocol protocol = S11AblationProtocol.canonical();
        Map<String, AblationVariant> variants = new HashMap<>();
        for (AblationVariant variant : protocol.variants()) {
            variants.put(variant.variantId(), variant);
        }

        List<AblationPredictionResult> results = new ArrayList<>();
        List<AblationCampaignCell> executedCells = new ArrayList<>();

        for (AblationCampaignCell cell : evidenceReadyPlan.cells()) {
            AblationCaseEvidence evidence = evidenceByCase.get(cell.caseId());
            if (evidence == null) throw new IllegalArgumentException("missing case prediction evidence: " + cell.caseId());
            AblationVariant variant = variants.get(cell.variantId());
            if (variant == null) throw new IllegalArgumentException("unknown variant: " + cell.variantId());

            AblationPredictionResult result = adapter.predict(variant, evidence);
            results.add(result);
            executedCells.add(new AblationCampaignCell(
                    cell.cellId(),
                    cell.caseId(),
                    cell.variantId(),
                    cell.requiredDimensions(),
                    AblationCampaignCellState.EXECUTED));
        }

        S11AblationCampaignPlan executedPlan = new S11AblationCampaignPlan(
                evidenceReadyPlan.planId(),
                evidenceReadyPlan.datasetId(),
                evidenceReadyPlan.protocolId(),
                executedCells,
                "");

        return new S11PredictionExecutionSnapshot("", results, executedPlan, "");
    }
}
