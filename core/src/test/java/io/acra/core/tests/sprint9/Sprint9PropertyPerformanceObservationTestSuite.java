package io.acra.core.tests.sprint9;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.engine.PolicyValidationEvaluator;
import io.acra.core.product.property.S9PropertyWorkspace;
import io.acra.core.property.S9PropertyCoverageTracker;
import io.acra.core.tests.TestSupport;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class Sprint9PropertyPerformanceObservationTestSuite {
    private static final Instant NOW = Instant.parse("2026-09-24T13:15:00Z");

    private Sprint9PropertyPerformanceObservationTestSuite() { }

    public static void main(String[] args) throws Exception {
        int assertions = 0;
        List<String> csv = new ArrayList<>();
        csv.add("contexts,population_ms,report_ms,approx_memory_delta_bytes");

        for (int count : List.of(100, 1000, 10000)) {
            long beforeMemory = usedMemory();
            long start = System.nanoTime();
            S9PropertyWorkspace workspace = populate(count);
            long afterPopulation = System.nanoTime();
            var report = workspace.report(NOW);
            long afterReport = System.nanoTime();
            long afterMemory = usedMemory();

            long populationMs = nanosToMillis(afterPopulation - start);
            long reportMs = nanosToMillis(afterReport - afterPopulation);
            long memoryDelta = Math.max(0L, afterMemory - beforeMemory);

            TestSupport.assertEquals(count, workspace.snapshot().policies().size(),
                    "performance fixture policy count");
            assertions++;
            TestSupport.assertEquals(count, workspace.snapshot().coverageEntries().size(),
                    "performance fixture coverage count");
            assertions++;
            TestSupport.assertEquals(count, report.summary().policyContextCount(),
                    "performance report policy-context count");
            assertions++;
            TestSupport.assertEquals(count, report.summary().unobservedContextCount(),
                    "performance fixture intentionally preserves unobserved contexts");
            assertions++;
            TestSupport.assertEquals(0, report.summary().confirmedFindingCount(),
                    "performance-scale report cannot auto-confirm findings");
            assertions++;

            csv.add(count + "," + populationMs + "," + reportMs + "," + memoryDelta);
            System.out.println("SPRINT9_PERF count=" + count
                    + " populationMs=" + populationMs
                    + " reportMs=" + reportMs
                    + " approxMemoryDeltaBytes=" + memoryDelta
                    + " policies=" + workspace.snapshot().policies().size()
                    + " coverage=" + workspace.snapshot().coverageEntries().size());
        }

        Path out = Path.of("build", "s9-foundation", "performance", "performance-s9.csv");
        Files.createDirectories(out.getParent());
        Files.writeString(out, String.join("\n", csv) + "\n", StandardCharsets.UTF_8);
        TestSupport.assertTrue(Files.isRegularFile(out), "Sprint 9 performance CSV artifact written");
        assertions++;

        System.out.println("SPRINT9_PERF_ARTIFACT " + out.toAbsolutePath());
        System.out.println("SPRINT9_PROPERTY_PERFORMANCE_OBSERVATION PASS assertions=" + assertions);
    }

    private static S9PropertyWorkspace populate(int count) {
        S9PropertyWorkspace workspace = new S9PropertyWorkspace();
        S9PropertyCoverageTracker coverage = new S9PropertyCoverageTracker();

        for (int i = 0; i < count; i++) {
            PolicyValidationEvaluator.PropertyOperation operation =
                    i % 2 == 0
                            ? PolicyValidationEvaluator.PropertyOperation.READ
                            : PolicyValidationEvaluator.PropertyOperation.UPDATE;
            var policy = new PolicyValidationEvaluator.PropertyPolicy(
                    "s9-perf-policy-" + i,
                    "s9-performance-fixture",
                    "/api/v1/s9/resources/" + i,
                    "field_" + i,
                    operation,
                    "viewer",
                    "tenant-" + (i % 20),
                    AuthorizationDecision.DENY,
                    List.of("s9-perf-evidence-" + i));
            workspace.recordPolicy(policy);
            coverage.recordPolicy(policy);
        }
        workspace.replaceCoverage(coverage);
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
