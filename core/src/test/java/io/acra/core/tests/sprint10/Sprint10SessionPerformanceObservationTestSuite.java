package io.acra.core.tests.sprint10;

import io.acra.core.product.session.S10SessionWorkspace;
import io.acra.core.session.S10SessionCoverageTracker;
import io.acra.core.session.SessionCoverageObjective;
import io.acra.core.session.SessionCoverageTarget;
import io.acra.core.tests.TestSupport;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class Sprint10SessionPerformanceObservationTestSuite {
    private static final Instant NOW = Instant.parse("2026-09-24T18:45:00Z");

    private Sprint10SessionPerformanceObservationTestSuite() { }

    public static void main(String[] args) throws Exception {
        int assertions = 0;
        List<String> csv = new ArrayList<>();
        csv.add("contexts,population_ms,report_ms,approx_memory_delta_bytes");

        for (int count : List.of(100, 1000, 10000)) {
            long beforeMemory = usedMemory();
            long start = System.nanoTime();
            S10SessionWorkspace workspace = populate(count);
            long afterPopulation = System.nanoTime();
            var report = workspace.report(NOW);
            long afterReport = System.nanoTime();
            long afterMemory = usedMemory();

            long populationMs = nanosToMillis(afterPopulation - start);
            long reportMs = nanosToMillis(afterReport - afterPopulation);
            long memoryDelta = Math.max(0L, afterMemory - beforeMemory);

            TestSupport.assertEquals(count, workspace.snapshot().coverageEntries().size(),
                    "performance fixture coverage count");
            assertions++;
            TestSupport.assertEquals(count, report.summary().coverageTargetCount(),
                    "performance report coverage target count");
            assertions++;
            TestSupport.assertEquals(count, report.summary().unobservedTargetCount(),
                    "performance fixture preserves unobserved coverage");
            assertions++;
            TestSupport.assertEquals(0, report.summary().confirmedFindingCount(),
                    "performance-scale report cannot auto-confirm findings");
            assertions++;

            csv.add(count + "," + populationMs + "," + reportMs + "," + memoryDelta);
            System.out.println("SPRINT10_PERF count=" + count
                    + " populationMs=" + populationMs
                    + " reportMs=" + reportMs
                    + " approxMemoryDeltaBytes=" + memoryDelta
                    + " coverage=" + workspace.snapshot().coverageEntries().size());
        }

        Path out = Path.of("build", "s10-foundation", "performance", "performance-s10.csv");
        Files.createDirectories(out.getParent());
        Files.writeString(out, String.join("\n", csv) + "\n", StandardCharsets.UTF_8);
        TestSupport.assertTrue(Files.isRegularFile(out),
                "Sprint 10 performance CSV artifact written");
        assertions++;

        System.out.println("SPRINT10_PERF_ARTIFACT " + out.toAbsolutePath());
        System.out.println("SPRINT10_SESSION_PERFORMANCE_OBSERVATION PASS assertions=" + assertions);
    }

    private static S10SessionWorkspace populate(int count) {
        S10SessionWorkspace workspace = new S10SessionWorkspace();
        S10SessionCoverageTracker coverage = new S10SessionCoverageTracker();

        SessionCoverageObjective[] objectives = SessionCoverageObjective.values();
        for (int i = 0; i < count; i++) {
            SessionCoverageTarget target = SessionCoverageTarget.of(
                    "s10-perf-target-" + i,
                    "s10-perf-context-" + i,
                    objectives[i % objectives.length],
                    i % 2 == 0 ? "FR-033" : "SEC-003");
            coverage.recordTarget(target);
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
