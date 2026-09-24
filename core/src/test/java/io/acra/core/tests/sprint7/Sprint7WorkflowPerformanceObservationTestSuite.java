package io.acra.core.tests.sprint7;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.AuthorizationRuleEffect;
import io.acra.core.domain.workflow.WorkflowAuthorizationRequest;
import io.acra.core.domain.workflow.WorkflowAuthorizationResolution;
import io.acra.core.domain.workflow.WorkflowPolicySnapshot;
import io.acra.core.domain.workflow.WorkflowTransitionRule;
import io.acra.core.engine.S7WorkflowCoverageTracker;
import io.acra.core.engine.WorkflowAuthorizationResolver;
import io.acra.core.product.workflow.S7WorkflowWorkspace;
import io.acra.core.tests.TestSupport;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class Sprint7WorkflowPerformanceObservationTestSuite {
    private static final Instant NOW = Instant.parse("2026-09-24T07:15:00Z");

    private Sprint7WorkflowPerformanceObservationTestSuite() { }

    public static void main(String[] args) throws Exception {
        int assertions = 0;
        List<Row> rows = new ArrayList<>();
        for (int count : new int[]{100, 1000, 10000}) {
            Row row = runWorkload(count);
            rows.add(row);
            TestSupport.assertEquals(count, row.resolved(), "all workflow requests resolved");
            assertions++;
            TestSupport.assertEquals(count, row.coverageContexts(), "all workflow contexts represented in coverage");
            assertions++;
            TestSupport.assertEquals(count, row.reportResolutions(), "report retains each unique workflow resolution");
            assertions++;
            TestSupport.assertTrue(row.resolveMs() >= 0 && row.coverageMs() >= 0 && row.reportMs() >= 0,
                    "workflow performance observations are non-negative");
            assertions++;
            System.out.println("SPRINT7_PERF count=" + count
                    + " resolveMs=" + row.resolveMs()
                    + " coverageMs=" + row.coverageMs()
                    + " reportMs=" + row.reportMs()
                    + " approxMemoryDeltaBytes=" + row.approxMemoryDeltaBytes()
                    + " resolved=" + row.resolved()
                    + " coverageContexts=" + row.coverageContexts()
                    + " reportResolutions=" + row.reportResolutions());
        }

        String output = System.getenv("ACRA_S7_PERF_OUTPUT");
        if (output != null && !output.isBlank()) {
            Path path = Path.of(output);
            if (path.getParent() != null) Files.createDirectories(path.getParent());
            StringBuilder csv = new StringBuilder(
                    "count,resolveMs,coverageMs,reportMs,approxMemoryDeltaBytes,resolved,coverageContexts,reportResolutions\n");
            for (Row row : rows) {
                csv.append(row.count()).append(',')
                        .append(row.resolveMs()).append(',')
                        .append(row.coverageMs()).append(',')
                        .append(row.reportMs()).append(',')
                        .append(row.approxMemoryDeltaBytes()).append(',')
                        .append(row.resolved()).append(',')
                        .append(row.coverageContexts()).append(',')
                        .append(row.reportResolutions()).append('\n');
            }
            Files.writeString(path, csv.toString(), StandardCharsets.UTF_8);
            System.out.println("SPRINT7_PERF_ARTIFACT " + path);
        }
        System.out.println("SPRINT7_WORKFLOW_PERFORMANCE_OBSERVATION PASS assertions=" + assertions);
    }

    private static Row runWorkload(int count) {
        Runtime runtime = Runtime.getRuntime();
        System.gc();
        long before = used(runtime);

        WorkflowPolicySnapshot policy = policy();
        WorkflowAuthorizationResolver resolver = new WorkflowAuthorizationResolver();
        List<WorkflowAuthorizationResolution> resolutions = new ArrayList<>(count);

        long t0 = System.nanoTime();
        for (int index = 0; index < count; index++) {
            WorkflowAuthorizationRequest request = new WorkflowAuthorizationRequest(
                    "document-approval",
                    "author-a",
                    List.of("author"),
                    "tenant-a",
                    "document-" + index,
                    "SUBMIT",
                    "DRAFT",
                    "SUBMITTED",
                    false,
                    true,
                    "",
                    "",
                    AuthorizationDecision.UNKNOWN,
                    NOW);
            resolutions.add(resolver.resolve(policy, null, request));
        }
        long t1 = System.nanoTime();

        S7WorkflowCoverageTracker tracker = new S7WorkflowCoverageTracker();
        for (WorkflowAuthorizationResolution resolution : resolutions) {
            tracker.recordResolution(resolution);
        }
        long t2 = System.nanoTime();

        S7WorkflowWorkspace workspace = new S7WorkflowWorkspace();
        workspace.loadPolicy(policy);
        for (WorkflowAuthorizationResolution resolution : resolutions) {
            workspace.recordResolution(resolution);
        }
        workspace.replaceCoverage(tracker.matrix());
        var report = workspace.report(NOW);
        long t3 = System.nanoTime();

        long after = used(runtime);
        return new Row(
                count,
                ms(t1 - t0),
                ms(t2 - t1),
                ms(t3 - t2),
                Math.max(0L, after - before),
                resolutions.size(),
                tracker.matrix().size(),
                report.summary().resolutionCount());
    }

    private static WorkflowPolicySnapshot policy() {
        return WorkflowPolicySnapshot.create(
                "s7-perf-policy",
                "1",
                "bounded-performance-observation",
                List.of(new WorkflowTransitionRule(
                        "submit-allow",
                        "document-approval",
                        "DRAFT",
                        "SUBMITTED",
                        "SUBMIT",
                        "tenant-a",
                        List.of("author"),
                        false,
                        false,
                        false,
                        false,
                        "",
                        AuthorizationRuleEffect.ALLOW,
                        10,
                        "explicit-performance-fixture",
                        List.of("e-rule"))),
                List.of(),
                List.of("e-policy"),
                AuthorizationDecision.DENY,
                NOW);
    }

    private static long used(Runtime runtime) {
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private static long ms(long nanos) {
        return Math.max(0L, nanos / 1_000_000L);
    }

    private record Row(
            int count,
            long resolveMs,
            long coverageMs,
            long reportMs,
            long approxMemoryDeltaBytes,
            int resolved,
            int coverageContexts,
            int reportResolutions) { }
}
