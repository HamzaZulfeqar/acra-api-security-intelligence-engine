package io.acra.core.active.research;

import io.acra.core.active.model.ConfigurationSnapshot;
import io.acra.core.active.model.ReproducibilityMetadata;
import io.acra.core.domain.common.Validation;
import java.util.List;

public record ResearchCaseDefinition(
        String caseId,
        ResearchCaseFamily family,
        ResearchGroundTruth groundTruth,
        ConfigurationSnapshot configuration,
        ReproducibilityMetadata reproducibility,
        List<String> requiredEvidence) {
    public ResearchCaseDefinition {
        caseId = Validation.requireNonBlank(caseId, "caseId");
        if (family == null || groundTruth == null || configuration == null || reproducibility == null) {
            throw new IllegalArgumentException("research case fields required");
        }
        requiredEvidence = List.copyOf(requiredEvidence == null ? List.of() : requiredEvidence);
    }
}
