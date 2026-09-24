package io.acra.core.product.routing;

import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.route.RouteAuthorizationAssessment;
import io.acra.core.route.RouteNormalizationTrace;
import io.acra.core.route.RouteSecurityBoundaryTrace;
import java.util.List;
import java.util.TreeMap;

public final class S8RoutingWorkspace {
    private final TreeMap<String, RouteNormalizationTrace> normalizationTraces = new TreeMap<>();
    private final TreeMap<String, RouteSecurityBoundaryTrace> boundaryTraces = new TreeMap<>();
    private final TreeMap<String, RouteAuthorizationAssessment> assessments = new TreeMap<>();
    private final TreeMap<String, FindingCandidate> candidates = new TreeMap<>();

    public synchronized void recordNormalizationTrace(RouteNormalizationTrace trace) {
        if (trace == null) throw new IllegalArgumentException("normalization trace required");
        normalizationTraces.put(trace.traceId(), trace);
    }

    public synchronized void recordBoundaryTrace(RouteSecurityBoundaryTrace trace) {
        if (trace == null) throw new IllegalArgumentException("boundary trace required");
        boundaryTraces.put(trace.traceId(), trace);
    }

    public synchronized void recordAssessment(RouteAuthorizationAssessment assessment) {
        if (assessment == null) throw new IllegalArgumentException("routing assessment required");
        assessments.put(assessment.assessmentId(), assessment);
    }

    public synchronized void recordCandidate(FindingCandidate candidate) {
        if (candidate == null) throw new IllegalArgumentException("finding candidate required");
        candidates.put(candidate.candidateId(), candidate);
    }

    public synchronized void clearRuntimeState() {
        normalizationTraces.clear();
        boundaryTraces.clear();
        assessments.clear();
        candidates.clear();
    }

    public synchronized S8RoutingProductSnapshot snapshot() {
        return new S8RoutingProductSnapshot(
                List.copyOf(normalizationTraces.values()),
                List.copyOf(boundaryTraces.values()),
                List.copyOf(assessments.values()),
                List.copyOf(candidates.values()));
    }
}
