package io.acra.core.tests.sprint11;

import io.acra.core.active.research.AblationCampaignCell;
import io.acra.core.active.research.AblationCampaignCellState;
import io.acra.core.active.research.S11AblationCampaignPlan;
import io.acra.core.active.research.S11AblationProtocol;
import io.acra.core.tests.TestSupport;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.HashSet;

public final class Sprint11AblationCampaignPlanTestSuite {
    private Sprint11AblationCampaignPlanTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT11_ABLATION_CAMPAIGN_PLAN PASS assertions=" + assertions);
    }

    public static int run() {
        S11AblationCampaignPlan plan = S11AblationCampaignPlan.canonical();
        var protocol = S11AblationProtocol.canonical();
        int assertions = 0;

        TestSupport.assertEquals(120, plan.cells().size(),
                "15 dataset cases x 8 variants create exactly 120 cells");
        assertions++;
        TestSupport.assertEquals(120, plan.coverage().totalCells(),
                "campaign coverage denominator is fixed at 120");
        assertions++;
        TestSupport.assertEquals(120, plan.coverage().plannedCells(),
                "initial campaign keeps every cell planned");
        assertions++;
        TestSupport.assertEquals(0, plan.coverage().evidenceReadyCells(),
                "no cell is evidence-ready before evidence collection");
        assertions++;
        TestSupport.assertEquals(0, plan.coverage().executedCells(),
                "no cell is executed by campaign planning");
        assertions++;
        TestSupport.assertEquals(0, plan.coverage().inconclusiveCells(),
                "planning alone creates no prediction outcome");
        assertions++;
        TestSupport.assertEquals(0, plan.coverage().blockedCells(),
                "planning alone does not fabricate blocker classification");
        assertions++;

        TestSupport.assertEquals("S11-EVAL-001", plan.cells().getFirst().caseId(),
                "campaign begins with first deterministic dataset case");
        assertions++;
        TestSupport.assertEquals("A0", plan.cells().getFirst().variantId(),
                "campaign begins with A0");
        assertions++;
        TestSupport.assertEquals("S11-EVAL-015", plan.cells().getLast().caseId(),
                "campaign ends with final deterministic dataset case");
        assertions++;
        TestSupport.assertEquals("A7", plan.cells().getLast().variantId(),
                "campaign ends with A7");
        assertions++;

        HashSet<String> ids = new HashSet<>();
        for (AblationCampaignCell cell : plan.cells()) {
            TestSupport.assertTrue(ids.add(cell.cellId()),
                    "campaign cell identities are unique");
            assertions++;
            TestSupport.assertEquals(AblationCampaignCellState.PLANNED, cell.state(),
                    "canonical campaign cell starts PLANNED");
            assertions++;
            int order = Integer.parseInt(cell.variantId().substring(1));
            TestSupport.assertEquals(order, cell.requiredDimensions().size(),
                    "campaign cell dimensions match ablation order");
            assertions++;
        }

        for (int caseIndex = 1; caseIndex <= 15; caseIndex++) {
            String caseId = String.format("S11-EVAL-%03d", caseIndex);
            TestSupport.assertEquals(8, plan.cellsForCase(caseId).size(),
                    "every dataset case has all eight variants");
            assertions++;
        }
        for (int variantIndex = 0; variantIndex <= 7; variantIndex++) {
            String variantId = "A" + variantIndex;
            TestSupport.assertEquals(15, plan.cellsForVariant(variantId).size(),
                    "every variant spans all fifteen cases");
            assertions++;
            TestSupport.assertEquals(
                    protocol.variants().get(variantIndex).enabledDimensions(),
                    plan.cellsForVariant(variantId).getFirst().requiredDimensions(),
                    "campaign required dimensions match protocol");
            assertions++;
        }

        S11AblationCampaignPlan repeated = S11AblationCampaignPlan.canonical();
        TestSupport.assertEquals(plan.planId(), repeated.planId(),
                "campaign plan identity is deterministic");
        assertions++;
        TestSupport.assertEquals(plan.fingerprint(), repeated.fingerprint(),
                "campaign plan fingerprint is deterministic");
        assertions++;

        var duplicateCells = new ArrayList<>(plan.cells());
        duplicateCells.set(1, duplicateCells.getFirst());
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> new S11AblationCampaignPlan(
                        "",
                        plan.datasetId(),
                        plan.protocolId(),
                        duplicateCells,
                        ""),
                "duplicate case/variant campaign cell fails closed");
        assertions++;

        var wrongDimensions = new ArrayList<>(plan.cells());
        AblationCampaignCell original = wrongDimensions.get(1);
        wrongDimensions.set(1, new AblationCampaignCell(
                "",
                original.caseId(),
                original.variantId(),
                java.util.List.of(),
                original.state()));
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> new S11AblationCampaignPlan(
                        "",
                        plan.datasetId(),
                        plan.protocolId(),
                        wrongDimensions,
                        ""),
                "campaign dimension drift from protocol fails closed");
        assertions++;

        for (RecordComponent component : AblationCampaignCell.class.getRecordComponents()) {
            TestSupport.assertTrue(!component.getName().toLowerCase().contains("groundtruth"),
                    "campaign cell contains no ground-truth field");
            assertions++;
            TestSupport.assertTrue(!component.getName().toLowerCase().contains("prediction"),
                    "campaign cell contains no prediction field");
            assertions++;
        }

        return assertions;
    }
}
