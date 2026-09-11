package io.acra.core.tests.sprint4;

import io.acra.core.active.analysis.AuthorizationOutcome;
import io.acra.core.active.analysis.AuthorizationOutcomeNormalizer;
import io.acra.core.active.analysis.DifferentialClassification;
import io.acra.core.active.analysis.ExpectedDecisionResolution;
import io.acra.core.active.analysis.ExpectedDecisionSource;
import io.acra.core.active.analysis.MultiWayDifferentialAnalyzer;
import io.acra.core.active.evidence.EvidenceStage;
import io.acra.core.active.evidence.ExecutionEvidenceStore;
import io.acra.core.active.evidence.ResponseSnapshot;
import io.acra.core.active.execution.ExecutionQueue;
import io.acra.core.active.execution.RequestBuilder;
import io.acra.core.active.execution.TestPriority;
import io.acra.core.active.model.Mutation;
import io.acra.core.active.model.MutationLocation;
import io.acra.core.active.model.MutationType;
import io.acra.core.active.model.SafetyClass;
import io.acra.core.active.planning.TestPlanner;
import io.acra.core.analysis.semantic.ResponseSemanticAnalyzer;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.http.HttpHeader;
import io.acra.core.domain.http.HttpMethod;
import io.acra.core.domain.http.HttpProtocol;
import io.acra.core.domain.http.HttpRequest;
import io.acra.core.domain.testing.TestState;
import io.acra.core.planning.ReconTestPlanner;
import io.acra.core.recon.ApiReconnaissanceEngine;
import io.acra.core.engine.SecurityContextEngine;
import io.acra.core.tests.Fixtures;
import io.acra.core.tests.TestSupport;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

public final class Sprint4CoreVerificationTestSuite {
    private static int tests;

    private Sprint4CoreVerificationTestSuite() {}

    public static void main(String[] args) {
        testSecurityTestAndMutationContract();
        testRequestBuilderImmutabilityAndSingleDifference();
        testPlannerDeterminismDedupAndDryRun();
        testQueueOrderingTransitionsAndStopAll();
        testOutcomeNormalizationAndFourWayDifferential();
        testAppendOnlyEvidence();
        System.out.println("PASS Sprint4 core verification tests=" + tests);
    }

    private static void testSecurityTestAndMutationContract() {
        var test = Sprint4Fixtures.test("S4-CORE-001", "principal:A->B");
        TestSupport.assertEquals("S4-CORE-001", test.testId(), "test id");
        TestSupport.assertEquals("1", test.testVersion(), "test version");
        TestSupport.assertEquals(HttpMethod.GET, test.method(), "method");
        TestSupport.assertEquals(AuthorizationDecision.DENY, test.expectedDecision(), "independent expected decision");
        TestSupport.assertEquals("User-A", test.sourceContext().principal(), "source principal");
        TestSupport.assertEquals("User-B", test.targetContext().principal(), "target principal");
        TestSupport.assertEquals("1001", test.sourceResource().resourceId(), "source resource");
        TestSupport.assertEquals("1001", test.targetResource().resourceId(), "target resource constant");
        TestSupport.assertEquals(test.signature(), Sprint4Fixtures.test("S4-CORE-001", "principal:A->B").signature(), "deterministic signature");
        TestSupport.assertThrows(UnsupportedOperationException.class,
                () -> test.expectedEvidence().add("mutate"), "expected evidence immutable");
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> new Mutation("bad", MutationType.IDENTITY_SUBSTITUTION, MutationLocation.IDENTITY,
                        "same", "same", "a", "b", "invalid no-op", "none", SafetyClass.SAFE_READ_ONLY, "bad"),
                "mutation must change exactly one value");
        tests += 10;
    }

    private static void testRequestBuilderImmutabilityAndSingleDifference() {
        var test = Sprint4Fixtures.test("S4-CORE-002", "principal:A->B");
        var baselineBefore = test.baselineDefinition().request();
        String authBefore = baselineBefore.firstHeader("Authorization").orElseThrow();
        String principalBefore = baselineBefore.firstHeader("X-Principal").orElseThrow();
        var set = new RequestBuilder().build(test);
        var mutated = set.mutation().request();
        TestSupport.assertEquals("User-A", principalBefore, "baseline principal captured");
        TestSupport.assertEquals("User-A", baselineBefore.firstHeader("X-Principal").orElseThrow(), "baseline remains immutable");
        TestSupport.assertEquals("User-B", mutated.firstHeader("X-Principal").orElseThrow(), "single declared principal change applied");
        TestSupport.assertEquals(authBefore, mutated.firstHeader("Authorization").orElseThrow(), "unrelated authorization header preserved");
        TestSupport.assertEquals(baselineBefore.rawTarget(), mutated.rawTarget(), "resource path unchanged");
        TestSupport.assertEquals(baselineBefore.method(), mutated.method(), "method unchanged");
        TestSupport.assertEquals(baselineBefore.bodyUtf8(), mutated.bodyUtf8(), "body unchanged");
        TestSupport.assertEquals(test.positiveControl().request().rawTarget(), set.positiveControl().request().rawTarget(), "positive control preserved");
        TestSupport.assertEquals(test.negativeControl().request().firstHeader("X-Principal").orElseThrow(),
                set.negativeControl().request().firstHeader("X-Principal").orElseThrow(), "negative control preserved");

        HttpRequest ambiguous = HttpRequest.of(HttpMethod.GET, "http", "localhost", 8080, "/api/v1/documents/1001",
                List.of(new HttpHeader("X-Principal", "User-A"), new HttpHeader("X-Other", "User-A")),
                new byte[0], HttpProtocol.HTTP_1_1);
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> new RequestBuilder().mutate(ambiguous, Sprint4Fixtures.mutation("ambiguous")),
                "declared header mutation cannot silently affect multiple locations");
        tests += 10;
    }

    private static void testPlannerDeterminismDedupAndDryRun() {
        var input = Sprint4Fixtures.planningInput(List.of(
                Sprint4Fixtures.seed("S4-PLAN-001", "principal:A->B"),
                Sprint4Fixtures.seed("S4-PLAN-002", "principal:A->B")));
        var planner = new TestPlanner();
        var first = planner.plan(input);
        var second = planner.plan(input);
        TestSupport.assertEquals(first.fingerprint(), second.fingerprint(), "planner deterministic fingerprint");
        TestSupport.assertEquals(first.tests().stream().map(t -> t.signature()).toList(),
                second.tests().stream().map(t -> t.signature()).toList(), "planner deterministic ordering");
        TestSupport.assertEquals(1, first.tests().size(), "duplicate canonical test deduplicated");
        TestSupport.assertEquals(1, first.skipped().size(), "duplicate skip recorded");
        TestSupport.assertContains(first.skipped().getFirst().reason(), "duplicate", "dedup reason retained");
        TestSupport.assertEquals(4, first.estimatedRequests(), "request cost estimated without execution");

        var tx = Fixtures.tx("/api/v1/tenants/Tenant-A/documents/1001");
        var snapshot = new SecurityContextEngine().analyze(tx);
        var recon = new ApiReconnaissanceEngine().analyze(tx, snapshot, null,
                List.of("User-A", "User-B"), List.of("document:1001", "document:2001"));
        var dryRun = new ReconTestPlanner().plan("ACRA-Lab", snapshot, recon.reconnaissance(),
                List.of("User-A", "User-B"), List.of("document:1001", "document:2001"));
        TestSupport.assertEquals(0, dryRun.dispatchedRequests(), "dry run dispatch invariant");
        TestSupport.assertTrue(dryRun.estimatedRequests() >= 0, "dry run estimates requests");
        TestSupport.assertTrue(dryRun.safetyNotes().contains("NO_NETWORK_REQUESTS"), "dry run safety marker");
        tests += 9;
    }

    private static void testQueueOrderingTransitionsAndStopAll() {
        var audit = new io.acra.core.active.evidence.SafetyAuditLog(Clock.fixed(Sprint4Fixtures.NOW, ZoneOffset.UTC));
        var queue = new ExecutionQueue(audit);
        var low = Sprint4Fixtures.test("S4-Q-LOW", "low-key");
        var high = Sprint4Fixtures.test("S4-Q-HIGH", "high-key");
        var duplicate = Sprint4Fixtures.test("S4-Q-DUP", "high-key");
        TestPriority lowPriority = new TestPriority(10, 10, 10, 10, 10, 10, 10);
        TestPriority highPriority = new TestPriority(100, 100, 100, 100, 100, 100, 100);
        queue.enqueue(low, lowPriority);
        queue.enqueue(high, highPriority);
        var dup = queue.enqueue(duplicate, highPriority);
        TestSupport.assertEquals(TestState.SKIPPED, dup.state(), "duplicate signature skipped");
        TestSupport.assertEquals("S4-Q-HIGH", queue.startNext().orElseThrow().testId(), "priority order deterministic");
        TestSupport.assertEquals(TestState.RUNNING, queue.snapshot("S4-Q-HIGH").state(), "running transition");
        TestSupport.assertEquals(TestState.PAUSED, queue.pause("S4-Q-HIGH").state(), "pause transition");
        TestSupport.assertEquals(TestState.QUEUED, queue.resume("S4-Q-HIGH").state(), "resume transition");
        TestSupport.assertEquals("S4-Q-HIGH", queue.startNext().orElseThrow().testId(), "resumed high priority remains first");
        TestSupport.assertEquals(TestState.COMPLETED, queue.complete("S4-Q-HIGH").state(), "complete transition");
        TestSupport.assertEquals("S4-Q-LOW", queue.startNext().orElseThrow().testId(), "low test eventually starts");
        var cancelled = queue.stopAll("verification STOP ALL");
        TestSupport.assertEquals(1, cancelled.size(), "stop all cancels active work");
        TestSupport.assertEquals(TestState.CANCELLED, queue.snapshot("S4-Q-LOW").state(), "stop all terminal state");
        TestSupport.assertTrue(queue.startNext().isEmpty(), "queue empty after stop all");
        tests += 11;
    }

    private static void testOutcomeNormalizationAndFourWayDifferential() {
        var analyzer = new ResponseSemanticAnalyzer();
        var normalizer = new AuthorizationOutcomeNormalizer();
        var allow = Sprint4Fixtures.response(200, "{\"id\":1001,\"owner_id\":\"User-A\",\"tenant_id\":\"Tenant-A\"}");
        var softDeny = Sprint4Fixtures.response(200, "{\"success\":false,\"message\":\"Access denied\"}");
        TestSupport.assertEquals(AuthorizationOutcome.ALLOW, normalizer.normalize(allow, analyzer.fingerprint(allow)), "semantic allow");
        TestSupport.assertEquals(AuthorizationOutcome.DENY, normalizer.normalize(softDeny, analyzer.fingerprint(softDeny)), "HTTP 200 application denial normalized to deny");
        TestSupport.assertEquals(AuthorizationOutcome.AUTHENTICATION_REQUIRED,
                normalizer.normalize(Sprint4Fixtures.response(401, "{}"), analyzer.fingerprint(Sprint4Fixtures.response(401, "{}"))),
                "401 auth required");
        TestSupport.assertEquals(AuthorizationOutcome.NOT_FOUND,
                normalizer.normalize(Sprint4Fixtures.response(404, "{}"), analyzer.fingerprint(Sprint4Fixtures.response(404, "{}"))),
                "404 not found");

        var baseline = ResponseSnapshot.capture("R-B", "Q-B", allow, Map.of(), Duration.ofMillis(5), Sprint4Fixtures.NOW);
        var positive = ResponseSnapshot.capture("R-P", "Q-P", allow, Map.of(), Duration.ofMillis(5), Sprint4Fixtures.NOW);
        var negative = ResponseSnapshot.capture("R-N", "Q-N", softDeny, Map.of(), Duration.ofMillis(5), Sprint4Fixtures.NOW);
        var mutation = ResponseSnapshot.capture("R-M", "Q-M", softDeny, Map.of(), Duration.ofMillis(5), Sprint4Fixtures.NOW);
        var expected = new ExpectedDecisionResolution(AuthorizationDecision.DENY, ExpectedDecisionSource.ACRA_LAB_GROUND_TRUTH,
                "GT-S4-DENY", List.of("GT-S4-DENY"), 1.0, false);
        var differential = new MultiWayDifferentialAnalyzer().compare(baseline, positive, negative, mutation, expected);
        TestSupport.assertEquals(AuthorizationOutcome.ALLOW, differential.positiveControlOutcome(), "positive control established");
        TestSupport.assertTrue(differential.negativeControlOutcome().deniedEquivalent(), "negative control established");
        TestSupport.assertEquals(AuthorizationOutcome.DENY, differential.mutationOutcome(), "mutation outcome deny");
        TestSupport.assertEquals(DifferentialClassification.EXPECTED_CHANGE, differential.classification(), "expected semantic change classification");
        TestSupport.assertEquals(5, differential.differences().size(), "four-way comparison evidence retained");
        tests += 9;
    }

    private static void testAppendOnlyEvidence() {
        var store = new ExecutionEvidenceStore(Clock.fixed(Sprint4Fixtures.NOW, ZoneOffset.UTC));
        var test = Sprint4Fixtures.test("S4-EVID-001", "evidence-key");
        var first = store.append("EXEC-1", test.testId(), EvidenceStage.TEST, "OBJ-1", test);
        TestSupport.assertEquals("OBJ-1", first.objectId(), "evidence object id");
        TestSupport.assertEquals(1, store.chain("EXEC-1").size(), "evidence chain append");
        TestSupport.assertEquals(test, store.object("OBJ-1"), "stored object retrievable");
        TestSupport.assertThrows(IllegalStateException.class,
                () -> store.append("EXEC-1", test.testId(), EvidenceStage.TEST, "OBJ-1", test),
                "evidence object ids are immutable and append-only");
        TestSupport.assertThrows(IllegalArgumentException.class, () -> store.object("missing"), "missing evidence rejected");
        tests += 5;
    }
}
