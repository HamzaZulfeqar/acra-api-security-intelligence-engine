package io.acra.core.tests.sprint8;

import io.acra.core.route.RouteNormalizationAnalyzer;
import io.acra.core.route.RouteNormalizationDivergenceKind;
import io.acra.core.route.RouteNormalizationTraceState;
import io.acra.core.route.RouteObservationSource;
import io.acra.core.route.RouteProcessingStage;
import io.acra.core.route.RouteStageObservation;
import io.acra.core.tests.TestSupport;
import java.util.List;

public final class Sprint8RoutingNormalizationFoundationTestSuite {
    private Sprint8RoutingNormalizationFoundationTestSuite() { }

    public static void main(String[] args) {
        int assertions = 0;
        RouteNormalizationAnalyzer analyzer = new RouteNormalizationAnalyzer();

        var complete = analyzer.analyze("S8-TRACE-COMPLETE", List.of(
                obs("raw", RouteProcessingStage.RAW_URI, "/api/v1/admin", "e-raw"),
                obs("proxy", RouteProcessingStage.PROXY, "/api/v1/admin", "e-proxy"),
                obs("gateway", RouteProcessingStage.GATEWAY, "/api/v1/admin", "e-gateway"),
                obs("framework", RouteProcessingStage.FRAMEWORK, "/api/v1/admin", "e-framework"),
                obs("application", RouteProcessingStage.APPLICATION, "/api/v1/admin", "e-application")));
        TestSupport.assertEquals(RouteNormalizationTraceState.COMPLETE, complete.state(),
                "five-stage evidence produces a complete routing trace");
        assertions++;
        TestSupport.assertEquals(4, complete.transitions().size(),
                "complete trace contains four stage transitions");
        assertions++;
        TestSupport.assertTrue(complete.transitions().stream()
                        .allMatch(value -> value.divergence() == RouteNormalizationDivergenceKind.NONE),
                "identical observed paths do not invent normalization divergence");
        assertions++;
        TestSupport.assertEquals(5, complete.evidenceIds().size(),
                "trace retains stage evidence provenance");
        assertions++;

        var representation = analyzer.analyze("S8-TRACE-REPRESENTATION", List.of(
                obs("raw", RouteProcessingStage.RAW_URI, "/api//v1/admin", "e-raw"),
                obs("proxy", RouteProcessingStage.PROXY, "/api/v1/admin", "e-proxy")));
        TestSupport.assertEquals(RouteNormalizationDivergenceKind.REPRESENTATION_CHANGE,
                representation.transitions().getFirst().divergence(),
                "duplicate-separator collapse is represented as canonical-equivalent representation change");
        assertions++;

        var encodedSlash = analyzer.analyze("S8-TRACE-ENCODED-SLASH", List.of(
                obs("raw", RouteProcessingStage.RAW_URI, "/api/a%2Fb/items/1", "e-raw"),
                obs("proxy", RouteProcessingStage.PROXY, "/api/a/b/items/1", "e-proxy")));
        TestSupport.assertEquals(RouteNormalizationDivergenceKind.CANONICAL_DIVERGENCE,
                encodedSlash.transitions().getFirst().divergence(),
                "encoded slash decoding can produce an evidence-backed canonical route divergence");
        assertions++;
        TestSupport.assertEquals(1L, encodedSlash.canonicalDivergenceCount(),
                "canonical divergence count remains explicit");
        assertions++;

        var family = analyzer.analyze("S8-TRACE-FAMILY", List.of(
                obs("proxy", RouteProcessingStage.PROXY, "/docs/{invoice_id}", "e-proxy"),
                obs("gateway", RouteProcessingStage.GATEWAY, "/docs/{order_id}", "e-gateway")));
        TestSupport.assertEquals(RouteNormalizationDivergenceKind.FAMILY_VARIATION,
                family.transitions().getFirst().divergence(),
                "same static route family remains distinct from exact canonical equivalence");
        assertions++;

        var partial = analyzer.analyze("S8-TRACE-PARTIAL", List.of(
                obs("raw", RouteProcessingStage.RAW_URI, "/api/v1/admin", "e-raw"),
                obs("app", RouteProcessingStage.APPLICATION, "/api/v1/admin", "e-app")));
        TestSupport.assertEquals(RouteNormalizationTraceState.PARTIAL, partial.state(),
                "missing intermediary observations produce a partial trace");
        assertions++;
        TestSupport.assertEquals(3, partial.missingStages().size(),
                "proxy/gateway/framework gaps remain visible");
        assertions++;
        TestSupport.assertEquals(RouteNormalizationDivergenceKind.INCONCLUSIVE,
                partial.transitions().getFirst().divergence(),
                "stage gaps prevent attribution even when endpoint representations match");
        assertions++;
        TestSupport.assertContains(String.join(",", partial.transitions().getFirst().reasons()),
                "missing-intermediate-stage", "stage-gap reason retained");
        assertions++;

        var oneStage = analyzer.analyze("S8-TRACE-ONE", List.of(
                obs("raw", RouteProcessingStage.RAW_URI, "/api/v1/admin", "e-raw")));
        TestSupport.assertEquals(RouteNormalizationTraceState.INCONCLUSIVE, oneStage.state(),
                "one observed stage cannot establish a normalization transition");
        assertions++;

        expectFailure(() -> analyzer.analyze("S8-DUP", List.of(
                obs("raw-1", RouteProcessingStage.RAW_URI, "/a", "e1"),
                obs("raw-2", RouteProcessingStage.RAW_URI, "/b", "e2"))),
                "duplicate processing stage must fail closed");
        assertions++;

        expectFailure(() -> new RouteStageObservation(
                "query-leak", RouteProcessingStage.RAW_URI, "/a?token=secret",
                RouteObservationSource.OBSERVED, List.of("e")),
                "stage observation must remain path-only");
        assertions++;

        expectFailure(() -> new RouteStageObservation(
                "no-provenance", RouteProcessingStage.PROXY, "/a",
                RouteObservationSource.OBSERVED, List.of()),
                "stage observation without provenance evidence must fail");
        assertions++;

        System.out.println("SPRINT8_ROUTING_NORMALIZATION_FOUNDATION PASS assertions=" + assertions);
    }

    private static RouteStageObservation obs(
            String id,
            RouteProcessingStage stage,
            String path,
            String evidence) {
        return new RouteStageObservation(
                "S8-OBS-" + id,
                stage,
                path,
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
