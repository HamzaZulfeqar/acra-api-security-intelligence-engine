package io.acra.core.active.research;

import io.acra.core.domain.common.Validation;
import io.acra.core.security.TokenFingerprint;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record S11AblationCampaignPlan(
        String planId,
        String datasetId,
        String protocolId,
        List<AblationCampaignCell> cells,
        String fingerprint) {

    public S11AblationCampaignPlan {
        datasetId = Validation.requireNonBlank(datasetId, "datasetId");
        protocolId = Validation.requireNonBlank(protocolId, "protocolId");
        cells = List.copyOf(cells == null ? List.of() : cells).stream()
                .sorted(Comparator.comparing(AblationCampaignCell::caseId)
                        .thenComparingInt(value -> variantOrder(value.variantId())))
                .toList();
        validate(datasetId, protocolId, cells);
        String structural = canonicalStructure(datasetId, protocolId, cells);
        String expectedPlanId = "s11-campaign-" + TokenFingerprint.sha256(structural).substring(0, 24);
        planId = planId == null || planId.isBlank() ? expectedPlanId : planId;
        if (!planId.equals(expectedPlanId)) throw new IllegalArgumentException("campaign plan identity mismatch");
        String calculated = TokenFingerprint.sha256(canonicalState(structural, cells));
        fingerprint = fingerprint == null || fingerprint.isBlank() ? calculated : fingerprint;
        if (!fingerprint.equals(calculated)) throw new IllegalArgumentException("campaign fingerprint mismatch");
    }

    public static S11AblationCampaignPlan canonical() {
        S11EvaluationDatasetManifest dataset = S11EvaluationDatasetManifest.canonical();
        S11AblationProtocol protocol = S11AblationProtocol.canonical();
        List<AblationCampaignCell> cells = new ArrayList<>();
        for (ResearchDatasetCase item : dataset.cases()) {
            for (AblationVariant variant : protocol.variants()) {
                cells.add(new AblationCampaignCell(
                        "",
                        item.caseId(),
                        variant.variantId(),
                        variant.enabledDimensions(),
                        AblationCampaignCellState.PLANNED));
            }
        }
        return new S11AblationCampaignPlan(
                "",
                dataset.datasetId(),
                protocol.protocolId(),
                cells,
                "");
    }

    public AblationCampaignCoverage coverage() {
        return AblationCampaignCoverage.from(cells);
    }

    public List<AblationCampaignCell> cellsForCase(String caseId) {
        return cells.stream().filter(value -> value.caseId().equals(caseId)).toList();
    }

    public List<AblationCampaignCell> cellsForVariant(String variantId) {
        return cells.stream().filter(value -> value.variantId().equals(variantId)).toList();
    }

    private static void validate(
            String datasetId,
            String protocolId,
            List<AblationCampaignCell> cells) {
        S11EvaluationDatasetManifest dataset = S11EvaluationDatasetManifest.canonical();
        S11AblationProtocol protocol = S11AblationProtocol.canonical();
        if (!dataset.datasetId().equals(datasetId)) {
            throw new IllegalArgumentException("campaign dataset mismatch");
        }
        if (!protocol.protocolId().equals(protocolId)) {
            throw new IllegalArgumentException("campaign protocol mismatch");
        }
        if (cells.size() != dataset.cases().size() * protocol.variants().size()) {
            throw new IllegalArgumentException("campaign must enumerate the full case-by-variant matrix");
        }

        Map<String, ResearchDatasetCase> cases = new HashMap<>();
        for (ResearchDatasetCase item : dataset.cases()) cases.put(item.caseId(), item);
        Map<String, AblationVariant> variants = new HashMap<>();
        for (AblationVariant variant : protocol.variants()) variants.put(variant.variantId(), variant);

        Set<String> pairIds = new HashSet<>();
        for (AblationCampaignCell cell : cells) {
            if (!cases.containsKey(cell.caseId())) throw new IllegalArgumentException("unknown campaign case");
            AblationVariant variant = variants.get(cell.variantId());
            if (variant == null) throw new IllegalArgumentException("unknown campaign variant");
            if (!variant.enabledDimensions().equals(cell.requiredDimensions())) {
                throw new IllegalArgumentException("campaign required dimensions do not match protocol");
            }
            if (!pairIds.add(cell.caseId() + "|" + cell.variantId())) {
                throw new IllegalArgumentException("duplicate campaign case/variant cell");
            }
        }
    }

    private static int variantOrder(String variantId) {
        if (variantId == null || !variantId.matches("A[0-7]")) {
            throw new IllegalArgumentException("invalid ablation variant id");
        }
        return Integer.parseInt(variantId.substring(1));
    }

    private static String canonicalStructure(
            String datasetId,
            String protocolId,
            List<AblationCampaignCell> cells) {
        StringBuilder out = new StringBuilder(datasetId).append('|').append(protocolId).append('\n');
        for (AblationCampaignCell cell : cells) {
            out.append(cell.caseId()).append('|')
                    .append(cell.variantId()).append('|')
                    .append(cell.requiredDimensions()).append('|')
                    .append(cell.cellId()).append('\n');
        }
        return out.toString();
    }

    private static String canonicalState(String structure, List<AblationCampaignCell> cells) {
        StringBuilder out = new StringBuilder(structure);
        for (AblationCampaignCell cell : cells) {
            out.append(cell.cellId()).append('=').append(cell.state()).append('\n');
        }
        return out.toString();
    }
}
