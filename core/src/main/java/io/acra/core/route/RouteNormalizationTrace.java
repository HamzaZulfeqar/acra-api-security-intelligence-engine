package io.acra.core.route;

import java.util.List;

public record RouteNormalizationTrace(
        String traceId,
        RouteNormalizationTraceState state,
        List<RouteStageObservation> observations,
        List<RouteNormalizationTransition> transitions,
        List<RouteProcessingStage> missingStages,
        List<String> evidenceIds) {

    public RouteNormalizationTrace {
        if (traceId == null || traceId.isBlank()) throw new IllegalArgumentException("traceId required");
        if (state == null) throw new IllegalArgumentException("state required");
        observations = List.copyOf(observations == null ? List.of() : observations);
        transitions = List.copyOf(transitions == null ? List.of() : transitions);
        missingStages = List.copyOf(missingStages == null ? List.of() : missingStages);
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
    }

    public long canonicalDivergenceCount() {
        return transitions.stream()
                .filter(value -> value.divergence() == RouteNormalizationDivergenceKind.CANONICAL_DIVERGENCE)
                .count();
    }

    public long inconclusiveTransitionCount() {
        return transitions.stream()
                .filter(value -> value.divergence() == RouteNormalizationDivergenceKind.INCONCLUSIVE)
                .count();
    }
}
