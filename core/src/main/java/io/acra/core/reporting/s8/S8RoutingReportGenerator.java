package io.acra.core.reporting.s8;

import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.product.routing.S8RoutingProductSnapshot;
import io.acra.core.route.RouteAuthorizationAssessment;
import io.acra.core.route.RouteAuthorizationAssessmentState;
import io.acra.core.route.RouteNormalizationTrace;
import io.acra.core.route.RouteNormalizationTraceState;
import io.acra.core.route.RouteSecurityBoundaryState;
import io.acra.core.route.RouteSecurityBoundaryTrace;
import io.acra.core.security.TokenFingerprint;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

public final class S8RoutingReportGenerator {
    public static final String REPORT_VERSION = "s8-routing-report-v1";

    public S8RoutingReport generate(S8RoutingProductSnapshot snapshot, Instant generatedAt) {
        if (snapshot == null || generatedAt == null) {
            throw new IllegalArgumentException("snapshot/generatedAt required");
        }

        List<RouteNormalizationTrace> normalization = snapshot.normalizationTraces().stream()
                .sorted(Comparator.comparing(RouteNormalizationTrace::traceId)).toList();
        List<RouteSecurityBoundaryTrace> boundaries = snapshot.boundaryTraces().stream()
                .sorted(Comparator.comparing(RouteSecurityBoundaryTrace::traceId)).toList();
        List<RouteAuthorizationAssessment> assessments = snapshot.assessments().stream()
                .sorted(Comparator.comparing(RouteAuthorizationAssessment::assessmentId)).toList();
        List<FindingCandidate> candidates = snapshot.candidates().stream()
                .sorted(Comparator.comparing(FindingCandidate::candidateId)).toList();

        int complete = (int) normalization.stream()
                .filter(value -> value.state() == RouteNormalizationTraceState.COMPLETE).count();
        int routing = (int) boundaries.stream().flatMap(value -> value.transitions().stream())
                .filter(value -> value.routingChanged()).count();
        int authorization = (int) boundaries.stream().flatMap(value -> value.transitions().stream())
                .filter(value -> value.authorizationChanged()).count();
        int combined = (int) boundaries.stream().flatMap(value -> value.transitions().stream())
                .filter(value -> value.state() == RouteSecurityBoundaryState.COMBINED_DIVERGENCE).count();
        int inconclusive = (int) boundaries.stream().flatMap(value -> value.transitions().stream())
                .filter(value -> value.state() == RouteSecurityBoundaryState.INCONCLUSIVE).count();
        int assessmentCandidates = (int) assessments.stream()
                .filter(value -> value.state() == RouteAuthorizationAssessmentState.CANDIDATE).count();
        int findingCandidates = (int) candidates.stream()
                .filter(value -> value.state() == FindingCandidateState.CANDIDATE).count();

        S8RoutingReportSummary summary = new S8RoutingReportSummary(
                normalization.size(), complete, boundaries.size(), routing, authorization, combined, inconclusive,
                assessments.size(), assessmentCandidates, findingCandidates, 0);

        TreeSet<String> evidence = new TreeSet<>();
        normalization.forEach(value -> evidence.addAll(value.evidenceIds()));
        boundaries.forEach(value -> evidence.addAll(value.evidenceIds()));
        assessments.forEach(value -> evidence.addAll(value.evidenceIds()));
        candidates.forEach(value -> evidence.addAll(value.supportingEvidenceIds()));

        String material = REPORT_VERSION
                + "|" + normalization.stream().map(RouteNormalizationTrace::traceId).toList()
                + "|" + boundaries.stream().map(RouteSecurityBoundaryTrace::traceId).toList()
                + "|" + assessments.stream().map(RouteAuthorizationAssessment::assessmentId).toList()
                + "|" + candidates.stream().map(FindingCandidate::candidateId).toList();

        List<String> limitations = List.of(
                "FindingCandidate is a review candidate, not a confirmed vulnerability.",
                "Routing divergence is evidence about request interpretation, not vulnerability severity.",
                "Missing processing-stage observations remain unknown and prevent causal attribution.",
                "Normal export sanitization excludes raw credentials and authentication secrets.",
                "Controlled localhost routing validation does not establish real-world proxy or scanner accuracy.");

        boolean empty = normalization.isEmpty() && boundaries.isEmpty() && assessments.isEmpty() && candidates.isEmpty();
        return new S8RoutingReport(
                "s8-report-" + TokenFingerprint.sha256(material).substring(0, 24),
                REPORT_VERSION,
                empty ? S8RoutingReportStatus.NO_ROUTING_EVIDENCE : S8RoutingReportStatus.READY_FOR_REVIEW,
                generatedAt,
                summary,
                normalization,
                boundaries,
                assessments,
                candidates,
                List.copyOf(evidence),
                limitations);
    }
}
