package io.acra.core.tests.sprint10;

import io.acra.core.batch.BatchItemPolicy;
import io.acra.core.coverage.S10AuthorizationCoverageTracker;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.product.batchindirect.S10BatchIndirectWorkspace;
import io.acra.core.reference.IndirectReferencePolicy;
import io.acra.core.tests.TestSupport;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class Sprint10BatchIndirectPerformanceObservationTestSuite {
    private static final Instant NOW = Instant.parse("2026-09-25T00:20:00Z");

    private Sprint10BatchIndirectPerformanceObservationTestSuite() { }

    public static void main(String[] args) throws Exception {
        int assertions = 0;
        List<String> csv = new ArrayList<>();
        csv.add("contexts,population_ms,report_ms,approx_memory_delta_bytes");

        for (int count : List.of(100, 1000, 10000)) {
            long beforeMemory = usedMemory();
            long start = System.nanoTime();
            S10BatchIndirectWorkspace workspace = populate(count);
            long afterPopulation = System.nanoTime();
            var report = workspace.report(NOW);
            long afterReport = System.nanoTime();
            long afterMemory = usedMemory();

            long populationMs = nanosToMillis(afterPopulation - start);
            long reportMs = nanosToMillis(afterReport - afterPopulation);
            long memoryDelta = Math.max(0L, afterMemory - beforeMemory);

            TestSupport.assertEquals(count, workspace.snapshot().policyCount(),
                    "performance fixture policy count");
            assertions++;
            TestSupport.assertEquals(count, workspace.snapshot().coverageEntries().size(),
                    "performance fixture coverage count");
            assertions++;
            TestSupport.assertEquals(count, report.summary().policyContextCount(),
                    "performance report policy-context count");
            assertions++;
            TestSupport.assertEquals(count / 2, report.summary().batchPolicyContextCount(),
                    "performance report batch-context count");
            assertions++;
            TestSupport.assertEquals(count / 2, report.summary().indirectPolicyContextCount(),
                    "performance report indirect-context count");
            assertions++;
            TestSupport.assertEquals(count, report.summary().unobservedContextCount(),
                    "performance fixture intentionally preserves unobserved contexts");
            assertions++;
            TestSupport.assertEquals(0, report.summary().confirmedFindingCount(),
                    "performance-scale report cannot auto-confirm findings");
            assertions++;

            csv.add(count + "," + populationMs + "," + reportMs + "," + memoryDelta);
            System.out.println("SPRINT10_PERF count=" + count
                    + " populationMs=" + populationMs
                    + " reportMs=" + reportMs
                    + " approxMemoryDeltaBytes=" + memoryDelta
                    + " policies=" + workspace.snapshot().policyCount()
                    + " coverage=" + workspace.snapshot().coverageEntries().size());
        }

        Path out = Path.of("build", "s10-foundation", "performance", "performance-s10.csv");
        Files.createDirectories(out.getParent());
        Files.writeString(out, String.join("\n", csv) + "\n", StandardCharsets.UTF_8);
        TestSupport.assertTrue(Files.isRegularFile(out), "Sprint 10 performance CSV artifact written");
        assertions++;

        System.out.println("SPRINT10_PERF_ARTIFACT " + out.toAbsolutePath());
        System.out.println("SPRINT10_BATCH_INDIRECT_PERFORMANCE_OBSERVATION PASS assertions=" + assertions);
    }

    private static S10BatchIndirectWorkspace populate(int count) {
        S10BatchIndirectWorkspace workspace = new S10BatchIndirectWorkspace();
        S10AuthorizationCoverageTracker coverage = new S10AuthorizationCoverageTracker();

        for (int i = 0; i < count; i++) {
            String reference = "s10-perf-policy-" + i;
            String resource = "resource-" + i;
            String tenant = "tenant-" + (i % 20);

            if (i % 2 == 0) {
                BatchItemPolicy policy = new BatchItemPolicy(
                        reference,
                        "s10-performance-fixture",
                        "/api/v1/s10/batch/" + i,
                        resource,
                        "READ",
                        "viewer",
                        tenant,
                        AuthorizationDecision.DENY,
                        List.of("s10-perf-evidence-" + i));
                workspace.recordPolicy(policy);
                coverage.recordPolicy(policy);
            } else {
                IndirectReferencePolicy policy = new IndirectReferencePolicy(
                        reference,
                        "s10-performance-fixture",
                        "/api/v1/s10/share/{alias}/" + i,
                        resource,
                        "READ",
                        "viewer",
                        tenant,
                        AuthorizationDecision.DENY,
                        List.of("s10-perf-evidence-" + i));
                workspace.recordPolicy(policy);
                coverage.recordPolicy(policy);
            }
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
