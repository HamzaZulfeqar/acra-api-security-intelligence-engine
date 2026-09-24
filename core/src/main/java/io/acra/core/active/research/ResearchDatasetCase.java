package io.acra.core.active.research;

import io.acra.core.domain.common.Validation;

public record ResearchDatasetCase(
        String caseId,
        String sourceGroundTruthId,
        String sourceCaseId,
        String family,
        ResearchGroundTruth groundTruth) {

    public ResearchDatasetCase {
        caseId = Validation.requireNonBlank(caseId, "caseId");
        sourceGroundTruthId = Validation.requireNonBlank(sourceGroundTruthId, "sourceGroundTruthId");
        sourceCaseId = Validation.requireNonBlank(sourceCaseId, "sourceCaseId");
        family = Validation.requireNonBlank(family, "family");
        if (groundTruth == null) throw new IllegalArgumentException("groundTruth required");
    }
}
