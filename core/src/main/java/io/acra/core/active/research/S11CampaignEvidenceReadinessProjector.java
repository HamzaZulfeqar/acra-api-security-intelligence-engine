package io.acra.core.active.research;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class S11CampaignEvidenceReadinessProjector {

    public S11AblationCampaignPlan project(
            S11AblationCampaignPlan plan,
            List<AblationEvidenceBundle> bundles) {
        if (plan == null) throw new IllegalArgumentException("plan required");

        Map<String, AblationEvidenceBundle> byCase = new HashMap<>();
        for (AblationEvidenceBundle bundle : bundles == null ? List.<AblationEvidenceBundle>of() : bundles) {
            if (bundle == null) throw new IllegalArgumentException("evidence bundle required");
            if (byCase.put(bundle.caseId(), bundle) != null) {
                throw new IllegalArgumentException("duplicate evidence bundle case");
            }
        }

        List<AblationCampaignCell> projected = plan.cells().stream()
                .map(cell -> project(cell, byCase.get(cell.caseId())))
                .toList();

        return new S11AblationCampaignPlan(
                plan.planId(),
                plan.datasetId(),
                plan.protocolId(),
                projected,
                "");
    }

    private static AblationCampaignCell project(
            AblationCampaignCell cell,
            AblationEvidenceBundle bundle) {
        if (cell.state() == AblationCampaignCellState.EXECUTED) {
            throw new IllegalArgumentException("evidence-readiness projection cannot rewrite executed cells");
        }
        AblationCampaignCellState state = bundle != null && bundle.completeFor(cell.requiredDimensions())
                ? AblationCampaignCellState.EVIDENCE_READY
                : AblationCampaignCellState.PLANNED;
        return new AblationCampaignCell(
                cell.cellId(),
                cell.caseId(),
                cell.variantId(),
                cell.requiredDimensions(),
                state);
    }
}
