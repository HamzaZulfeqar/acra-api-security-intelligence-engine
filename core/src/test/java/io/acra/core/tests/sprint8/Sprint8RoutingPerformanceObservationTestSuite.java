package io.acra.core.tests.sprint8;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.http.HttpMethod;
import io.acra.core.product.routing.S8RoutingWorkspace;
import io.acra.core.route.RouteBoundaryObservation;
import io.acra.core.route.RouteNormalizationAnalyzer;
import io.acra.core.route.RouteObservationSource;
import io.acra.core.route.RouteProcessingStage;
import io.acra.core.route.RouteSecurityBoundaryAnalyzer;
import io.acra.core.route.RouteStageObservation;
import io.acra.core.tests.TestSupport;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class Sprint8RoutingPerformanceObservationTestSuite {
    private static final Instant NOW = Instant.parse("2026-09-24T08:10:00Z");

    private Sprint8RoutingPerformanceObservationTestSuite() { }

    public static void main(String[] args) throws Exception {
        int assertions = 0;
        List<String> csv = new ArrayList<>();
        csv.add("contexts,analysis_ms,report_ms,approx_memory_delta_bytes");

        for (int count : List.of(100, 1000, 10000)) {
            long beforeMemory = usedMemory();
            long start = System.nanoTime();
            S8RoutingWorkspace workspace = populate(count);
            long afterAnalysis = System.nanoTime();
            var report = workspace.report(NOW);
            long afterReport = System.nanoTime();
            long afterMemory = usedMemory();

            long analysisMs = nanosToMillis(afterAnalysis - start);
            long reportMs = nanosToMillis(afterReport - afterAnalysis);
            long memoryDelta = Math.max(0L, afterMemory - beforeMemory);

            TestSupport.assertEquals(count, workspace.snapshot().normalizationTraces().size(),
                    "performance fixture normalization trace count");
            assertions++;
            TestSupport.assertEquals(count, workspace.snapshot().boundaryTraces().size(),
                    "performance fixture boundary trace count");
            assertions++;
            TestSupport.assertEquals(count, report.summary().normalizationTraceCount(),
                    "performance report normalization count");
            assertions++;
            TestSupport.assertEquals(count, report.summary().boundaryTraceCount(),
                    "performance report boundary count");
            assertions++;

            csv.add(count + "," + analysisMs + "," + reportMs + "," + memoryDelta);
            System.out.println("SPRINT8_PERF count=" + count
                    + " analysisMs=" + analysisMs
                    + " reportMs=" + reportMs
                    + " approxMemoryDeltaBytes=" + memoryDelta
                    + " traces=" + workspace.snapshot().normalizationTraces().size()
                    + " boundaries=" + workspace.snapshot().boundaryTraces().size());
        }

        Path out = Path.of("build", "s8-foundation", "performance", "performance-s8.csv");
        Files.createDirectories(out.getParent());
        Files.writeString(out, String.join("\n", csv) + "\n", StandardCharsets.UTF_8);
        TestSupport.assertTrue(Files.isRegularFile(out), "performance CSV artifact written");
        assertions++;
        System.out.println("SPRINT8_PERF_ARTIFACT " + out.toAbsolutePath());
        System.out.println("SPRINT8_ROUTING_PERFORMANCE_OBSERVATION PASS assertions=" + assertions);
    }

    private static S8RoutingWorkspace populate(int count) {
        S8RoutingWorkspace workspace = new S8RoutingWorkspace();
        RouteNormalizationAnalyzer normalization = new RouteNormalizationAnalyzer();
        RouteSecurityBoundaryAnalyzer boundary = new RouteSecurityBoundaryAnalyzer();

        for (int i = 0; i < count; i++) {
            String canonical = "/api/v1/items/" + i;
            String alternate = "/api//v1/items/" + i;
            workspace.recordNormalizationTrace(normalization.analyze(
                    "S8-PERF-N-" + i,
                    List.of(
                            new RouteStageObservation(
                                    "S8-PERF-RAW-" + i,
                                    RouteProcessingStage.RAW_URI,
                                    alternate,
                                    RouteObservationSource.OBSERVED,
                                    List.of("e-raw-" + i)),
                            new RouteStageObservation(
                                    "S8-PERF-PROXY-" + i,
                                    RouteProcessingStage.PROXY,
                                    canonical,
                                    RouteObservationSource.OBSERVED,
                                    List.of("e-proxy-" + i)))));

            workspace.recordBoundaryTrace(boundary.analyze(
                    "S8-PERF-B-" + i,
                    List.of(
                            new RouteBoundaryObservation(
                                    "S8-PERF-FW-" + i,
                                    RouteProcessingStage.FRAMEWORK,
                                    canonical,
                                    HttpMethod.GET,
                                    "localhost",
                                    "v1",
                                    AuthorizationDecision.DENY,
                                    AuthorizationDecision.DENY,
                                    "s8-perf-policy",
                                    RouteObservationSource.CONFIGURED,
                                    List.of("e-fw-" + i)),
                            new RouteBoundaryObservation(
                                    "S8-PERF-APP-" + i,
                                    RouteProcessingStage.APPLICATION,
                                    alternate,
                                    HttpMethod.GET,
                                    "localhost",
                                    "v1",
                                    AuthorizationDecision.DENY,
                                    AuthorizationDecision.DENY,
                                    "s8-perf-policy",
                                    RouteObservationSource.OBSERVED,
                                    List.of("e-app-" + i)))));
        }
        return workspace;
    }

    private static long nanosToMillis(long nanos) {
        return Math.max(0L, nanos / 1_000_000L);
    }

    private static long usedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
}
