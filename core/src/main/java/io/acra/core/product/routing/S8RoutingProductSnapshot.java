package io.acra.core.product.routing;

import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.route.RouteAuthorizationAssessment;
import io.acra.core.route.RouteNormalizationTrace;
import io.acra.core.route.RouteSecurityBoundaryTrace;
import java.util.List;

public record S8RoutingProductSnapshot(
        List<RouteNormalizationTrace> normalizationTraces,
        List<RouteSecurityBoundaryTrace> boundaryTraces,
        List<RouteAuthorizationAssessment> assessments,
        List<FindingCandidate> candidates) {

    public S8RoutingProductSnapshot {
        normalizationTraces = List.copyOf(normalizationTraces == null ? List.of() : normalizationTraces);
        boundaryTraces = List.copyOf(boundaryTraces == null ? List.of() : boundaryTraces);
        assessments = List.copyOf(assessments == null ? List.of() : assessments);
        candidates = List.copyOf(candidates == null ? List.of() : candidates);
    }

    public long candidateCount() {
        return candidates.stream()
                .filter(value -> value.state() == io.acra.core.domain.finding.FindingCandidateState.CANDIDATE)
                .count();
    }

    public long inconclusiveAssessmentCount() {
        return assessments.stream()
                .filter(value -> value.state() == io.acra.core.route.RouteAuthorizationAssessmentState.INCONCLUSIVE)
                .count();
    }
}
