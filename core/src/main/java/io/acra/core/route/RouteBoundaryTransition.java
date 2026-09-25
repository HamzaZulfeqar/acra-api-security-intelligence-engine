package io.acra.core.route;

import java.util.List;

public record RouteBoundaryTransition(
        RouteProcessingStage fromStage,
        RouteProcessingStage toStage,
        RouteNormalizationDivergenceKind pathDivergence,
        boolean methodChanged,
        boolean hostChanged,
        boolean apiVersionChanged,
        boolean expectedAuthorizationChanged,
        boolean observedAuthorizationChanged,
        RouteSecurityBoundaryState state,
        List<String> evidenceIds,
        List<String> reasons) {

    public RouteBoundaryTransition {
        if (fromStage == null || toStage == null || pathDivergence == null || state == null) {
            throw new IllegalArgumentException("route boundary transition metadata required");
        }
        if (fromStage.ordinal() >= toStage.ordinal()) {
            throw new IllegalArgumentException("route boundary transition stages must move forward");
        }
        evidenceIds = sorted(evidenceIds);
        reasons = sorted(reasons);
    }

    public boolean routingChanged() {
        return pathDivergence != RouteNormalizationDivergenceKind.NONE
                || methodChanged
                || hostChanged
                || apiVersionChanged;
    }

    public boolean authorizationChanged() {
        return expectedAuthorizationChanged || observedAuthorizationChanged;
    }

    private static List<String> sorted(List<String> values) {
        return List.copyOf(values == null ? List.<String>of() : values).stream()
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .sorted()
                .toList();
    }
}
