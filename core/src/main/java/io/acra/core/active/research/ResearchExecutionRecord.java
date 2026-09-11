package io.acra.core.active.research;

import io.acra.core.domain.common.Validation;
import java.util.List;

public record ResearchExecutionRecord(
        String caseId,
        String executionId,
        String testId,
        String observationId,
        List<String> evidenceIds,
        ResearchGroundTruth groundTruth,
        ResearchPrediction prediction) {
    public ResearchExecutionRecord {
        caseId = Validation.requireNonBlank(caseId, "caseId");
        executionId = Validation.requireNonBlank(executionId, "executionId");
        testId = Validation.requireNonBlank(testId, "testId");
        observationId = Validation.requireNonBlank(observationId, "observationId");
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
        if (evidenceIds.isEmpty() || groundTruth == null || prediction == null) {
            throw new IllegalArgumentException("labelled research evidence required");
        }
    }
}
