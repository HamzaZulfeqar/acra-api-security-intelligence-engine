package io.acra.core.tests.sprint8;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.http.HttpMethod;
import io.acra.core.route.RouteBoundaryObservation;
import io.acra.core.route.RouteNormalizationDivergenceKind;
import io.acra.core.route.RouteObservationSource;
import io.acra.core.route.RouteProcessingStage;
import io.acra.core.route.RouteSecurityBoundaryAnalyzer;
import io.acra.core.route.RouteSecurityBoundaryState;
import io.acra.core.tests.TestSupport;
import java.util.List;

public final class Sprint8AuthorizationPathDifferentialTestSuite {
    private Sprint8AuthorizationPathDifferentialTestSuite() { }

    public static void main(String[] args) {
        int assertions = 0;
        RouteSecurityBoundaryAnalyzer analyzer = new RouteSecurityBoundaryAnalyzer();

        var stable = analyzer.analyze("S8-BOUNDARY-STABLE", List.of(
                obs("proxy", RouteProcessingStage.PROXY, "/api/v1/admin", HttpMethod.GET,
                        "api.example.test", "v1", AuthorizationDecision.DENY, AuthorizationDecision.DENY, "e-proxy"),
                obs("gateway", RouteProcessingStage.GATEWAY, "/api/v1/admin", HttpMethod.GET,
                        "api.example.test", "v1", AuthorizationDecision.DENY, AuthorizationDecision.DENY, "e-gateway")));
        TestSupport.assertEquals(RouteSecurityBoundaryState.STABLE, stable.state(),
                "identical adjacent routing and authorization observations remain stable");
        assertions++;
        TestSupport.assertEquals(0L, stable.authorizationBoundaryChangeCount(),
                "stable trace has no authorization boundary changes");
        assertions++;

        var routeOnly = analyzer.analyze("S8-BOUNDARY-ROUTE", List.of(
                obs("proxy", RouteProcessingStage.PROXY, "/api//v1/admin", HttpMethod.GET,
                        "api.example.test", "v1", AuthorizationDecision.DENY, AuthorizationDecision.DENY, "e-proxy"),
                obs("gateway", RouteProcessingStage.GATEWAY, "/api/v1/admin", HttpMethod.GET,
                        "api.example.test", "v1", AuthorizationDecision.DENY, AuthorizationDecision.DENY, "e-gateway")));
        TestSupport.assertEquals(RouteSecurityBoundaryState.ROUTING_DIVERGENCE, routeOnly.state(),
                "canonical-equivalent representation change remains routing-only when authorization is stable");
        assertions++;
        TestSupport.assertEquals(RouteNormalizationDivergenceKind.REPRESENTATION_CHANGE,
                routeOnly.transitions().getFirst().pathDivergence(),
                "path representation change remains explicit");
        assertions++;

        var methodChange = analyzer.analyze("S8-BOUNDARY-METHOD", List.of(
                obs("gateway", RouteProcessingStage.GATEWAY, "/api/v1/items/1", HttpMethod.GET,
                        "api.example.test", "v1", AuthorizationDecision.ALLOW, AuthorizationDecision.ALLOW, "e-gateway"),
                obs("framework", RouteProcessingStage.FRAMEWORK, "/api/v1/items/1", HttpMethod.POST,
                        "api.example.test", "v1", AuthorizationDecision.ALLOW, AuthorizationDecision.ALLOW, "e-framework")));
        TestSupport.assertEquals(RouteSecurityBoundaryState.ROUTING_DIVERGENCE, methodChange.state(),
                "HTTP method change is a routing-boundary divergence");
        assertions++;
        TestSupport.assertTrue(methodChange.transitions().getFirst().methodChanged(),
                "method-change flag retained");
        assertions++;

        var hostVersionChange = analyzer.analyze("S8-BOUNDARY-HOST-VERSION", List.of(
                obs("gateway", RouteProcessingStage.GATEWAY, "/users/1", HttpMethod.GET,
                        "v1.api.example.test", "v1", AuthorizationDecision.ALLOW, AuthorizationDecision.ALLOW, "e-gateway"),
                obs("framework", RouteProcessingStage.FRAMEWORK, "/users/1", HttpMethod.GET,
                        "api.example.test", "v2", AuthorizationDecision.ALLOW, AuthorizationDecision.ALLOW, "e-framework")));
        TestSupport.assertEquals(RouteSecurityBoundaryState.ROUTING_DIVERGENCE, hostVersionChange.state(),
                "host/version changes remain routing-boundary divergence");
        assertions++;
        TestSupport.assertTrue(hostVersionChange.transitions().getFirst().hostChanged(),
                "host-change flag retained");
        assertions++;
        TestSupport.assertTrue(hostVersionChange.transitions().getFirst().apiVersionChanged(),
                "API-version-change flag retained");
        assertions++;

        var authorizationOnly = analyzer.analyze("S8-BOUNDARY-AUTH", List.of(
                obs("framework", RouteProcessingStage.FRAMEWORK, "/api/v1/admin", HttpMethod.GET,
                        "api.example.test", "v1", AuthorizationDecision.DENY, AuthorizationDecision.DENY, "e-framework"),
                obs("application", RouteProcessingStage.APPLICATION, "/api/v1/admin", HttpMethod.GET,
                        "api.example.test", "v1", AuthorizationDecision.ALLOW, AuthorizationDecision.ALLOW, "e-app")));
        TestSupport.assertEquals(RouteSecurityBoundaryState.AUTHORIZATION_BOUNDARY_CHANGE, authorizationOnly.state(),
                "authorization decision change can be represented independently of route change");
        assertions++;
        TestSupport.assertEquals(1L, authorizationOnly.authorizationBoundaryChangeCount(),
                "authorization change is counted exactly once");
        assertions++;

        var combined = analyzer.analyze("S8-BOUNDARY-COMBINED", List.of(
                obs("gateway", RouteProcessingStage.GATEWAY, "/api/a%2Fb/admin", HttpMethod.GET,
                        "api.example.test", "v1", AuthorizationDecision.DENY, AuthorizationDecision.DENY, "e-gateway"),
                obs("framework", RouteProcessingStage.FRAMEWORK, "/api/a/b/admin", HttpMethod.GET,
                        "api.example.test", "v1", AuthorizationDecision.ALLOW, AuthorizationDecision.ALLOW, "e-framework")));
        TestSupport.assertEquals(RouteSecurityBoundaryState.COMBINED_DIVERGENCE, combined.state(),
                "route structure plus authorization change remains a combined differential");
        assertions++;
        TestSupport.assertTrue(combined.transitions().getFirst().routingChanged(),
                "combined differential retains routing change");
        assertions++;
        TestSupport.assertTrue(combined.transitions().getFirst().authorizationChanged(),
                "combined differential retains authorization change");
        assertions++;

        var missingStage = analyzer.analyze("S8-BOUNDARY-GAP", List.of(
                obs("raw", RouteProcessingStage.RAW_URI, "/api/v1/admin", HttpMethod.GET,
                        "api.example.test", "v1", AuthorizationDecision.DENY, AuthorizationDecision.DENY, "e-raw"),
                obs("application", RouteProcessingStage.APPLICATION, "/api/v1/admin", HttpMethod.GET,
                        "api.example.test", "v1", AuthorizationDecision.ALLOW, AuthorizationDecision.ALLOW, "e-app")));
        TestSupport.assertEquals(RouteSecurityBoundaryState.INCONCLUSIVE, missingStage.state(),
                "stage gaps prevent causal boundary attribution");
        assertions++;

        var unknownAuthorization = analyzer.analyze("S8-BOUNDARY-UNKNOWN-AUTH", List.of(
                obs("proxy", RouteProcessingStage.PROXY, "/api/v1/admin", HttpMethod.GET,
                        "api.example.test", "v1", AuthorizationDecision.DENY, AuthorizationDecision.UNKNOWN, "e-proxy"),
                obs("gateway", RouteProcessingStage.GATEWAY, "/api/v1/admin", HttpMethod.GET,
                        "api.example.test", "v1", AuthorizationDecision.DENY, AuthorizationDecision.DENY, "e-gateway")));
        TestSupport.assertEquals(RouteSecurityBoundaryState.INCONCLUSIVE, unknownAuthorization.state(),
                "unknown authorization evidence cannot be promoted to stable or changed boundary");
        assertions++;
        TestSupport.assertContains(String.join(",", unknownAuthorization.transitions().getFirst().reasons()),
                "authorization-context-incomplete", "incomplete authorization reason retained");
        assertions++;

        expectFailure(() -> new RouteBoundaryObservation(
                "bad-host", RouteProcessingStage.PROXY, "/a", HttpMethod.GET, "https://api.example.test/x", "v1",
                AuthorizationDecision.DENY, AuthorizationDecision.DENY, "policy", RouteObservationSource.OBSERVED,
                List.of("e")), "host with scheme/path must fail closed");
        assertions++;

        expectFailure(() -> new RouteBoundaryObservation(
                "query-leak", RouteProcessingStage.PROXY, "/a?token=secret", HttpMethod.GET, "api.example.test", "v1",
                AuthorizationDecision.DENY, AuthorizationDecision.DENY, "policy", RouteObservationSource.OBSERVED,
                List.of("e")), "path/query conflation must fail closed");
        assertions++;

        System.out.println("SPRINT8_AUTHORIZATION_PATH_DIFFERENTIAL PASS assertions=" + assertions);
    }

    private static RouteBoundaryObservation obs(
            String id,
            RouteProcessingStage stage,
            String path,
            HttpMethod method,
            String host,
            String version,
            AuthorizationDecision expected,
            AuthorizationDecision observed,
            String evidence) {
        return new RouteBoundaryObservation(
                "S8-BOUNDARY-OBS-" + id,
                stage,
                path,
                method,
                host,
                version,
                expected,
                observed,
                "policy-" + id,
                RouteObservationSource.OBSERVED,
                List.of(evidence));
    }

    private static void expectFailure(Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }
}
