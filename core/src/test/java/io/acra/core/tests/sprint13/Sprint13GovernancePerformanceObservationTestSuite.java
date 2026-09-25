package io.acra.core.tests.sprint13;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.AuthorizationImpactProfile;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.engine.AuthorizationSeverityEvaluator;
import io.acra.core.product.finding.FindingGovernanceQueue;
import io.acra.core.product.finding.FindingGovernanceWorkspace;
import io.acra.core.tests.TestSupport;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class Sprint13GovernancePerformanceObservationTestSuite {
    private static final Instant NOW = Instant.parse("2026-09-25T01:05:00Z");

    private Sprint13GovernancePerformanceObservationTestSuite() { }

    public static void main(String[] args) throws Exception {
        int assertions = 0;
        List<String> csv = new ArrayList<>();
        csv.add("findings,population_ms,snapshot_ms,report_ms,approx_memory_delta_bytes");

        for (int count : List.of(100, 1000, 10000)) {
            long beforeMemory = usedMemory();
            long start = System.nanoTime();
            FindingGovernanceWorkspace workspace = populate(count);
            long afterPopulation = System.nanoTime();
            var snapshot = workspace.snapshot();
            long afterSnapshot = System.nanoTime();
            var report = workspace.report(NOW);
            long afterReport = System.nanoTime();
            long afterMemory = usedMemory();

            long populationMs = nanosToMillis(afterPopulation - start);
            long snapshotMs = nanosToMillis(afterSnapshot - afterPopulation);
            long reportMs = nanosToMillis(afterReport - afterSnapshot);
            long memoryDelta = Math.max(0L, afterMemory - beforeMemory);

            TestSupport.assertEquals(count, snapshot.findingCount(),
                    "performance fixture finding denominator");
            assertions++;
            TestSupport.assertEquals((long) count,
                    snapshot.queueCount(FindingGovernanceQueue.REVIEW_REQUIRED),
                    "performance fixture keeps all findings review-required");
            assertions++;
            TestSupport.assertEquals(0L, snapshot.confirmedHistoryCount(),
                    "performance fixture contains no confirmation history");
            assertions++;
            TestSupport.assertEquals(count, report.summary().findingCount(),
                    "performance report finding denominator");
            assertions++;
            TestSupport.assertEquals((long) count, report.summary().reviewRequiredCount(),
                    "performance report review-required denominator");
            assertions++;
            TestSupport.assertEquals(0, report.summary().lifecycleEventCount(),
                    "performance fixture emits no lifecycle events");
            assertions++;

            csv.add(count + "," + populationMs + "," + snapshotMs + "," + reportMs + "," + memoryDelta);
            System.out.println("SPRINT13_PERF findings=" + count
                    + " populationMs=" + populationMs
                    + " snapshotMs=" + snapshotMs
                    + " reportMs=" + reportMs
                    + " approxMemoryDeltaBytes=" + memoryDelta);
        }

        Path out = Path.of("build", "s13-foundation", "performance", "performance-s13.csv");
        Files.createDirectories(out.getParent());
        Files.writeString(out, String.join("\n", csv) + "\n", StandardCharsets.UTF_8);
        TestSupport.assertTrue(Files.isRegularFile(out),
                "Sprint 13 performance CSV artifact written");
        assertions++;

        System.out.println("SPRINT13_PERF_ARTIFACT " + out.toAbsolutePath());
        System.out.println("SPRINT13_GOVERNANCE_PERFORMANCE_OBSERVATION PASS assertions=" + assertions);
    }

    private static FindingGovernanceWorkspace populate(int count) {
        FindingGovernanceWorkspace workspace = new FindingGovernanceWorkspace();
        AuthorizationSeverityEvaluator evaluator = new AuthorizationSeverityEvaluator();
        for (int index = 0; index < count; index++) {
            FindingCandidate candidate = candidate(index);
            workspace.open(candidate, evaluator.evaluate(candidate, AuthorizationImpactProfile.none()));
        }
        return workspace;
    }

    private static FindingCandidate candidate(int index) {
        String id = "s13-perf-candidate-" + index;
        String resource = "document:" + index;
        return new FindingCandidate(
                id,
                FindingCandidateState.CANDIDATE,
                "acra-s13-performance",
                List.of("test-" + index),
                List.of("execution-" + index),
                List.of("observation-" + index),
                List.of("assessment-" + index),
                List.of("OBJECT_AUTHORIZATION"),
                "/api/v1/documents/{id}",
                resource,
                "principal-" + (index % 20),
                "TENANT_RELATION-" + (index % 10),
                AuthorizationDecision.DENY,
                AuthorizationDecision.ALLOW,
                List.of("evidence-" + index),
                List.of(),
                List.of("policy-" + index),
                "MEDIUM",
                "bounded Sprint 13 performance fixture",
                FindingFingerprint.of(
                        "/api/v1/documents/{id}",
                        resource,
                        "principal-" + (index % 20),
                        "TENANT_RELATION-" + (index % 10),
                        "DENY_TO_ALLOW",
                        "OBJECT_AUTHORIZATION"));
    }

    private static long nanosToMillis(long nanos) {
        return Math.max(0L, nanos / 1_000_000L);
    }

    private static long usedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
}
