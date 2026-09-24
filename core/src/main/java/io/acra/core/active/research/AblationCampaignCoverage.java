package io.acra.core.active.research;

import java.util.List;

public record AblationCampaignCoverage(
        int totalCells,
        int plannedCells,
        int evidenceReadyCells,
        int executedCells,
        int inconclusiveCells,
        int blockedCells) {

    public AblationCampaignCoverage {
        if (totalCells < 0 || plannedCells < 0 || evidenceReadyCells < 0 || executedCells < 0
                || inconclusiveCells < 0 || blockedCells < 0) {
            throw new IllegalArgumentException("negative campaign coverage count");
        }
        if (plannedCells + evidenceReadyCells + executedCells + inconclusiveCells + blockedCells != totalCells) {
            throw new IllegalArgumentException("campaign coverage counts must equal total");
        }
    }

    public static AblationCampaignCoverage from(List<AblationCampaignCell> cells) {
        int planned = 0;
        int ready = 0;
        int executed = 0;
        int inconclusive = 0;
        int blocked = 0;
        for (AblationCampaignCell cell : cells == null ? List.<AblationCampaignCell>of() : cells) {
            switch (cell.state()) {
                case PLANNED -> planned++;
                case EVIDENCE_READY -> ready++;
                case EXECUTED -> executed++;
                case INCONCLUSIVE -> inconclusive++;
                case BLOCKED -> blocked++;
            }
        }
        int total = cells == null ? 0 : cells.size();
        return new AblationCampaignCoverage(total, planned, ready, executed, inconclusive, blocked);
    }
}
