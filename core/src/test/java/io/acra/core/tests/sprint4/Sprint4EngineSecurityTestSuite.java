package io.acra.core.tests.sprint4;

import io.acra.core.active.analysis.DifferentialClassification;
import io.acra.core.active.evidence.ExecutionEvidenceStore;
import io.acra.core.active.evidence.SafetyAuditLog;
import io.acra.core.active.execution.BackoffPolicy;
import io.acra.core.active.execution.DelayController;
import io.acra.core.active.execution.ExecutionQueue;
import io.acra.core.active.execution.ExecutionErrorType;
import io.acra.core.active.execution.HttpTransport;
import io.acra.core.active.execution.RequestBuilder;
import io.acra.core.active.execution.TestExecutor;
import io.acra.core.active.execution.TestPriority;
import io.acra.core.active.execution.TransportResult;
import io.acra.core.active.model.ConfigurationSnapshot;
import io.acra.core.active.model.ExecutionEnvironment;
import io.acra.core.active.model.Mutation;
import io.acra.core.active.model.MutationLocation;
import io.acra.core.active.model.MutationType;
import io.acra.core.active.model.SafetyClass;
import io.acra.core.active.model.TargetDescriptor;
import io.acra.core.active.replay.ReplayDescriptor;
import io.acra.core.active.replay.ReplayService;
import io.acra.core.active.safety.ActiveConsent;
import io.acra.core.active.safety.BudgetKey;
import io.acra.core.active.safety.BudgetScope;
import io.acra.core.active.safety.ConcurrencyKey;
import io.acra.core.active.safety.ConcurrencyScope;
import io.acra.core.active.safety.EnvironmentGuard;
import io.acra.core.active.safety.HardScopeGuard;
import io.acra.core.active.safety.HierarchicalBudgetManager;
import io.acra.core.active.safety.HierarchicalConcurrencyController;
import io.acra.core.active.safety.KillSwitch;
import io.acra.core.active.safety.MutationBudgetTracker;
import io.acra.core.active.safety.MutationValidator;
import io.acra.core.active.safety.RateLimitPolicy;
import io.acra.core.active.safety.ScopedRateLimiter;
import io.acra.core.active.safety.ValidationStatus;
import io.acra.core.domain.http.HttpHeader;
import io.acra.core.domain.http.HttpMethod;
import io.acra.core.domain.http.HttpProtocol;
import io.acra.core.domain.http.HttpRequest;
import io.acra.core.domain.testing.TestState;
import io.acra.core.serialization.DomainSerializer;
import io.acra.core.tests.TestSupport;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public final class Sprint4EngineSecurityTestSuite {
    private static int tests;

    private Sprint4EngineSecurityTestSuite() {}

    public static void main(String[] args) {
        testScopeEnvironmentConsentAndKillSwitch();
        testBudgetConcurrencyAndRateSafety();
        testMutationCorruptionAndConfigurationTampering();
        testQueueDuplicateAndStopAllControls();
        testCredentialAndAuditRedaction();
        testMutationDispatchSafety();
        testExecutorObservationEvidenceAndReplayIdentity();
        System.out.println("PASS Sprint4 engine security tests=" + tests);
    }

    private static void testScopeEnvironmentConsentAndKillSwitch() {
        var test = Sprint4Fixtures.test("S4-SEC-001", "scope-key");
        var scope = new HardScopeGuard();
        TestSupport.assertEquals(ValidationStatus.ALLOWED,
                scope.evaluate(Sprint4Fixtures.PROJECT, test, test.baselineDefinition().request()).status(), "in-scope request allowed");
        TestSupport.assertEquals(ValidationStatus.BLOCKED,
                scope.evaluate("other-project", test, test.baselineDefinition().request()).status(), "cross-project request blocked");
        HttpRequest escaped = HttpRequest.of(HttpMethod.GET, "http", "localhost", 8080, "/api/v1/%2e%2e/admin",
                List.of(), new byte[0], HttpProtocol.HTTP_1_1);
        TestSupport.assertEquals(ValidationStatus.BLOCKED, scope.evaluate(Sprint4Fixtures.PROJECT, test, escaped).status(), "encoded traversal blocked");

        var env = new EnvironmentGuard();
        TargetDescriptor remoteLab = new TargetDescriptor(Sprint4Fixtures.PROJECT, "remote-lab", "http", "example.test", 8080,
                ExecutionEnvironment.LAB, true, List.of("/api"), Set.of(HttpMethod.GET));
        TestSupport.assertEquals(ValidationStatus.BLOCKED,
                env.evaluate(remoteLab, Set.of(ExecutionEnvironment.LAB)).status(), "LAB environment requires loopback");

        var audit = fixedAudit();
        var kill = new KillSwitch(audit);
        TestSupport.assertTrue(kill.engaged(), "kill switch defaults engaged");
        TestSupport.assertThrows(IllegalArgumentException.class, () -> kill.reset(false, "unauthorized reset"), "kill reset requires explicit authorization");
        kill.reset(true, "authorized test reset");
        TestSupport.assertFalse(kill.engaged(), "authorized reset opens execution gate");
        kill.engage("STOP ALL");
        TestSupport.assertTrue(kill.engaged(), "STOP ALL re-engages global kill switch");
        tests += 8;
    }

    private static void testBudgetConcurrencyAndRateSafety() {
        var audit = fixedAudit();
        var budgets = new HierarchicalBudgetManager(audit);
        BudgetKey key = new BudgetKey(BudgetScope.GLOBAL, "global");
        budgets.configure(key, 2);
        TestSupport.assertTrue(budgets.canReserve(List.of(key), 2), "budget can reserve exact remaining amount");
        try (var reservation = budgets.reserve(List.of(key), 1, "E", "T")) {
            TestSupport.assertEquals(1, budgets.snapshot(key).reserved(), "reservation accounted");
            reservation.commit();
        }
        TestSupport.assertEquals(1, budgets.snapshot(key).executed(), "committed budget accounted");
        TestSupport.assertEquals(1, budgets.snapshot(key).remaining(), "budget cannot underflow");
        TestSupport.assertThrows(io.acra.core.active.safety.BudgetExceededException.class,
                () -> budgets.reserve(List.of(key), 2, "E2", "T2"), "budget overflow rejected");

        var concurrency = new HierarchicalConcurrencyController();
        ConcurrencyKey ckey = new ConcurrencyKey(ConcurrencyScope.GLOBAL, "global");
        concurrency.configure(ckey, 1);
        try (var lease = concurrency.acquire(List.of(ckey))) {
            TestSupport.assertTrue(lease != null, "concurrency lease acquired");
            TestSupport.assertEquals(1, concurrency.running(ckey), "concurrency lease counted");
            TestSupport.assertFalse(concurrency.canAcquire(List.of(ckey)), "concurrency limit enforced");
        }
        TestSupport.assertEquals(0, concurrency.running(ckey), "concurrency lease releases exactly once");

        var rate = new ScopedRateLimiter();
        rate.configure("test", new RateLimitPolicy(1, 10, 1, 0));
        Instant now = Sprint4Fixtures.NOW;
        TestSupport.assertTrue(rate.tryAcquire(List.of("test"), now).allowed(), "first rate-limited request allowed");
        TestSupport.assertFalse(rate.tryAcquire(List.of("test"), now).allowed(), "second same-window request rate-limited");
        tests += 10;
    }

    private static void testMutationCorruptionAndConfigurationTampering() {
        HttpRequest ambiguous = HttpRequest.of(HttpMethod.GET, "http", "localhost", 8080, "/api/v1/documents/1001",
                List.of(new HttpHeader("X-A", "same"), new HttpHeader("X-B", "same")),
                new byte[0], HttpProtocol.HTTP_1_1);
        Mutation mutation = new Mutation("M-CORRUPT", MutationType.IDENTITY_SUBSTITUTION, MutationLocation.IDENTITY,
                "same", "other", "a", "b", "must target exactly one location", "none",
                SafetyClass.SAFE_READ_ONLY, "corrupt-key");
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> new RequestBuilder().mutate(ambiguous, mutation), "multi-match mutation corruption rejected");

        Mutation uriEscape = new Mutation("M-URI", MutationType.RESOURCE_SUBSTITUTION, MutationLocation.PATH,
                "1001", "http://outside.test", "a", "b", "authority escape must fail", "none",
                SafetyClass.SAFE_READ_ONLY, "uri-escape");
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> new RequestBuilder().mutate(Sprint4Fixtures.baseline().request(), uriEscape), "URI authority escape rejected");

        var good = Sprint4Fixtures.configuration();
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> new ConfigurationSnapshot(good.values(), "tampered-fingerprint"), "configuration tampering rejected");
        tests += 3;
    }

    private static void testQueueDuplicateAndStopAllControls() {
        var queue = new ExecutionQueue(fixedAudit());
        TestPriority p = new TestPriority(50, 50, 50, 50, 50, 50, 50);
        var first = Sprint4Fixtures.test("S4-SEC-Q1", "dup-key");
        var second = Sprint4Fixtures.test("S4-SEC-Q2", "dup-key");
        queue.enqueue(first, p);
        var duplicate = queue.enqueue(second, p);
        TestSupport.assertEquals(TestState.SKIPPED, duplicate.state(), "duplicate execution controlled by canonical signature");
        queue.startNext().orElseThrow();
        var stopped = queue.stopAll("security verification STOP ALL");
        TestSupport.assertEquals(1, stopped.size(), "STOP ALL cancels one running execution");
        TestSupport.assertEquals(TestState.CANCELLED, queue.snapshot(first.testId()).state(), "running work cancelled");
        TestSupport.assertTrue(queue.startNext().isEmpty(), "no duplicate or cancelled work remains dispatchable");
        tests += 4;
    }

    private static void testCredentialAndAuditRedaction() {
        var test = Sprint4Fixtures.test("S4-SEC-REDACT", "redact-key");
        String serialized = new DomainSerializer().serialize(test);
        TestSupport.assertNotContains(serialized, "synthetic-user-a-token", "serialized evidence must not leak bearer token");
        TestSupport.assertNotContains(serialized, "synthetic-user-b-token", "serialized controls must not leak bearer token");
        TestSupport.assertContains(serialized, "<redacted>", "serialized evidence marks redacted secret");

        var audit = fixedAudit();
        audit.append(io.acra.core.active.evidence.SafetyEventType.VALIDATION_BLOCKED, "E", "T",
                Map.of("detail", "Authorization: Bearer synthetic-audit-secret"));
        String detail = audit.entries().getFirst().details().get("detail");
        TestSupport.assertNotContains(detail, "synthetic-audit-secret", "audit log secret redacted");
        TestSupport.assertContains(detail, "<redacted>", "audit log redaction marker retained");
        tests += 5;
    }

    private record DispatchProof(TestState state, ExecutionErrorType failureType, int sends, int mutationSends) {}

    private static void testMutationDispatchSafety() {
        DispatchProof valid = dispatchProof(Sprint4Fixtures.test("S4-DISPATCH-VALID", "dispatch-valid"));
        TestSupport.assertEquals(TestState.COMPLETED, valid.state(), "valid mutation completes controlled execution");
        TestSupport.assertEquals(4, valid.sends(), "valid mutation permits the four declared transports");
        TestSupport.assertEquals(1, valid.mutationSends(), "valid mutation permits exactly one mutation transport");

        Mutation invalid = new Mutation("MUT-DISPATCH-INVALID", MutationType.METHOD_REPRESENTATION,
                MutationLocation.METHOD, "POST", "HEAD", "ctx-a", "ctx-b",
                "declared original method does not match baseline", "none",
                SafetyClass.SAFE_READ_ONLY, "dispatch-invalid");
        DispatchProof invalidProof = dispatchProof(Sprint4Fixtures.test(
                "S4-DISPATCH-INVALID", "dispatch-invalid", invalid));
        TestSupport.assertEquals(TestState.BLOCKED, invalidProof.state(), "invalid mutation is blocked before dispatch");
        TestSupport.assertEquals(ExecutionErrorType.INVALID_REQUEST, invalidProof.failureType(), "invalid mutation is an invalid request");
        TestSupport.assertEquals(0, invalidProof.sends(), "invalid mutation produces zero transport dispatch");
        TestSupport.assertEquals(0, invalidProof.mutationSends(), "invalid mutation produces zero mutation transport dispatch");

        Mutation contaminated = new Mutation("MUT-DISPATCH-CONTAMINATED", MutationType.IDENTITY_SUBSTITUTION,
                MutationLocation.PATH, "1001", "2001", "ctx-a", "ctx-b",
                "identity declaration must not change the resource path", "none",
                SafetyClass.SAFE_READ_ONLY, "dispatch-contaminated");
        DispatchProof contaminatedProof = dispatchProof(Sprint4Fixtures.test(
                "S4-DISPATCH-CONTAMINATED", "dispatch-contaminated", contaminated));
        TestSupport.assertEquals(TestState.BLOCKED, contaminatedProof.state(), "contaminated mutation is blocked before dispatch");
        TestSupport.assertEquals(ExecutionErrorType.INVALID_REQUEST, contaminatedProof.failureType(), "contaminated mutation is an invalid request");
        TestSupport.assertEquals(0, contaminatedProof.sends(), "contaminated mutation produces zero transport dispatch");
        TestSupport.assertEquals(0, contaminatedProof.mutationSends(), "contaminated mutation produces zero mutation transport dispatch");
        tests += 11;
    }

    private static DispatchProof dispatchProof(io.acra.core.active.model.SecurityTest test) {
        Clock clock = Clock.fixed(Sprint4Fixtures.NOW, ZoneOffset.UTC);
        var audit = new SafetyAuditLog(clock);
        var kill = new KillSwitch(audit);
        kill.reset(true, "dispatch-safety proof");
        var budgets = new HierarchicalBudgetManager(audit);
        configureBudgets(budgets, test, 20);
        var concurrency = new HierarchicalConcurrencyController();
        configureConcurrency(concurrency, test, 1);
        var rate = new ScopedRateLimiter();
        configureRates(rate, test);
        var mutationBudget = new MutationBudgetTracker(10);
        var validator = new MutationValidator(Sprint4Fixtures.PROJECT, Set.of(ExecutionEnvironment.LAB), kill,
                budgets, concurrency, rate, audit);
        AtomicInteger sends = new AtomicInteger();
        AtomicInteger mutationSends = new AtomicInteger();
        HttpTransport transport = (request, timeout, cancellation) -> {
            sends.incrementAndGet();
            if (request.kind() == io.acra.core.active.execution.RequestVariantKind.MUTATION) {
                mutationSends.incrementAndGet();
            }
            var response = switch (request.kind()) {
                case BASELINE, POSITIVE_CONTROL -> Sprint4Fixtures.response(200, "{\"id\":1001}");
                case NEGATIVE_CONTROL, MUTATION -> Sprint4Fixtures.response(403, "{\"message\":\"Access denied\"}");
            };
            return new TransportResult(response, Map.of(), Duration.ofMillis(1));
        };
        var executor = new TestExecutor(clock, Duration.ofSeconds(2), transport, duration -> { }, validator,
                budgets, concurrency, rate, new BackoffPolicy(0, Duration.ofMillis(10)), kill, mutationBudget,
                audit, new ExecutionEvidenceStore(clock));
        var result = executor.execute(test, new ActiveConsent(true, true, true, true, false),
                List.of(Sprint4Fixtures.groundTruthDeny()));
        return new DispatchProof(result.state(), result.failure() == null ? null : result.failure().type(),
                sends.get(), mutationSends.get());
    }

    private static void testExecutorObservationEvidenceAndReplayIdentity() {
        var test = Sprint4Fixtures.test("S4-SEC-EXEC", "exec-key");
        Clock clock = Clock.fixed(Sprint4Fixtures.NOW, ZoneOffset.UTC);
        var audit = new SafetyAuditLog(clock);
        var kill = new KillSwitch(audit);
        kill.reset(true, "synthetic verification");
        var budgets = new HierarchicalBudgetManager(audit);
        configureBudgets(budgets, test, 20);
        var concurrency = new HierarchicalConcurrencyController();
        configureConcurrency(concurrency, test, 1);
        var rate = new ScopedRateLimiter();
        configureRates(rate, test);
        var mutationBudget = new MutationBudgetTracker(10);
        var validator = new MutationValidator(Sprint4Fixtures.PROJECT, Set.of(ExecutionEnvironment.LAB), kill,
                budgets, concurrency, rate, audit);
        var evidence = new ExecutionEvidenceStore(clock);
        AtomicInteger sends = new AtomicInteger();
        HttpTransport transport = (request, timeout, cancellation) -> {
            sends.incrementAndGet();
            cancellation.throwIfCancelled();
            var response = switch (request.kind()) {
                case BASELINE, POSITIVE_CONTROL -> Sprint4Fixtures.response(200,
                        "{\"id\":1001,\"owner_id\":\"User-A\",\"tenant_id\":\"Tenant-A\",\"timestamp\":\"dynamic\"}");
                case NEGATIVE_CONTROL, MUTATION -> Sprint4Fixtures.response(200,
                        "{\"success\":false,\"message\":\"Access denied\",\"request_id\":\"dynamic\"}");
            };
            return new TransportResult(response, Map.of("session", "synthetic-secret-cookie"), Duration.ofMillis(5));
        };
        DelayController noDelay = duration -> { };
        var executor = new TestExecutor(clock, Duration.ofSeconds(2), transport, noDelay, validator, budgets,
                concurrency, rate, new BackoffPolicy(0, Duration.ofMillis(10)), kill, mutationBudget, audit, evidence);
        ActiveConsent consent = new ActiveConsent(true, true, true, true, false);
        var result = executor.execute(test, consent, List.of(Sprint4Fixtures.groundTruthDeny()));
        TestSupport.assertEquals(TestState.COMPLETED, result.state(), "synthetic executor completes");
        TestSupport.assertEquals(4, sends.get(), "exact four controlled requests dispatched by executor test double");
        TestSupport.assertEquals(DifferentialClassification.EXPECTED_CHANGE,
                result.observation().differences().classification(), "executor retains semantic differential observation");
        TestSupport.assertTrue(result.evidenceChain().size() >= 11, "full evidence chain retained");
        TestSupport.assertEquals("<redacted>", result.observation().baseline().cookies().get("session"), "response cookies redacted in evidence snapshot");
        TestSupport.assertEquals(1, evidence.observations().size(), "observation recorded separately from findings");

        var replay = new ReplayService(clock);
        var descriptor = replay.descriptor(result, test);
        var replayResult = replay.replay(descriptor, executor, consent, List.of(Sprint4Fixtures.groundTruthDeny()));
        TestSupport.assertTrue(replayResult.verified(), "replay identity verified before fresh execution");
        TestSupport.assertFalse(result.executionId().equals(replayResult.freshExecution().executionId()), "rerun receives new execution id");
        TestSupport.assertEquals(2, evidence.observations().size(), "replay appends evidence without overwriting prior observation");

        var tampered = new ReplayDescriptor(descriptor.originalExecutionId(), descriptor.test(), "bad-signature",
                descriptor.configurationFingerprint(), descriptor.environmentFingerprint(), descriptor.createdAt());
        int sendsBeforeTamperedReplay = sends.get();
        var rejected = replay.replay(tampered, executor, consent, List.of(Sprint4Fixtures.groundTruthDeny()));
        TestSupport.assertFalse(rejected.verified(), "tampered replay descriptor rejected");
        TestSupport.assertEquals(sendsBeforeTamperedReplay, sends.get(), "rejected replay produces zero dispatch");
        tests += 11;
    }

    private static SafetyAuditLog fixedAudit() {
        return new SafetyAuditLog(Clock.fixed(Sprint4Fixtures.NOW, ZoneOffset.UTC));
    }

    private static void configureBudgets(HierarchicalBudgetManager budgets, io.acra.core.active.model.SecurityTest test, int amount) {
        for (BudgetKey key : budgetKeys(test)) budgets.configure(key, amount);
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

    private static void configureConcurrency(HierarchicalConcurrencyController concurrency,
                                             io.acra.core.active.model.SecurityTest test, int limit) {
        concurrency.configure(new ConcurrencyKey(ConcurrencyScope.GLOBAL, "global"), limit);
        concurrency.configure(new ConcurrencyKey(ConcurrencyScope.HOST, test.target().host()), limit);
        concurrency.configure(new ConcurrencyKey(ConcurrencyScope.TARGET, test.target().targetId()), limit);
        concurrency.configure(new ConcurrencyKey(ConcurrencyScope.ENDPOINT, test.endpoint().endpointId()), limit);
    }

    private static void configureRates(ScopedRateLimiter rate, io.acra.core.active.model.SecurityTest test) {
        RateLimitPolicy policy = new RateLimitPolicy(100, 1000, 100, 0);
        rate.configure("host:" + test.target().host(), policy);
        rate.configure("target:" + test.target().targetId(), policy);
        rate.configure("endpoint:" + test.endpoint().endpointId(), policy);
        rate.configure("test:" + test.testId(), policy);
    }
}
