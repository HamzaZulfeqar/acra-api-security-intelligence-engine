package io.acra.core.route;

import java.util.List;

public record RouteSecurityBoundaryTrace(
        String traceId,
        RouteSecurityBoundaryState state,
        List<RouteBoundaryObservation> observations,
        List<RouteBoundaryTransition> transitions,
        List<RouteProcessingStage> missingStages,
        List<String> evidenceIds) {

    public RouteSecurityBoundaryTrace {
        if (traceId == null || traceId.isBlank()) throw new IllegalArgumentException("traceId required");
        if (state == null) throw new IllegalArgumentException("state required");
        observations = List.copyOf(observations == null ? List.of() : observations);
        transitions = List.copyOf(transitions == null ? List.of() : transitions);
        missingStages = List.copyOf(missingStages == null ? List.of() : missingStages);
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
    }

    public long authorizationBoundaryChangeCount() {
        return transitions.stream().filter(RouteBoundaryTransition::authorizationChanged).count();
    }

    public long routingDivergenceCount() {
        return transitions.stream().filter(RouteBoundaryTransition::routingChanged).count();
    }
}
