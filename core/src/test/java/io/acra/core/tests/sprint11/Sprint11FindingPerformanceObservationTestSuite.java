package io.acra.core.tests.sprint11;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.AuthorizationRiskAssessment;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingConfidence;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.domain.finding.FindingSeverity;
import io.acra.core.product.finding.FindingReviewWorkspace;
import io.acra.core.reporting.finding.FindingReproductionJsonExporter;
import io.acra.core.reporting.finding.FindingReproductionPackageGenerator;
import io.acra.core.reporting.finding.FindingReproductionSarifExporter;
import io.acra.core.tests.TestSupport;
import java.time.Instant;
import java.util.List;

public final class Sprint11FindingPerformanceObservationTestSuite {
    private static final int CASES = 1_000;
    private static final int EXPORT_ITERATIONS = 2_000;
    private static final Instant AT = Instant.parse("2026-09-25T05:00:00Z");

    private Sprint11FindingPerformanceObservationTestSuite() { }

    public static void main(String[] args) {
        Observation observation = run();
        System.out.println(
                "SPRINT11_FINDING_PERFORMANCE PASS"
                + " cases=" + CASES
                + " exportIterations=" + EXPORT_ITERATIONS
                + " intakeMillis=" + observation.intakeMillis()
                + " exportMillis=" + observation.exportMillis()
                + " jsonBytes=" + observation.jsonBytes()
                + " sarifBytes=" + observation.sarifBytes()
                + " scope=CONTROLLED_CI_ENGINEERING_OBSERVATION");
    }

    public static Observation run() {
        FindingReviewWorkspace workspace = new FindingReviewWorkspace("project-performance");

        long intakeStart = System.nanoTime();
        for (int index = 0; index < CASES; index++) {
            FindingCandidate candidate = candidate(index);
            workspace.open(candidate, risk(candidate.candidateId()), AT.plusSeconds(index));
        }
        long intakeMillis = nanosToMillis(System.nanoTime() - intakeStart);

        TestSupport.assertEquals(CASES, workspace.snapshot().totalCount(),
                "bounded performance fixture retains all findings");
        TestSupport.assertEquals((long) CASES, workspace.snapshot().openReviewCount(),
                "bounded performance fixture retains review-only state");

        var first = workspace.snapshot().cases().get(0);
        var reproduction = new FindingReproductionPackageGenerator().generate(first, AT.plusSeconds(10_000));
        FindingReproductionJsonExporter jsonExporter = new FindingReproductionJsonExporter();
        FindingReproductionSarifExporter sarifExporter = new FindingReproductionSarifExporter();

        String referenceJson = jsonExporter.json(reproduction).content();
        String referenceSarif = sarifExporter.sarif(reproduction).content();

        long exportStart = System.nanoTime();
        for (int index = 0; index < EXPORT_ITERATIONS; index++) {
            String json = jsonExporter.json(reproduction).content();
            String sarif = sarifExporter.sarif(reproduction).content();
            if (!referenceJson.equals(json) || !referenceSarif.equals(sarif)) {
                throw new AssertionError("repeated export became non-deterministic at iteration " + index);
            }
        }
        long exportMillis = nanosToMillis(System.nanoTime() - exportStart);

        TestSupport.assertTrue(referenceJson.length() < 20_000,
                "canonical JSON remains bounded for single finding reproduction");
        TestSupport.assertTrue(referenceSarif.length() < 40_000,
                "canonical SARIF remains bounded for single finding reproduction");

        // CI guardrail only. This is intentionally loose and is not a production SLO.
        TestSupport.assertTrue(intakeMillis < 15_000,
                "1,000-case in-memory intake stays inside loose CI guardrail");
        TestSupport.assertTrue(exportMillis < 20_000,
                "2,000 JSON+SARIF repetitions stay inside loose CI guardrail");

        return new Observation(
                intakeMillis,
                exportMillis,
                referenceJson.getBytes(java.nio.charset.StandardCharsets.UTF_8).length,
                referenceSarif.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
    }

    private static long nanosToMillis(long nanos) {
        return Math.max(0L, nanos / 1_000_000L);
    }

    private static FindingCandidate candidate(int index) {
        String suffix = String.format("%04d", index);
        String resource = "resource-perf-" + suffix;
        return new FindingCandidate(
                "candidate-perf-" + suffix,
                FindingCandidateState.CANDIDATE,
                "project-performance",
                List.of("test-perf-" + suffix),
                List.of("execution-perf-" + suffix),
                List.of("observation-perf-" + suffix),
                List.of("assessment-perf-" + suffix),
                List.of(index % 2 == 0 ? "BATCH_AUTHORIZATION" : "INDIRECT_REFERENCE_AUTHORIZATION"),
                "/api/v1/s11/performance",
                resource,
                "user-a",
                "CROSS_TENANT",
                AuthorizationDecision.DENY,
                AuthorizationDecision.ALLOW,
                List.of("evidence-perf-" + suffix),
                List.of(),
                List.of("policy-perf-" + suffix),
                "MEDIUM",
                "controlled performance fixture",
                FindingFingerprint.of(
                        "/api/v1/s11/performance",
                        resource,
                        "user-a",
                        "CROSS_TENANT",
                        index % 2 == 0 ? "BATCH_AUTHORIZATION" : "INDIRECT_REFERENCE_AUTHORIZATION",
                        "CANDIDATE"));
    }

    private static AuthorizationRiskAssessment risk(String candidateId) {
        return new AuthorizationRiskAssessment(
                "risk-" + candidateId,
                candidateId,
                FindingSeverity.MEDIUM,
                FindingConfidence.MEDIUM,
                60,
                "controlled performance fixture",
                List.of("authorization-impact"));
    }

    public record Observation(
            long intakeMillis,
            long exportMillis,
            int jsonBytes,
            int sarifBytes) { }
}
