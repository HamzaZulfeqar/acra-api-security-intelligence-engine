package io.acra.core.active.research;

import io.acra.core.domain.common.Validation;
import io.acra.core.security.TokenFingerprint;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record AblationEvidenceBundle(
        String caseId,
        List<String> baselineEvidenceIds,
        List<AblationDimensionEvidenceReference> dimensionEvidence,
        String fingerprint) {

    public AblationEvidenceBundle {
        caseId = Validation.requireNonBlank(caseId, "caseId");
        baselineEvidenceIds = clean(baselineEvidenceIds);
        if (baselineEvidenceIds.isEmpty()) throw new IllegalArgumentException("baseline evidence required");

        List<AblationDimensionEvidenceReference> supplied =
                List.copyOf(dimensionEvidence == null ? List.of() : dimensionEvidence);
        Map<AblationDimension, AblationDimensionEvidenceReference> unique =
                new EnumMap<>(AblationDimension.class);
        for (AblationDimensionEvidenceReference item : supplied) {
            if (item == null) throw new IllegalArgumentException("dimension evidence required");
            if (unique.put(item.dimension(), item) != null) {
                throw new IllegalArgumentException("duplicate dimension evidence reference");
            }
        }
        dimensionEvidence = unique.values().stream()
                .sorted((left, right) -> Integer.compare(
                        left.dimension().ordinal(), right.dimension().ordinal()))
                .toList();

        String calculated = TokenFingerprint.sha256(canonical(
                caseId, baselineEvidenceIds, dimensionEvidence));
        fingerprint = fingerprint == null || fingerprint.isBlank() ? calculated : fingerprint;
        if (!fingerprint.equals(calculated)) {
            throw new IllegalArgumentException("evidence bundle fingerprint mismatch");
        }
    }

    public boolean completeFor(List<AblationDimension> requiredDimensions) {
        if (baselineEvidenceIds.isEmpty()) return false;
        List<AblationDimension> required =
                List.copyOf(requiredDimensions == null ? List.of() : requiredDimensions);
        return required.stream().allMatch(this::hasDimension);
    }

    public List<AblationDimension> missingDimensions(List<AblationDimension> requiredDimensions) {
        return List.copyOf(requiredDimensions == null ? List.<AblationDimension>of() : requiredDimensions).stream()
                .filter(dimension -> !hasDimension(dimension))
                .toList();
    }

    public AblationDimensionEvidenceReference evidenceFor(AblationDimension dimension) {
        return dimensionEvidence.stream()
                .filter(value -> value.dimension() == dimension)
                .findFirst()
                .orElse(null);
    }

    private boolean hasDimension(AblationDimension dimension) {
        return evidenceFor(dimension) != null;
    }

    private static List<String> clean(List<String> values) {
        return List.copyOf(values == null ? List.of() : values).stream()
                .map(value -> Validation.requireNonBlank(value, "evidenceId"))
                .distinct()
                .sorted()
                .toList();
    }

    private static String canonical(
            String caseId,
            List<String> baselineEvidenceIds,
            List<AblationDimensionEvidenceReference> dimensionEvidence) {
        StringBuilder out = new StringBuilder(caseId).append('|').append(baselineEvidenceIds).append('\n');
        for (AblationDimensionEvidenceReference value : dimensionEvidence) {
            out.append(value.dimension()).append('|')
                    .append(value.disposition()).append('|')
                    .append(value.evidenceIds()).append('\n');
        }
        return out.toString();
    }
}
