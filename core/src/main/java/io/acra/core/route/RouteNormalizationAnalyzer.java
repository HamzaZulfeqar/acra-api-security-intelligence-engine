package io.acra.core.route;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public final class RouteNormalizationAnalyzer {
    private final RouteEquivalenceEngine equivalence = new RouteEquivalenceEngine();

    public RouteNormalizationTrace analyze(String traceId, List<RouteStageObservation> observations) {
        if (traceId == null || traceId.isBlank()) throw new IllegalArgumentException("traceId required");

        Map<RouteProcessingStage, RouteStageObservation> byStage = new EnumMap<>(RouteProcessingStage.class);
        for (RouteStageObservation observation : observations == null
                ? List.<RouteStageObservation>of() : observations) {
            if (observation == null) throw new IllegalArgumentException("null stage observation");
            if (byStage.putIfAbsent(observation.stage(), observation) != null) {
                throw new IllegalArgumentException("duplicate route processing stage: " + observation.stage());
            }
        }

        List<RouteStageObservation> ordered = byStage.values().stream()
                .sorted(java.util.Comparator.comparingInt(value -> value.stage().ordinal()))
                .toList();

        List<RouteProcessingStage> missing = new ArrayList<>();
        for (RouteProcessingStage stage : RouteProcessingStage.values()) {
            if (!byStage.containsKey(stage)) missing.add(stage);
        }

        List<RouteNormalizationTransition> transitions = new ArrayList<>();
        for (int index = 1; index < ordered.size(); index++) {
            RouteStageObservation from = ordered.get(index - 1);
            RouteStageObservation to = ordered.get(index);
            transitions.add(compare(from, to));
        }

        TreeSet<String> evidence = new TreeSet<>();
        ordered.forEach(value -> evidence.addAll(value.evidenceIds()));

        RouteNormalizationTraceState state;
        if (ordered.size() < 2) {
            state = RouteNormalizationTraceState.INCONCLUSIVE;
        } else if (missing.isEmpty()) {
            state = RouteNormalizationTraceState.COMPLETE;
        } else {
            state = RouteNormalizationTraceState.PARTIAL;
        }

        return new RouteNormalizationTrace(
                traceId,
                state,
                ordered,
                transitions,
                List.copyOf(missing),
                List.copyOf(evidence));
    }

    private RouteNormalizationTransition compare(RouteStageObservation from, RouteStageObservation to) {
        RouteEquivalenceResult result = equivalence.compare(from.path(), to.path());
        boolean stageGap = to.stage().ordinal() - from.stage().ordinal() > 1;

        List<String> reasons = new ArrayList<>(result.reasons());
        if (stageGap) reasons.add("missing-intermediate-stage");

        RouteNormalizationDivergenceKind divergence;
        if (stageGap || result.kind() == RouteEquivalenceKind.UNKNOWN) {
            divergence = RouteNormalizationDivergenceKind.INCONCLUSIVE;
        } else {
            divergence = switch (result.kind()) {
                case SYNTACTICALLY_EQUAL -> RouteNormalizationDivergenceKind.NONE;
                case CANONICALLY_EQUIVALENT -> RouteNormalizationDivergenceKind.REPRESENTATION_CHANGE;
                case SAME_FAMILY -> RouteNormalizationDivergenceKind.FAMILY_VARIATION;
                case DIFFERENT -> RouteNormalizationDivergenceKind.CANONICAL_DIVERGENCE;
                case UNKNOWN -> RouteNormalizationDivergenceKind.INCONCLUSIVE;
            };
        }

        TreeSet<String> evidence = new TreeSet<>();
        evidence.addAll(from.evidenceIds());
        evidence.addAll(to.evidenceIds());

        return new RouteNormalizationTransition(
                from.stage(),
                to.stage(),
                result.kind(),
                divergence,
                result.canonicalA(),
                result.canonicalB(),
                List.copyOf(evidence),
                reasons);
    }
}
