package io.acra.core.route;

import java.util.List;

public record RouteNormalizationTransition(
        RouteProcessingStage fromStage,
        RouteProcessingStage toStage,
        RouteEquivalenceKind equivalence,
        RouteNormalizationDivergenceKind divergence,
        String canonicalFrom,
        String canonicalTo,
        List<String> evidenceIds,
        List<String> reasons) {

    public RouteNormalizationTransition {
        if (fromStage == null || toStage == null || equivalence == null || divergence == null) {
            throw new IllegalArgumentException("route transition metadata required");
        }
        if (fromStage.ordinal() >= toStage.ordinal()) {
            throw new IllegalArgumentException("route transition stages must move forward");
        }
        canonicalFrom = canonicalFrom == null ? "" : canonicalFrom;
        canonicalTo = canonicalTo == null ? "" : canonicalTo;
        evidenceIds = sorted(evidenceIds);
        reasons = sorted(reasons);
    }

    private static List<String> sorted(List<String> values) {
        return List.copyOf(values == null ? List.<String>of() : values).stream()
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .sorted()
                .toList();
    }
}
