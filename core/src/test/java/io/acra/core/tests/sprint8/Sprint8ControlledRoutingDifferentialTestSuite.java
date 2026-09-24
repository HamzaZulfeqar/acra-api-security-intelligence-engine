package io.acra.core.tests.sprint8;

import io.acra.core.active.analysis.AuthorizationOutcome;
import io.acra.core.active.analysis.DifferentialClassification;
import io.acra.core.active.analysis.ExpectedDecisionCandidate;
import io.acra.core.active.analysis.ExpectedDecisionSource;
import io.acra.core.active.evidence.ExecutionEvidenceStore;
import io.acra.core.active.evidence.SafetyAuditLog;
import io.acra.core.active.execution.BackoffPolicy;
import io.acra.core.active.execution.DelayController;
import io.acra.core.active.execution.LocalhostHttpTransport;
import io.acra.core.active.execution.RequestBuilder;
import io.acra.core.active.execution.TestExecutor;
import io.acra.core.active.model.ConfigurationSnapshot;
import io.acra.core.active.model.ExecutionEnvironment;
import io.acra.core.active.model.Mutation;
import io.acra.core.active.model.MutationLocation;
import io.acra.core.active.model.MutationType;
import io.acra.core.active.model.RequestDefinition;
import io.acra.core.active.model.SafetyClass;
import io.acra.core.active.model.SafetyPolicy;
import io.acra.core.active.model.SelectionMode;
import io.acra.core.active.model.TargetDescriptor;
import io.acra.core.active.model.TestContract;
import io.acra.core.active.model.TestProfile;
import io.acra.core.active.model.TestProfileDefinition;
import io.acra.core.active.planning.PlanningInput;
import io.acra.core.active.planning.TestSeed;
import io.acra.core.active.product.ActiveEngineWorkspace;
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
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.AuthorizationMatrix;
import io.acra.core.domain.endpoint.ApiEndpointRecord;
import io.acra.core.domain.endpoint.DocumentationStatus;
import io.acra.core.domain.endpoint.Endpoint;
import io.acra.core.domain.endpoint.RiskTier;
import io.acra.core.domain.http.HttpHeader;
import io.acra.core.domain.http.HttpMethod;
import io.acra.core.domain.http.HttpProtocol;
import io.acra.core.domain.http.HttpRequest;
import io.acra.core.domain.identity.AuthenticationType;
import io.acra.core.graph.SecurityContextGraph;
import io.acra.core.recon.SecurityContextFingerprint;
import io.acra.core.tests.TestSupport;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Sprint8ControlledRoutingDifferentialTestSuite {
    private static final String PROJECT = "acra-s8";
    private static final Instant NOW = Instant.parse("2026-09-24T06:20:00Z");

    private Sprint8ControlledRoutingDifferentialTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT8_CONTROLLED_ROUTING_DIFFERENTIAL PASS assertions=" + assertions);
    }

    public static int run() {
        int assertions = 0;
        Mutation mutation = mutation();
        HttpRequest built = new RequestBuilder().mutate(baseline(18082).request(), mutation);
        TestSupport.assertEquals("/api//v1/s8/admin", built.rawTarget(),
                "declared route-equivalence mutation must change only URI representation");
        assertions++;

        ExecutionResult secure = execute(18082, "secure");
        TestSupport.assertEquals(AuthorizationOutcome.DENY, secure.result().observation().observedDecision(),
                "secure fixture preserves DENY across equivalent route representation");
        assertions++;
        TestSupport.assertEquals(DifferentialClassification.NO_CHANGE,
                secure.result().observation().differences().classification(),
                "secure equivalent route remains semantically denied");
        assertions++;

        ExecutionResult vulnerable = execute(18081, "vulnerable");
        TestSupport.assertEquals(AuthorizationOutcome.ALLOW, vulnerable.result().observation().observedDecision(),
                "deliberately vulnerable fixture allows equivalent-route authorization mismatch");
        assertions++;
        TestSupport.assertEquals(DifferentialClassification.UNEXPECTED_CHANGE,
                vulnerable.result().observation().differences().classification(),
                "expected DENY plus observed ALLOW is an unexpected routing differential");
        assertions++;
        TestSupport.assertTrue(vulnerable.result().observation().evidenceIds().size() > 0,
                "live routing differential retains evidence references");
        assertions++;
        TestSupport.assertEquals(io.acra.core.domain.testing.TestState.COMPLETED, vulnerable.result().state(),
                "controlled routing test completes through existing active engine");
        assertions++;
        TestSupport.assertEquals(TestContract.ROUTE_EQUIVALENCE, vulnerable.test().category(),
                "controlled test remains routing category");
        assertions++;
        TestSupport.assertEquals(SafetyClass.SAFE_READ_ONLY, vulnerable.test().mutation().safetyClass(),
                "controlled route representation mutation remains safe read-only");
        assertions++;

        String rawViewer = token("viewer-a", "tenant-a", "viewer");
        String serialized = new io.acra.core.serialization.DomainSerializer().serialize(vulnerable.test());
        TestSupport.assertNotContains(serialized, rawViewer,
                "serialized route test must not expose raw bearer token");
        assertions++;

        System.out.println("SPRINT8_ROUTE_SECURE testId=" + secure.test().testId()
                + " expected=" + secure.test().expectedDecision()
                + " observed=" + secure.result().observation().observedDecision()
                + " differential=" + secure.result().observation().differences().classification());
        System.out.println("SPRINT8_ROUTE_VULNERABLE testId=" + vulnerable.test().testId()
                + " expected=" + vulnerable.test().expectedDecision()
                + " observed=" + vulnerable.result().observation().observedDecision()
                + " differential=" + vulnerable.result().observation().differences().classification());
        return assertions;
    }

    private static ExecutionResult execute(int port, String label) {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        Harness harness = harness(clock, port);
        ActiveEngineWorkspace workspace = new ActiveEngineWorkspace(clock, harness.executor());
        workspace.configureProfile(TestProfile.ROUTING_DIFFERENTIAL);
        var planning = workspace.plan(planningInput(port));
        TestSupport.assertEquals(1, planning.plan().tests().size(),
                label + " routing plan should contain exactly one controlled route-equivalence test");
        var test = planning.plan().tests().getFirst();
        configureExecutionGuards(harness, test);
        harness.kill().reset(true, "authorized Sprint 8 localhost " + label + " routing execution");
        ExpectedDecisionCandidate expected = new ExpectedDecisionCandidate(
                AuthorizationDecision.DENY,
                ExpectedDecisionSource.EXPLICIT_CONFIGURED_POLICY,
                "s8-route-equivalence-policy",
                List.of("s8-route-policy-evidence"),
                1.0);
        var result = workspace.executeNextLocal(List.of(expected)).orElseThrow();
        return new ExecutionResult(test, result);
    }

    private static PlanningInput planningInput(int port) {
        Endpoint endpoint = endpoint();
        ApiEndpointRecord inventory = new ApiEndpointRecord(
                endpoint, "v1", "LAB", AuthenticationType.BEARER, "ACRA-Lab",
                NOW, NOW, DocumentationStatus.DOCUMENTED, RiskTier.HIGH);
        SafetyPolicy safety = new SafetyPolicy(
                SafetyClass.SAFE_READ_ONLY, true, true, 20, 5, 2 * 1024 * 1024, Set.of(HttpMethod.GET));
        return new PlanningInput(
                "PLAN-S8-ROUTE-" + port,
                List.of(inventory),
                new SecurityContextGraph(),
                new AuthorizationMatrix(),
                List.of(viewerContext()),
                List.of(),
                null,
                target(port),
                Set.of(TestContract.ROUTE_EQUIVALENCE),
                Map.of(),
                Map.of(),
                20,
                5,
                safety,
                SelectionMode.ALL,
                TestProfileDefinition.defaults(TestProfile.ROUTING_DIFFERENTIAL),
                ConfigurationSnapshot.of(Map.of(
                        "dataset", "S8-ROUTING-NORMALIZATION",
                        "environment", "LAB",
                        "target", "localhost:" + port)),
                List.of(seed(port)),
                NOW);
    }

    private static TestSeed seed(int port) {
        return new TestSeed(
                "S8-AUTO-ROUTE-EQUIVALENCE",
                "1",
                TestContract.ROUTE_EQUIVALENCE,
                endpoint(),
                baseline(port),
                positive(port),
                negative(port),
                mutation(),
                viewerContext(),
                viewerContext(),
                null,
                null,
                AuthorizationDecision.DENY,
                List.of("s8-route-policy-evidence"),
                List.of(),
                List.of(
                        "method remains GET",
                        "authority remains localhost",
                        "authentication context remains viewer-a",
                        "only URI representation changes"),
                4,
                true,
                false,
                false,
                "controlled explicit equivalent-route authorization validation");
    }

    private static Mutation mutation() {
        return new Mutation(
                "S8-MUT-ROUTE-EQUIVALENCE",
                MutationType.EQUIVALENT_ROUTE_REPRESENTATION,
                MutationLocation.URI_REPRESENTATION,
                "/api/v1/s8/admin",
                "/api//v1/s8/admin",
                "viewer-route-context",
                "viewer-route-context",
                "duplicate-separator equivalent route representation",
                "authorization decision must remain DENY",
                SafetyClass.SAFE_READ_ONLY,
                "s8-route-equivalence-admin");
    }

    private static Endpoint endpoint() {
        return new Endpoint(
                "EP-S8-ADMIN",
                HttpMethod.GET,
                "/api/v1/s8/admin",
                "/api/v1/s8/admin",
                "/api/v1/s8/admin",
                "localhost",
                "v1",
                List.of("Sprint 8 controlled route-normalization fixture"));
    }

    private static TargetDescriptor target(int port) {
        return new TargetDescriptor(
                PROJECT, "acra-lab-s8-" + port, "http", "localhost", port,
                ExecutionEnvironment.LAB, true, List.of("/api/v1/s8"), Set.of(HttpMethod.GET));
    }

    private static RequestDefinition baseline(int port) {
        return definition(port, "S8-REQ-BASE", "viewer-a", "viewer", true);
    }

    private static RequestDefinition positive(int port) {
        return definition(port, "S8-REQ-POS", "admin-a", "admin", true);
    }

    private static RequestDefinition negative(int port) {
        HttpRequest request = HttpRequest.of(
                HttpMethod.GET, "http", "localhost", port, "/api/v1/s8/admin",
                List.of(new HttpHeader("Accept", "application/json")),
                new byte[0], HttpProtocol.HTTP_1_1);
        return new RequestDefinition("S8-REQ-NEG", request, "anonymous-context", "route:s8-admin");
    }

    private static RequestDefinition definition(
            int port, String id, String principal, String role, boolean authenticated) {
        List<HttpHeader> headers = authenticated
                ? List.of(
                        new HttpHeader("Authorization", "Bearer " + token(principal, "tenant-a", role)),
                        new HttpHeader("Accept", "application/json"))
                : List.of(new HttpHeader("Accept", "application/json"));
        HttpRequest request = HttpRequest.of(
                HttpMethod.GET, "http", "localhost", port, "/api/v1/s8/admin",
                headers, new byte[0], HttpProtocol.HTTP_1_1);
        return new RequestDefinition(id, request, principal + "-route-context", "route:s8-admin");
    }

    private static SecurityContextFingerprint viewerContext() {
        return new SecurityContextFingerprint(
                "viewer-a", "viewer", "tenant-a", "s8-admin", "system",
                "READ", "NONE", "GET /api/v1/s8/admin", "RAW",
                "viewer-route-context", AuthorizationDecision.DENY, AuthorizationDecision.UNKNOWN,
                List.of("s8-route-policy-evidence"));
    }

    private static Harness harness(Clock clock, int port) {
        SafetyAuditLog audit = new SafetyAuditLog(clock);
        KillSwitch kill = new KillSwitch(audit);
        HierarchicalBudgetManager budgets = new HierarchicalBudgetManager(audit);
        HierarchicalConcurrencyController concurrency = new HierarchicalConcurrencyController();
        ScopedRateLimiter rate = new ScopedRateLimiter();
        MutationValidator validator = new MutationValidator(
                PROJECT, Set.of(ExecutionEnvironment.LAB), kill, budgets, concurrency, rate, audit);
        ExecutionEvidenceStore evidence = new ExecutionEvidenceStore(clock);
        DelayController noDelay = duration -> { };
        TestExecutor executor = new TestExecutor(
                clock,
                Duration.ofSeconds(2),
                new LocalhostHttpTransport(target(port)),
                noDelay,
                validator,
                budgets,
                concurrency,
                rate,
                new BackoffPolicy(0, Duration.ofMillis(25)),
                kill,
                new MutationBudgetTracker(20),
                audit,
                evidence);
        return new Harness(executor, kill, budgets, concurrency, rate);
    }

    private static void configureExecutionGuards(Harness harness, io.acra.core.active.model.SecurityTest test) {
        for (BudgetKey key : List.of(
                new BudgetKey(BudgetScope.GLOBAL, "global"),
                new BudgetKey(BudgetScope.PROJECT, test.target().projectId()),
                new BudgetKey(BudgetScope.TARGET, test.target().targetId()),
                new BudgetKey(BudgetScope.ENDPOINT, test.endpoint().endpointId()),
                new BudgetKey(BudgetScope.TEST, test.testId()),
                new BudgetKey(BudgetScope.CONTEXT, test.targetContext().principal() + '|'
                        + test.targetContext().tenant() + '|' + test.targetContext().role()))) {
            harness.budgets().configure(key, 50);
        }
        for (ConcurrencyKey key : List.of(
                new ConcurrencyKey(ConcurrencyScope.GLOBAL, "global"),
                new ConcurrencyKey(ConcurrencyScope.HOST, test.target().host()),
                new ConcurrencyKey(ConcurrencyScope.TARGET, test.target().targetId()),
                new ConcurrencyKey(ConcurrencyScope.ENDPOINT, test.endpoint().endpointId()))) {
            harness.concurrency().configure(key, 2);
        }
        RateLimitPolicy policy = new RateLimitPolicy(1000, 5000, 1000, 0);
        for (String key : List.of(
                "host:" + test.target().host(),
                "target:" + test.target().targetId(),
                "endpoint:" + test.endpoint().endpointId(),
                "test:" + test.testId())) {
            harness.rate().configure(key, policy);
        }
    }

    private static String token(String sub, String tenant, String role) {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String header = encoder.encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}"
                .getBytes(StandardCharsets.UTF_8));
        String payload = encoder.encodeToString(("{\"sub\":\"" + sub + "\",\"tenant_id\":\"" + tenant
                + "\",\"role\":\"" + role + "\"}").getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".s8synthetic";
    }

    private record Harness(
            TestExecutor executor,
            KillSwitch kill,
            HierarchicalBudgetManager budgets,
            HierarchicalConcurrencyController concurrency,
            ScopedRateLimiter rate) { }

    private record ExecutionResult(
            io.acra.core.active.model.SecurityTest test,
            io.acra.core.active.execution.TestExecutionResult result) { }
}
