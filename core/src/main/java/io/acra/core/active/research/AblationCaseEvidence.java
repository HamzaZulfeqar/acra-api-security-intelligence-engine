package io.acra.core.active.research;

import io.acra.core.domain.common.Validation;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record AblationCaseEvidence(
        String caseId,
        ResearchPrediction baselinePrediction,
        List<String> baselineEvidenceIds,
        List<AblationDimensionEvidence> dimensionEvidence) {

    public AblationCaseEvidence {
        caseId = Validation.requireNonBlank(caseId, "caseId");
        if (baselinePrediction == null) throw new IllegalArgumentException("baselinePrediction required");
        baselineEvidenceIds = List.copyOf(baselineEvidenceIds == null ? List.of() : baselineEvidenceIds).stream()
                .map(AblationCaseEvidence::required)
                .distinct()
                .sorted()
                .toList();
        if (baselineEvidenceIds.isEmpty()) throw new IllegalArgumentException("baselineEvidenceIds required");
        dimensionEvidence = List.copyOf(dimensionEvidence == null ? List.of() : dimensionEvidence);
        Map<AblationDimension, AblationDimensionEvidence> unique = new EnumMap<>(AblationDimension.class);
        for (AblationDimensionEvidence item : dimensionEvidence) {
            if (item == null) throw new IllegalArgumentException("dimension evidence required");
            if (unique.put(item.dimension(), item) != null) {
                throw new IllegalArgumentException("duplicate dimension evidence");
            }
        }
        dimensionEvidence = unique.values().stream()
                .sorted((left, right) -> Integer.compare(left.dimension().ordinal(), right.dimension().ordinal()))
                .toList();
    }

    public AblationDimensionEvidence evidenceFor(AblationDimension dimension) {
        return dimensionEvidence.stream()
                .filter(value -> value.dimension() == dimension)
                .findFirst()
                .orElse(null);
    }

    private static String required(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("evidenceId required");
        return value.strip();
    }
}
