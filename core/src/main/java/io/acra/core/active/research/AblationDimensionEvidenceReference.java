package io.acra.core.active.research;

import java.util.List;

public record AblationDimensionEvidenceReference(
        AblationDimension dimension,
        AblationEvidenceDisposition disposition,
        List<String> evidenceIds) {

    public AblationDimensionEvidenceReference {
        if (dimension == null || disposition == null) {
            throw new IllegalArgumentException("dimension/disposition required");
        }
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds).stream()
                .map(AblationDimensionEvidenceReference::required)
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
