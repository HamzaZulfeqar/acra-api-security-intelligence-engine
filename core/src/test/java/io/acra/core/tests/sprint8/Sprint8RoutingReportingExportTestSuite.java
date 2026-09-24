package io.acra.core.tests.sprint8;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.domain.http.HttpMethod;
import io.acra.core.engine.S8RoutingAssessmentEvaluator;
import io.acra.core.product.routing.S8RoutingWorkspace;
import io.acra.core.reporting.s8.S8RoutingJsonReporter;
import io.acra.core.reporting.s8.S8RoutingReportStatus;
import io.acra.core.route.RouteBoundaryObservation;
import io.acra.core.route.RouteNormalizationAnalyzer;
import io.acra.core.route.RouteObservationSource;
import io.acra.core.route.RouteProcessingStage;
import io.acra.core.route.RouteSecurityBoundaryAnalyzer;
import io.acra.core.route.RouteStageObservation;
import io.acra.core.tests.TestSupport;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class Sprint8RoutingReportingExportTestSuite {
    private static final Instant NOW = Instant.parse("2026-09-24T07:45:00Z");

    private Sprint8RoutingReportingExportTestSuite() { }

    public static void main(String[] args) {
        int assertions = 0;
        S8RoutingWorkspace workspace = fixtureWorkspace();

        var report = workspace.report(NOW);
        TestSupport.assertEquals(S8RoutingReportStatus.READY_FOR_REVIEW, report.status(),
                "non-empty routing snapshot is ready for review");
        assertions++;
        TestSupport.assertEquals(1, report.summary().normalizationTraceCount(),
                "report counts normalization trace");
        assertions++;
        TestSupport.assertEquals(1, report.summary().boundaryTraceCount(),
                "report counts boundary trace");
        assertions++;
        TestSupport.assertEquals(1, report.summary().combinedDivergenceCount(),
                "report counts combined divergence");
        assertions++;
        TestSupport.assertEquals(1, report.summary().assessmentCandidateCount(),
                "report counts routing assessment candidate");
        assertions++;
        TestSupport.assertEquals(1, report.summary().findingCandidateCount(),
                "report counts review-only finding candidate");
        assertions++;
        TestSupport.assertEquals(0, report.summary().confirmedFindingCount(),
                "Sprint 8 report must never auto-confirm finding candidates");
        assertions++;

        var later = workspace.report(NOW.plusSeconds(60));
        TestSupport.assertEquals(report.reportId(), later.reportId(),
                "report identity is deterministic and independent of generated timestamp");
        assertions++;

        var jsonA = workspace.exportJson(NOW);
        var jsonB = workspace.exportJson(NOW);
        System.out.println("SPRINT8_REPORT_JSON " + jsonA.content());
        TestSupport.assertEquals(jsonA.sha256(), jsonB.sha256(),
                "canonical JSON SHA-256 is stable for identical snapshot/time");
        assertions++;
        TestSupport.assertEquals(jsonA.content(), jsonB.content(),
                "canonical JSON content is stable");
        assertions++;
        TestSupport.assertContains(jsonA.content(), "s8-routing-report-v1",
                "JSON export includes explicit report version");
        assertions++;
        TestSupport.assertContains(jsonA.content(), "\"confirmedFindingCount\":0",
                "JSON export preserves zero confirmed findings");
        assertions++;
        TestSupport.assertNotContains(jsonA.content(), "s8-report-secret",
                "JSON export excludes raw bearer secret material");
        assertions++;

        var markdown = workspace.exportMarkdown(NOW);
        TestSupport.assertContains(markdown.content(), "ACRA Sprint 8 Routing Normalization Report",
                "Markdown export renders canonical report heading");
        assertions++;
        TestSupport.assertContains(markdown.content(), "Confirmed findings: 0",
                "Markdown export preserves review-only candidate boundary");
        assertions++;
        TestSupport.assertContains(markdown.content(), "Finding Candidates - Review Only",
                "Markdown export labels candidates as review-only");
        assertions++;

        String plugin = new S8RoutingJsonReporter().render(Map.of("report", report));
        TestSupport.assertEquals(jsonA.content(), plugin,
                "Reporter plugin adapter uses canonical Sprint 8 JSON export");
        assertions++;

        var empty = new S8RoutingWorkspace().report(NOW);
        TestSupport.assertEquals(S8RoutingReportStatus.NO_ROUTING_EVIDENCE, empty.status(),
                "empty workspace report does not invent routing evidence");
        assertions++;
        TestSupport.assertEquals(0, empty.summary().confirmedFindingCount(),
                "empty report also preserves zero confirmed findings");
        assertions++;

        System.out.println("SPRINT8_ROUTING_REPORTING_EXPORT PASS assertions=" + assertions);
    }

    private static S8RoutingWorkspace fixtureWorkspace() {
        S8RoutingWorkspace workspace = new S8RoutingWorkspace();

        var normalization = new RouteNormalizationAnalyzer().analyze(
                "S8-REPORT-NORMALIZATION",
                List.of(
                        new RouteStageObservation(
                                "S8-REPORT-RAW", RouteProcessingStage.RAW_URI, "/api//v1/s8/admin",
                                RouteObservationSource.OBSERVED, List.of("e-raw")),
                        new RouteStageObservation(
                                "S8-REPORT-PROXY", RouteProcessingStage.PROXY, "/api/v1/s8/admin",
                                RouteObservationSource.OBSERVED, List.of("e-proxy"))));
        workspace.recordNormalizationTrace(normalization);

        var boundary = new RouteSecurityBoundaryAnalyzer().analyze(
                "S8-REPORT-BOUNDARY",
                List.of(
                        new RouteBoundaryObservation(
                                "S8-REPORT-FRAMEWORK",
                                RouteProcessingStage.FRAMEWORK,
                                "/api/v1/s8/admin",
                                HttpMethod.GET,
                                "localhost",
                                "v1",
                                AuthorizationDecision.DENY,
                                AuthorizationDecision.DENY,
                                "s8-report-policy",
                                RouteObservationSource.CONFIGURED,
                                List.of("e-framework")),
                        new RouteBoundaryObservation(
                                "S8-REPORT-APPLICATION",
                                RouteProcessingStage.APPLICATION,
                                "/api//v1/s8/admin",
                                HttpMethod.GET,
                                "localhost",
                                "v1",
                                AuthorizationDecision.DENY,
                                AuthorizationDecision.ALLOW,
                                "s8-report-policy",
                                RouteObservationSource.OBSERVED,
                                List.of("e-application"))));
        workspace.recordBoundaryTrace(boundary);

        var assessment = new S8RoutingAssessmentEvaluator().evaluate(
                boundary.transitions().getFirst(),
                AuthorizationDecision.DENY,
                AuthorizationDecision.ALLOW,
                List.of("e-framework", "e-application"));
        workspace.recordAssessment(assessment);

        FindingCandidate candidate = new FindingCandidate(
                "fc-s8-report",
                FindingCandidateState.CANDIDATE,
                "acra-s8-report",
                List.of("S8-REPORT-TEST"),
                List.of("S8-REPORT-EXEC"),
                List.of("S8-REPORT-OBS"),
                List.of(assessment.assessmentId()),
                List.of("ROUTING_NORMALIZATION", "PATH_REPRESENTATION", "AUTHORIZATION_BOUNDARY"),
                "/api/v1/s8/admin",
                "route:s8-admin",
                "viewer-a",
                "tenant-a",
                AuthorizationDecision.DENY,
                AuthorizationDecision.ALLOW,
                List.of("e-framework", "e-application"),
                List.of(),
                List.of("s8-report-policy"),
                "HIGH",
                "Authorization: Bearer s8-report-secret; candidate is not automatically confirmed",
                FindingFingerprint.of(
                        "/api/v1/s8/admin",
                        "route:s8-admin",
                        "viewer-a",
                        "tenant-a",
                        "ROUTING_NORMALIZATION",
                        "CANDIDATE"));
        workspace.recordCandidate(candidate);
        return workspace;
    }
}
