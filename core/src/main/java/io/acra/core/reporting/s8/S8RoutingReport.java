package io.acra.core.reporting.s8;

import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.route.RouteAuthorizationAssessment;
import io.acra.core.route.RouteNormalizationTrace;
import io.acra.core.route.RouteSecurityBoundaryTrace;
import java.time.Instant;
import java.util.List;

public record S8RoutingReport(
        String reportId,
        String reportVersion,
        S8RoutingReportStatus status,
        Instant generatedAt,
        S8RoutingReportSummary summary,
        List<RouteNormalizationTrace> normalizationTraces,
        List<RouteSecurityBoundaryTrace> boundaryTraces,
        List<RouteAuthorizationAssessment> assessments,
        List<FindingCandidate> findingCandidates,
        List<String> evidenceIds,
        List<String> limitations) {

    public S8RoutingReport {
        if (reportId == null || reportId.isBlank()) throw new IllegalArgumentException("reportId required");
        if (reportVersion == null || reportVersion.isBlank()) throw new IllegalArgumentException("reportVersion required");
        status = status == null ? S8RoutingReportStatus.NO_ROUTING_EVIDENCE : status;
        if (generatedAt == null) throw new IllegalArgumentException("generatedAt required");
        if (summary == null) throw new IllegalArgumentException("summary required");
        normalizationTraces = List.copyOf(normalizationTraces == null ? List.of() : normalizationTraces);
        boundaryTraces = List.copyOf(boundaryTraces == null ? List.of() : boundaryTraces);
        assessments = List.copyOf(assessments == null ? List.of() : assessments);
        findingCandidates = List.copyOf(findingCandidates == null ? List.of() : findingCandidates);
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
        limitations = List.copyOf(limitations == null ? List.of() : limitations);
    }
}
