package io.acra.core.active.research;

import java.util.List;

public record AblationDimensionEvidence(
        AblationDimension dimension,
        ResearchPrediction prediction,
        List<String> evidenceIds) {

    public AblationDimensionEvidence {
        if (dimension == null || prediction == null) {
            throw new IllegalArgumentException("dimension/prediction required");
        }
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds).stream()
                .map(AblationDimensionEvidence::required)
                .distinct()
                .sorted()
                .toList();
        if (evidenceIds.isEmpty()) throw new IllegalArgumentException("dimension evidenceIds required");
    }

    private static String required(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("evidenceId required");
        return value.strip();
    }
}
