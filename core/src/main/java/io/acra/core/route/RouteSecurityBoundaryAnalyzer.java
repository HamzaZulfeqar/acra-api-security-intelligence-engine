package io.acra.core.route;

import io.acra.core.domain.authorization.AuthorizationDecision;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public final class RouteSecurityBoundaryAnalyzer {
    private final RouteNormalizationAnalyzer normalizationAnalyzer = new RouteNormalizationAnalyzer();

    public RouteSecurityBoundaryTrace analyze(String traceId, List<RouteBoundaryObservation> observations) {
        if (traceId == null || traceId.isBlank()) throw new IllegalArgumentException("traceId required");

        Map<RouteProcessingStage, RouteBoundaryObservation> byStage = new EnumMap<>(RouteProcessingStage.class);
        for (RouteBoundaryObservation observation : observations == null
                ? List.<RouteBoundaryObservation>of() : observations) {
            if (observation == null) throw new IllegalArgumentException("null boundary observation");
            if (byStage.putIfAbsent(observation.stage(), observation) != null) {
                throw new IllegalArgumentException("duplicate route processing stage: " + observation.stage());
            }
        }

        List<RouteBoundaryObservation> ordered = byStage.values().stream()
                .sorted(java.util.Comparator.comparingInt(value -> value.stage().ordinal()))
                .toList();

        List<RouteProcessingStage> missing = new ArrayList<>();
        for (RouteProcessingStage stage : RouteProcessingStage.values()) {
            if (!byStage.containsKey(stage)) missing.add(stage);
        }

        RouteNormalizationTrace normalization = normalizationAnalyzer.analyze(
                traceId + "-normalization",
                ordered.stream().map(RouteBoundaryObservation::routeObservation).toList());

        List<RouteBoundaryTransition> transitions = new ArrayList<>();
        for (int index = 1; index < ordered.size(); index++) {
            RouteBoundaryObservation from = ordered.get(index - 1);
            RouteBoundaryObservation to = ordered.get(index);
            RouteNormalizationTransition path = normalization.transitions().get(index - 1);
            transitions.add(compare(from, to, path));
        }

        TreeSet<String> evidence = new TreeSet<>();
        ordered.forEach(value -> evidence.addAll(value.evidenceIds()));

        RouteSecurityBoundaryState aggregate;
        if (ordered.size() < 2) {
            aggregate = RouteSecurityBoundaryState.INCONCLUSIVE;
        } else if (transitions.stream().anyMatch(value -> value.state() == RouteSecurityBoundaryState.INCONCLUSIVE)) {
            aggregate = RouteSecurityBoundaryState.INCONCLUSIVE;
        } else if (transitions.stream().anyMatch(value -> value.state() == RouteSecurityBoundaryState.COMBINED_DIVERGENCE)) {
            aggregate = RouteSecurityBoundaryState.COMBINED_DIVERGENCE;
        } else if (transitions.stream().anyMatch(value -> value.state() == RouteSecurityBoundaryState.AUTHORIZATION_BOUNDARY_CHANGE)) {
            aggregate = RouteSecurityBoundaryState.AUTHORIZATION_BOUNDARY_CHANGE;
        } else if (transitions.stream().anyMatch(value -> value.state() == RouteSecurityBoundaryState.ROUTING_DIVERGENCE)) {
            aggregate = RouteSecurityBoundaryState.ROUTING_DIVERGENCE;
        } else {
            aggregate = RouteSecurityBoundaryState.STABLE;
        }

        return new RouteSecurityBoundaryTrace(
                traceId,
                aggregate,
                ordered,
                transitions,
                List.copyOf(missing),
                List.copyOf(evidence));
    }

    private static RouteBoundaryTransition compare(
            RouteBoundaryObservation from,
            RouteBoundaryObservation to,
            RouteNormalizationTransition path) {
        boolean gap = to.stage().ordinal() - from.stage().ordinal() > 1;
        boolean methodChanged = from.method() != to.method();
        boolean hostChanged = !from.host().equalsIgnoreCase(to.host());
        boolean versionChanged = !from.apiVersion().equals(to.apiVersion());
        boolean expectedChanged = knownDifferent(from.expectedDecision(), to.expectedDecision());
        boolean observedChanged = knownDifferent(from.observedDecision(), to.observedDecision());

        boolean routingChanged = path.divergence() != RouteNormalizationDivergenceKind.NONE
                || methodChanged || hostChanged || versionChanged;
        boolean authorizationChanged = expectedChanged || observedChanged;
        boolean unknownAuthorization = from.expectedDecision() == AuthorizationDecision.UNKNOWN
                || to.expectedDecision() == AuthorizationDecision.UNKNOWN
                || from.observedDecision() == AuthorizationDecision.UNKNOWN
                || to.observedDecision() == AuthorizationDecision.UNKNOWN;

        List<String> reasons = new ArrayList<>(path.reasons());
        if (methodChanged) reasons.add("http-method-changed");
        if (hostChanged) reasons.add("host-changed");
        if (versionChanged) reasons.add("api-version-changed");
        if (expectedChanged) reasons.add("expected-authorization-decision-changed");
        if (observedChanged) reasons.add("observed-authorization-decision-changed");
        if (unknownAuthorization) reasons.add("authorization-context-incomplete");

        RouteSecurityBoundaryState state;
        if (gap || path.divergence() == RouteNormalizationDivergenceKind.INCONCLUSIVE || unknownAuthorization) {
            state = RouteSecurityBoundaryState.INCONCLUSIVE;
        } else if (routingChanged && authorizationChanged) {
            state = RouteSecurityBoundaryState.COMBINED_DIVERGENCE;
        } else if (authorizationChanged) {
            state = RouteSecurityBoundaryState.AUTHORIZATION_BOUNDARY_CHANGE;
        } else if (routingChanged) {
            state = RouteSecurityBoundaryState.ROUTING_DIVERGENCE;
        } else {
            state = RouteSecurityBoundaryState.STABLE;
        }

        TreeSet<String> evidence = new TreeSet<>();
        evidence.addAll(from.evidenceIds());
        evidence.addAll(to.evidenceIds());

        return new RouteBoundaryTransition(
                from.stage(),
                to.stage(),
                path.divergence(),
                methodChanged,
                hostChanged,
                versionChanged,
                expectedChanged,
                observedChanged,
                state,
                List.copyOf(evidence),
                reasons);
    }

    private static boolean knownDifferent(AuthorizationDecision first, AuthorizationDecision second) {
        return first != AuthorizationDecision.UNKNOWN
                && second != AuthorizationDecision.UNKNOWN
                && first != second;
    }
}
