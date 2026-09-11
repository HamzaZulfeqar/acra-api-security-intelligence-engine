package io.acra.core.tests.sprint4;

import io.acra.core.active.analysis.AuthorizationOutcome;
import io.acra.core.active.analysis.AuthorizationOutcomeNormalizer;
import io.acra.core.active.analysis.DifferentialClassification;
import io.acra.core.active.evidence.EvidenceStage;
import io.acra.core.active.evidence.ExecutionEvidenceStore;
import io.acra.core.active.evidence.RequestSnapshot;
import io.acra.core.active.evidence.ResponseSnapshot;
import io.acra.core.active.evidence.SafetyAuditLog;
import io.acra.core.active.execution.BackoffPolicy;
import io.acra.core.active.execution.BuiltRequest;
import io.acra.core.active.execution.CancellationToken;
import io.acra.core.active.execution.DelayController;
import io.acra.core.active.execution.ExecutionErrorType;
import io.acra.core.active.execution.LocalhostHttpTransport;
import io.acra.core.active.execution.RequestVariantKind;
import io.acra.core.active.execution.TestExecutor;
import io.acra.core.active.graph.GraphHydrationStatus;
import io.acra.core.active.model.ExecutionEnvironment;
import io.acra.core.active.model.TargetDescriptor;
import io.acra.core.active.safety.ActiveConsent;
import io.acra.core.active.safety.BudgetKey;
import io.acra.core.active.safety.BudgetScope;
import io.acra.core.active.safety.ConcurrencyKey;
import io.acra.core.active.safety.ConcurrencyScope;
import io.acra.core.active.safety.HierarchicalBudgetManager;
import io.acra.core.active.safety.HierarchicalConcurrencyController;
import io.acra.core.active.safety.KillSwitch;
import io.acra.core.active.safety.MutationBudgetTracker;
import io.acra.core.active.safety.MutationValidator;
import io.acra.core.active.safety.RateLimitPolicy;
import io.acra.core.active.safety.ScopedRateLimiter;
import io.acra.core.analysis.PassiveDifferentialComparator;
import io.acra.core.analysis.ResponseComparisonMode;
import io.acra.core.analysis.semantic.ResponseSemanticAnalyzer;
import io.acra.core.domain.http.HttpHeader;
import io.acra.core.domain.http.HttpMethod;
import io.acra.core.graph.RelationType;
import io.acra.core.graph.SecurityContextGraph;
import io.acra.core.serialization.DomainSerializer;
import io.acra.core.tests.TestSupport;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Sprint4LocalhostIntegrationTestSuite {
    private static int tests;
    private static String secureExecutionId;

    private record Harness(TestExecutor executor, ExecutionEvidenceStore evidence, SafetyAuditLog audit,
                           SecurityContextGraph graph) {}

    private Sprint4LocalhostIntegrationTestSuite() {}

    public static void main(String[] args) throws Exception {
        testLocalhostBoundaryAndConcreteRoundTrip();
        testTimeoutAndOperationalFailureMapping();
        testSecureAllowDenyControlledExperiment();
        testVulnerableSyntheticMismatchObservation();
        testLiveFalsePositivePreparationCases();
        System.out.println("PASS Sprint4 localhost integration tests=" + tests);
    }

    private static void testLocalhostBoundaryAndConcreteRoundTrip() throws Exception {
        TargetDescriptor secure = Sprint4LocalhostFixtures.target(true);
        var transport = new LocalhostHttpTransport(secure);
        var echoRequest = Sprint4LocalhostFixtures.simpleRequest(Sprint4LocalhostFixtures.SECURE_PORT, HttpMethod.POST,
                "/api/v1/s4/echo", Sprint4LocalhostFixtures.USER_A_TOKEN,
                List.of(new HttpHeader("Content-Type", "text/plain"), new HttpHeader("X-Echo", "trace-local-001")),
                "hello-localhost".getBytes(StandardCharsets.UTF_8));
        var echoSnapshot = RequestSnapshot.capture("S4-DIRECT-ECHO",
                new BuiltRequest(RequestVariantKind.BASELINE, "DIRECT-ECHO", echoRequest, "ctx-user-a", "echo"), Instant.now());
        var echo = transport.send(echoSnapshot, Duration.ofSeconds(2), new CancellationToken());
        TestSupport.assertEquals(200, echo.response().status(), "localhost POST echo status");
        TestSupport.assertContains(echo.response().bodyUtf8(), "\"method\":\"POST\"", "HTTP method preserved");
        TestSupport.assertContains(echo.response().bodyUtf8(), "hello-localhost", "request body preserved");
        TestSupport.assertContains(echo.response().bodyUtf8(), "trace-local-001", "relevant custom header preserved");
        TestSupport.assertTrue(echo.response().firstHeader("X-Request-ID").isPresent(), "response headers captured");
        TestSupport.assertTrue(!echo.responseTiming().isNegative(), "response timing captured");

        TargetDescriptor external = new TargetDescriptor("acra-s4", "external", "http", "example.com", 80,
                ExecutionEnvironment.LAB, true, List.of("/api/v1"), Set.of(HttpMethod.GET));
        TestSupport.assertThrows(IllegalArgumentException.class, () -> new LocalhostHttpTransport(external),
                "external LAB target rejected by concrete transport");

        TargetDescriptor nonLab = new TargetDescriptor("acra-s4", "non-lab", "http", "localhost",
                Sprint4LocalhostFixtures.SECURE_PORT, ExecutionEnvironment.AUTHORIZED_DEV, true,
                List.of("/api/v1"), Set.of(HttpMethod.GET));
        TestSupport.assertThrows(IllegalArgumentException.class, () -> new LocalhostHttpTransport(nonLab),
                "localhost transport rejects non-LAB environment");

        var externalRequest = io.acra.core.domain.http.HttpRequest.of(HttpMethod.GET, "http", "example.com", 80,
                "/api/v1/test", List.of(), new byte[0], io.acra.core.domain.http.HttpProtocol.HTTP_1_1);
        var externalSnapshot = RequestSnapshot.capture("S4-EXTERNAL-REQ",
                new BuiltRequest(RequestVariantKind.BASELINE, "EXTERNAL", externalRequest, "ctx", "resource"), Instant.now());
        assertThrowsChecked(IllegalArgumentException.class,
                () -> transport.send(externalSnapshot, Duration.ofSeconds(1), new CancellationToken()),
                "request authority cannot escape configured localhost target");
        System.out.println("S4_TRANSPORT roundTrip=PASS methodBodyHeader=PASS externalRejected=PASS responseTiming=PASS");
        tests += 9;
    }

    private static void testTimeoutAndOperationalFailureMapping() throws Exception {
        var transport = new LocalhostHttpTransport(Sprint4LocalhostFixtures.target(true));
        var slowRequest = Sprint4LocalhostFixtures.simpleRequest(Sprint4LocalhostFixtures.SECURE_PORT, HttpMethod.GET,
                "/api/v1/s4/slow?ms=250", Sprint4LocalhostFixtures.USER_A_TOKEN, List.of(), new byte[0]);
        var snapshot = RequestSnapshot.capture("S4-SLOW-DIRECT",
                new BuiltRequest(RequestVariantKind.BASELINE, "SLOW", slowRequest, "ctx", "slow"), Instant.now());
        assertThrowsChecked(HttpTimeoutException.class,
                () -> transport.send(snapshot, Duration.ofMillis(30), new CancellationToken()),
                "concrete transport timeout surfaces as HttpTimeoutException");

        var timeoutTest = Sprint4LocalhostFixtures.timeoutTest();
        Harness harness = harness(timeoutTest, Duration.ofMillis(30));
        var result = harness.executor().execute(timeoutTest, consent(), List.of());
        TestSupport.assertEquals(io.acra.core.domain.testing.TestState.FAILED, result.state(), "timeout execution is operational failure");
        TestSupport.assertEquals(ExecutionErrorType.TIMEOUT, result.failure().type(), "executor maps localhost timeout to TIMEOUT");
        TestSupport.assertTrue(result.observation() == null, "timeout does not create security observation");
        TestSupport.assertTrue(result.evidenceChain().stream().noneMatch(entry -> entry.stage() == EvidenceStage.OBSERVATION),
                "timeout evidence contains no fabricated observation");
        System.out.println("S4_TIMEOUT errorType=" + result.failure().type() + " observationCreated=false");
        tests += 5;
    }

    private static void testSecureAllowDenyControlledExperiment() {
        var test = Sprint4LocalhostFixtures.resourceExperiment(true);
        TestSupport.assertEquals("/api/v1/s4/documents/Document-B", test.negativeControl().request().rawTarget(),
                "secure expected-DENY control is User-A to Document-B");
        TestSupport.assertEquals(test.baselineDefinition().request().firstHeader("Authorization").orElseThrow(),
                test.negativeControl().request().firstHeader("Authorization").orElseThrow(),
                "secure expected-DENY control preserves User-A authentication context");
        Harness harness = harness(test, Duration.ofSeconds(2));
        var result = harness.executor().execute(test, consent(), List.of(Sprint4LocalhostFixtures.groundTruthDeny()));
        TestSupport.assertEquals(io.acra.core.domain.testing.TestState.COMPLETED, result.state(), "secure localhost experiment completes");
        TestSupport.assertEquals(AuthorizationOutcome.ALLOW, result.observation().differences().baselineOutcome(), "baseline User-A Document-A allows");
        TestSupport.assertEquals(AuthorizationOutcome.ALLOW, result.observation().differences().positiveControlOutcome(), "positive control allows");
        TestSupport.assertEquals(AuthorizationOutcome.DENY, result.observation().differences().negativeControlOutcome(), "independent deny control denies");
        TestSupport.assertEquals(AuthorizationOutcome.DENY, result.observation().observedDecision(), "User-A Document-B mutation denied");
        TestSupport.assertEquals(DifferentialClassification.EXPECTED_CHANGE,
                result.observation().differences().classification(), "secure controlled difference is expected change");
        TestSupport.assertEquals(io.acra.core.active.analysis.ExpectedDecisionSource.ACRA_LAB_GROUND_TRUTH,
                result.observation().expectedDecision().source(), "ground truth outranks ACRA test prediction");
        TestSupport.assertEquals(11, result.evidenceChain().size(), "complete test/control/mutation/differential/observation evidence chain");
        TestSupport.assertEquals("<redacted>", result.observation().baseline().cookies().get("s4session"),
                "live response cookie redacted in observation evidence");

        var hydration = harness.executor().graphHydration(result.executionId()).orElseThrow();
        TestSupport.assertEquals(GraphHydrationStatus.HYDRATED, hydration.status(),
                "live observation hydrates existing SecurityContextGraph through executor path");
        TestSupport.assertTrue(harness.graph().node("principal:User-A").isPresent(), "live graph contains principal");
        TestSupport.assertTrue(harness.graph().node("resource:document:Document-B").isPresent(),
                "live graph contains controlled mutation resource");
        var access = harness.graph().outgoing("principal:User-A", RelationType.ACCESSES).stream()
                .filter(edge -> edge.target().equals("resource:document:Document-B")).findFirst().orElseThrow();
        var graphEvidence = access.evidenceIds().stream().map(id -> harness.graph().evidence(id).orElseThrow()).toList();
        TestSupport.assertTrue(graphEvidence.stream().anyMatch(value -> value.location().equals("s4/MUTATION_RESPONSE")),
                "live graph edge traces to mutation response");
        TestSupport.assertTrue(graphEvidence.stream().anyMatch(value -> value.location().equals("s4/OBSERVATION")),
                "live graph edge traces to observation");
        TestSupport.assertTrue(graphEvidence.stream().allMatch(value -> value.requestId().equals(result.executionId())),
                "live graph edge traces to execution id");
        TestSupport.assertTrue(graphEvidence.stream().allMatch(value -> value.extractedValue()
                        .contains("testId=" + result.testId()) && value.extractedValue()
                        .contains("observationId=" + result.observation().observationId())),
                "live graph edge traces to test and observation ids");

        var mutationEvidence = graphEvidence.stream()
                .filter(value -> value.location().equals("s4/MUTATION_RESPONSE"))
                .findFirst().orElseThrow();
        String serializedGraph = new DomainSerializer().serialize(harness.graph());
        String serializedObservation = new DomainSerializer().serialize(result.observation());
        TestSupport.assertNotContains(serializedGraph, "synthetic-cookie-secret",
                "live session secret never enters serialized graph state");
        TestSupport.assertNotContains(serializedObservation, "synthetic-cookie-secret",
                "live session secret never enters observation");

        String rawToken = Sprint4LocalhostFixtures.USER_A_TOKEN;
        for (var entry : result.evidenceChain()) {
            Object object = harness.evidence().object(entry.objectId());
            String serialized = new DomainSerializer().serialize(object);
            TestSupport.assertNotContains(serialized, rawToken, "persistable evidence serialization contains no raw synthetic credential");
        }
        TestSupport.assertTrue(harness.audit().entries().stream()
                        .map(event -> new DomainSerializer().serialize(event))
                        .noneMatch(serialized -> serialized.contains(rawToken)),
                "safety audit serialization contains no raw synthetic credential");
        secureExecutionId = result.executionId();
        System.out.println("S4_SECURE executionId=" + result.executionId()
                + " requestFingerprint=" + result.observation().executionFingerprint().requestFingerprint()
                + " responseFingerprint=" + result.observation().executionFingerprint().responseFingerprint()
                + " expected=" + result.observation().expectedDecision().decision()
                + " observed=" + result.observation().observedDecision()
                + " differential=" + result.observation().differences().classification()
                + " evidenceEntries=" + result.evidenceChain().size());
        System.out.println("S4_GRAPH executionId=" + result.executionId()
                + " testId=" + result.testId()
                + " observationId=" + result.observation().observationId()
                + " chainEvidenceId=" + mutationEvidence.extractedValue().split("chainEvidenceId=", 2)[1].split(";", 2)[0]
                + " graphEvidenceId=" + mutationEvidence.evidenceId()
                + " relationshipId=" + access.edgeId());
        tests += 22 + result.evidenceChain().size();
    }

    private static void testVulnerableSyntheticMismatchObservation() {
        var test = Sprint4LocalhostFixtures.resourceExperiment(false);
        Harness harness = harness(test, Duration.ofSeconds(2));
        var result = harness.executor().execute(test, consent(), List.of(Sprint4LocalhostFixtures.groundTruthDeny()));
        TestSupport.assertEquals(io.acra.core.domain.testing.TestState.COMPLETED, result.state(), "vulnerable localhost experiment completes");
        TestSupport.assertEquals(AuthorizationOutcome.ALLOW, result.observation().differences().baselineOutcome(), "vulnerable baseline control allows");
        TestSupport.assertEquals(AuthorizationOutcome.DENY, result.observation().differences().negativeControlOutcome(), "vulnerable fixture keeps deny control intact");
        TestSupport.assertEquals(AuthorizationOutcome.ALLOW, result.observation().observedDecision(), "intentional vulnerable mutation returns allow");
        TestSupport.assertEquals(DifferentialClassification.UNEXPECTED_CHANGE,
                result.observation().differences().classification(), "expected deny versus observed allow retained as unexpected change");
        TestSupport.assertEquals(io.acra.core.domain.authorization.AuthorizationDecision.DENY,
                result.observation().expectedDecision().decision(), "independent policy remains DENY");
        TestSupport.assertTrue(harness.evidence().observations().size() == 1, "mismatch retained as observation only");
        TestSupport.assertFalse(result.executionId().equals(secureExecutionId), "separate executors receive distinct process-wide execution IDs");
        System.out.println("S4_VULNERABLE executionId=" + result.executionId()
                + " requestFingerprint=" + result.observation().executionFingerprint().requestFingerprint()
                + " responseFingerprint=" + result.observation().executionFingerprint().responseFingerprint()
                + " expected=" + result.observation().expectedDecision().decision()
                + " observed=" + result.observation().observedDecision()
                + " differential=" + result.observation().differences().classification()
                + " evidenceEntries=" + result.evidenceChain().size());
        tests += 7;
    }

    private static void testLiveFalsePositivePreparationCases() throws Exception {
        var target = Sprint4LocalhostFixtures.target(true);
        var transport = new LocalhostHttpTransport(target);
        var semanticAnalyzer = new ResponseSemanticAnalyzer();
        var normalizer = new AuthorizationOutcomeNormalizer();
        var comparator = new PassiveDifferentialComparator();

        var soft = send(transport, "FP-SOFT", Sprint4LocalhostFixtures.simpleRequest(Sprint4LocalhostFixtures.SECURE_PORT,
                HttpMethod.GET, "/api/v1/s4/application-denial", Sprint4LocalhostFixtures.USER_A_TOKEN, List.of(), new byte[0]));
        TestSupport.assertEquals(200, soft.response().status(), "soft-deny fixture deliberately uses HTTP 200");
        TestSupport.assertEquals(AuthorizationOutcome.DENY,
                normalizer.normalize(soft.response(), semanticAnalyzer.fingerprint(soft.response())),
                "HTTP 200 application denial is semantically DENY");

        var dynamicA = send(transport, "FP-DYN-A", Sprint4LocalhostFixtures.request(Sprint4LocalhostFixtures.SECURE_PORT,
                Sprint4LocalhostFixtures.DOCUMENT_A, Sprint4LocalhostFixtures.USER_A_TOKEN, "User-A"));
        Thread.sleep(2L);
        var dynamicB = send(transport, "FP-DYN-B", Sprint4LocalhostFixtures.request(Sprint4LocalhostFixtures.SECURE_PORT,
                Sprint4LocalhostFixtures.DOCUMENT_A, Sprint4LocalhostFixtures.USER_A_TOKEN, "User-A"));
        TestSupport.assertFalse(dynamicA.response().bodyUtf8().equals(dynamicB.response().bodyUtf8()),
                "dynamic timestamp/request id produce raw response variance");
        TestSupport.assertTrue(comparator.compare(dynamicA.response(), dynamicB.response(), ResponseComparisonMode.SEMANTIC).equivalent(),
                "dynamic timestamp/request-id variance is semantically equivalent");

        var orderA = send(transport, "FP-ORDER-A", Sprint4LocalhostFixtures.simpleRequest(Sprint4LocalhostFixtures.SECURE_PORT,
                HttpMethod.GET, "/api/v1/s4/public?variant=a", Sprint4LocalhostFixtures.USER_A_TOKEN, List.of(), new byte[0]));
        var orderB = send(transport, "FP-ORDER-B", Sprint4LocalhostFixtures.simpleRequest(Sprint4LocalhostFixtures.SECURE_PORT,
                HttpMethod.GET, "/api/v1/s4/public?variant=b", Sprint4LocalhostFixtures.USER_A_TOKEN, List.of(), new byte[0]));
        TestSupport.assertFalse(orderA.response().bodyUtf8().equals(orderB.response().bodyUtf8()), "format/order representations differ in raw body");
        TestSupport.assertTrue(comparator.compare(orderA.response(), orderB.response(), ResponseComparisonMode.SEMANTIC).equivalent(),
                "format/order variation remains semantically equivalent");
        System.out.println("S4_FP http200ApplicationDenial=DENY dynamicEquivalent=true representationEquivalent=true");
        tests += 6;
    }

    private static io.acra.core.active.execution.TransportResult send(LocalhostHttpTransport transport, String id,
                                                                       io.acra.core.domain.http.HttpRequest request) throws Exception {
        RequestSnapshot snapshot = RequestSnapshot.capture(id,
                new BuiltRequest(RequestVariantKind.BASELINE, id, request, "ctx-user-a", "s4-fixture"), Instant.now());
        return transport.send(snapshot, Duration.ofSeconds(2), new CancellationToken());
    }

    private static Harness harness(io.acra.core.active.model.SecurityTest test, Duration timeout) {
        Clock clock = Clock.systemUTC();
        SafetyAuditLog audit = new SafetyAuditLog(clock);
        KillSwitch kill = new KillSwitch(audit);
        kill.reset(true, "authorized Sprint 4 localhost laboratory execution");
        HierarchicalBudgetManager budgets = new HierarchicalBudgetManager(audit);
        for (BudgetKey key : budgetKeys(test)) budgets.configure(key, 50);
        HierarchicalConcurrencyController concurrency = new HierarchicalConcurrencyController();
        for (ConcurrencyKey key : concurrencyKeys(test)) concurrency.configure(key, 2);
        ScopedRateLimiter rate = new ScopedRateLimiter();
        RateLimitPolicy policy = new RateLimitPolicy(1000, 5000, 1000, 0);
        for (String key : rateKeys(test)) rate.configure(key, policy);
        MutationBudgetTracker mutationBudget = new MutationBudgetTracker(10);
        MutationValidator validator = new MutationValidator(Sprint4LocalhostFixtures.PROJECT,
                Set.of(ExecutionEnvironment.LAB), kill, budgets, concurrency, rate, audit);
        ExecutionEvidenceStore evidence = new ExecutionEvidenceStore(clock);
        SecurityContextGraph graph = new SecurityContextGraph();
        DelayController noDelay = duration -> { };
        TestExecutor executor = new TestExecutor(clock, timeout, new LocalhostHttpTransport(test.target()), noDelay,
                validator, budgets, concurrency, rate, new BackoffPolicy(0, Duration.ofMillis(25)), kill,
                mutationBudget, audit, evidence, graph, test.target().projectId());
        return new Harness(executor, evidence, audit, graph);
    }

    private static ActiveConsent consent() {
        return new ActiveConsent(true, true, true, true, false);
    }

    private static List<BudgetKey> budgetKeys(io.acra.core.active.model.SecurityTest test) {
        return List.of(
                new BudgetKey(BudgetScope.GLOBAL, "global"),
                new BudgetKey(BudgetScope.PROJECT, test.target().projectId()),
                new BudgetKey(BudgetScope.TARGET, test.target().targetId()),
                new BudgetKey(BudgetScope.ENDPOINT, test.endpoint().endpointId()),
                new BudgetKey(BudgetScope.TEST, test.testId()),
                new BudgetKey(BudgetScope.CONTEXT, test.targetContext().principal() + '|' + test.targetContext().tenant() + '|' + test.targetContext().role()));
    }

    private static List<ConcurrencyKey> concurrencyKeys(io.acra.core.active.model.SecurityTest test) {
        return List.of(
                new ConcurrencyKey(ConcurrencyScope.GLOBAL, "global"),
                new ConcurrencyKey(ConcurrencyScope.HOST, test.target().host()),
                new ConcurrencyKey(ConcurrencyScope.TARGET, test.target().targetId()),
                new ConcurrencyKey(ConcurrencyScope.ENDPOINT, test.endpoint().endpointId()));
    }

    private static List<String> rateKeys(io.acra.core.active.model.SecurityTest test) {
        return List.of("host:" + test.target().host(), "target:" + test.target().targetId(),
                "endpoint:" + test.endpoint().endpointId(), "test:" + test.testId());
    }

    @FunctionalInterface
    private interface ThrowingRunnable { void run() throws Exception; }

    private static <T extends Throwable> void assertThrowsChecked(Class<T> type, ThrowingRunnable runnable, String message) {
        try {
            runnable.run();
        } catch (Throwable thrown) {
            if (type.isInstance(thrown)) return;
            throw new AssertionError(message + " wrong exception " + thrown, thrown);
        }
        throw new AssertionError(message + " expected exception " + type.getSimpleName());
    }
}
