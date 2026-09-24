package io.acra.core.active.research;

import io.acra.core.domain.common.Validation;
import io.acra.core.security.TokenFingerprint;
import java.util.List;

public record AblationCampaignCell(
        String cellId,
        String caseId,
        String variantId,
        List<AblationDimension> requiredDimensions,
        AblationCampaignCellState state) {

    public AblationCampaignCell {
        caseId = Validation.requireNonBlank(caseId, "caseId");
        variantId = Validation.requireNonBlank(variantId, "variantId");
        requiredDimensions = List.copyOf(requiredDimensions == null ? List.of() : requiredDimensions);
        state = state == null ? AblationCampaignCellState.PLANNED : state;
        String expected = deterministicId(caseId, variantId, requiredDimensions);
        cellId = cellId == null || cellId.isBlank() ? expected : cellId;
        if (!cellId.equals(expected)) throw new IllegalArgumentException("campaign cell identity mismatch");
    }

    public static String deterministicId(
            String caseId,
            String variantId,
            List<AblationDimension> requiredDimensions) {
        String material = caseId + "|" + variantId + "|" + requiredDimensions;
        return "s11-cell-" + TokenFingerprint.sha256(material).substring(0, 24);
    }
}
