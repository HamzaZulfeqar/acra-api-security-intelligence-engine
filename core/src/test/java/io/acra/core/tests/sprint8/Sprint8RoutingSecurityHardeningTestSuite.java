package io.acra.core.tests.sprint8;

import io.acra.core.active.execution.UriMutationAdapter;
import io.acra.core.active.model.Mutation;
import io.acra.core.active.model.MutationLocation;
import io.acra.core.active.model.MutationType;
import io.acra.core.active.model.SafetyClass;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.http.HttpMethod;
import io.acra.core.engine.S8RoutingAssessmentEvaluator;
import io.acra.core.product.routing.S8RoutingWorkspace;
import io.acra.core.route.RouteAuthorizationAssessmentState;
import io.acra.core.route.RouteBoundaryObservation;
import io.acra.core.route.RouteObservationSource;
import io.acra.core.route.RouteProcessingStage;
import io.acra.core.route.RouteSecurityBoundaryAnalyzer;
import io.acra.core.route.RouteSecurityBoundaryState;
import io.acra.core.route.RouteStageObservation;
import io.acra.core.security.UniversalRedactor;
import io.acra.core.tests.TestSupport;
import java.time.Instant;
import java.util.List;

public final class Sprint8RoutingSecurityHardeningTestSuite {
    private Sprint8RoutingSecurityHardeningTestSuite() { }

    public static void main(String[] args) {
        int assertions = 0;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> stage("/a?token=secret", RouteProcessingStage.RAW_URI, List.of("e")),
                "route stage query material rejected");
        assertions++;
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> stage("/a#fragment", RouteProcessingStage.RAW_URI, List.of("e")),
                "route stage fragment material rejected");
        assertions++;
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> stage("/a", RouteProcessingStage.RAW_URI, List.of()),
                "route stage without evidence rejected");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> boundary("/a", "https://api.example.test/x", RouteProcessingStage.PROXY,
                        AuthorizationDecision.DENY, List.of("e")),
                "route boundary host with scheme/path rejected");
        assertions++;
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> boundary("/a", "user@api.example.test", RouteProcessingStage.PROXY,
                        AuthorizationDecision.DENY, List.of("e")),
                "route boundary user-info rejected");
        assertions++;

        RouteSecurityBoundaryAnalyzer analyzer = new RouteSecurityBoundaryAnalyzer();
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> analyzer.analyze("dup", List.of(
                        boundary("/a", "api.example.test", RouteProcessingStage.PROXY,
                                AuthorizationDecision.DENY, List.of("e1")),
                        boundary("/b", "api.example.test", RouteProcessingStage.PROXY,
                                AuthorizationDecision.DENY, List.of("e2")))),
                "duplicate processing stage rejected");
        assertions++;

        var unknown = analyzer.analyze("unknown-auth", List.of(
                new RouteBoundaryObservation(
                        "u1", RouteProcessingStage.PROXY, "/a", HttpMethod.GET, "api.example.test", "v1",
                        AuthorizationDecision.DENY, AuthorizationDecision.UNKNOWN, "p",
                        RouteObservationSource.OBSERVED, List.of("e1")),
                boundary("/a", "api.example.test", RouteProcessingStage.GATEWAY,
                        AuthorizationDecision.DENY, List.of("e2"))));
        TestSupport.assertEquals(RouteSecurityBoundaryState.INCONCLUSIVE, unknown.state(),
                "unknown authorization context fails closed");
        assertions++;

        var gap = analyzer.analyze("stage-gap", List.of(
                boundary("/a", "api.example.test", RouteProcessingStage.RAW_URI,
                        AuthorizationDecision.DENY, List.of("e1")),
                boundary("/a", "api.example.test", RouteProcessingStage.APPLICATION,
                        AuthorizationDecision.ALLOW, List.of("e2"))));
        TestSupport.assertEquals(RouteSecurityBoundaryState.INCONCLUSIVE, gap.state(),
                "missing intermediary stages prevent causal attribution");
        assertions++;

        var authOnly = analyzer.analyze("auth-only", List.of(
                boundary("/a", "api.example.test", RouteProcessingStage.FRAMEWORK,
                        AuthorizationDecision.DENY, List.of("e1")),
                boundary("/a", "api.example.test", RouteProcessingStage.APPLICATION,
                        AuthorizationDecision.ALLOW, List.of("e2"))));
        var authOnlyAssessment = new S8RoutingAssessmentEvaluator().evaluate(
                authOnly.transitions().getFirst(),
                AuthorizationDecision.DENY,
                AuthorizationDecision.ALLOW,
                List.of("e1", "e2"));
        TestSupport.assertEquals(RouteAuthorizationAssessmentState.NO_VIOLATION, authOnlyAssessment.state(),
                "route-stable authorization change is not promoted as Sprint 8 routing candidate");
        assertions++;

        var routeAndAuth = analyzer.analyze("route-auth", List.of(
                boundary("/api/v1/admin", "api.example.test", RouteProcessingStage.FRAMEWORK,
                        AuthorizationDecision.DENY, List.of("e1")),
                boundary("/api//v1/admin", "api.example.test", RouteProcessingStage.APPLICATION,
                        AuthorizationDecision.ALLOW, List.of("e2"))));
        var candidate = new S8RoutingAssessmentEvaluator().evaluate(
                routeAndAuth.transitions().getFirst(),
                AuthorizationDecision.DENY,
                AuthorizationDecision.ALLOW,
                List.of("e1", "e2"));
        TestSupport.assertEquals(RouteAuthorizationAssessmentState.CANDIDATE, candidate.state(),
                "routing change plus DENY-to-ALLOW mismatch can become review candidate");
        assertions++;

        UriMutationAdapter uri = new UriMutationAdapter();
        String equivalent = uri.apply("/api/v1/s8/admin", mutation(
                "/api/v1/s8/admin", "/api//v1/s8/admin"));
        TestSupport.assertEquals("/api//v1/s8/admin", equivalent,
                "explicit equivalent route representation is accepted");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> uri.apply("/api/v1/s8/admin", mutation(
                        "/api/v1/s8/admin", "/private/root")),
                "non-equivalent route representation rejected");
        assertions++;
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> uri.apply("/api/v1/s8/admin", mutation(
                        "/api/v1/s8/admin", "http://outside.test/admin")),
                "URI authority escape rejected");
        assertions++;
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> uri.apply("/api/v1/s8/admin", mutation(
                        "/api/v1/s8/admin", "/api/v1/s8/admin#fragment")),
                "URI fragment injection rejected");
        assertions++;

        String secret = "s8-redactor-secret";
        String embedded = "{\"rationale\":\"Authorization: Bearer " + secret
                + "; review-only\",\"reportVersion\":\"s8-routing-report-v1\","
                + "\"summary\":{\"confirmedFindingCount\":0}}";
        String redacted = new UniversalRedactor().redactText(embedded);
        TestSupport.assertNotContains(redacted, secret,
                "embedded bearer secret is redacted");
        assertions++;
        TestSupport.assertContains(redacted, "s8-routing-report-v1",
                "redaction preserves subsequent report fields");
        assertions++;

        var emptyReport = new S8RoutingWorkspace().report(Instant.parse("2026-09-24T08:00:00Z"));
        TestSupport.assertEquals(0, emptyReport.summary().confirmedFindingCount(),
                "routing report cannot auto-confirm findings");
        assertions++;

        System.out.println("SPRINT8_ROUTING_SECURITY_HARDENING PASS assertions=" + assertions);
    }

    private static RouteStageObservation stage(
            String path, RouteProcessingStage stage, List<String> evidence) {
        return new RouteStageObservation(
                "stage-" + stage, stage, path, RouteObservationSource.OBSERVED, evidence);
    }

    private static RouteBoundaryObservation boundary(
            String path,
            String host,
            RouteProcessingStage stage,
            AuthorizationDecision observed,
            List<String> evidence) {
        return new RouteBoundaryObservation(
                "boundary-" + stage + "-" + path,
                stage,
                path,
                HttpMethod.GET,
                host,
                "v1",
                AuthorizationDecision.DENY,
                observed,
                "s8-security-policy",
                RouteObservationSource.OBSERVED,
                evidence);
    }

    private static Mutation mutation(String original, String replacement) {
        return new Mutation(
                "S8-SEC-MUT-" + Integer.toHexString((original + replacement).hashCode()),
                MutationType.EQUIVALENT_ROUTE_REPRESENTATION,
                MutationLocation.URI_REPRESENTATION,
                original,
                replacement,
                "ctx",
                "ctx",
                "security-hardening route mutation",
                "authorization decision must remain stable",
                SafetyClass.SAFE_READ_ONLY,
                "s8-sec-" + Integer.toHexString((replacement + original).hashCode()));
    }
}
