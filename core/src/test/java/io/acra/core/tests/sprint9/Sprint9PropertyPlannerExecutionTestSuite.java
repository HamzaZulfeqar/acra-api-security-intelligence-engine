package io.acra.core.tests.sprint9;

import io.acra.core.active.analysis.AuthorizationOutcome;
import io.acra.core.active.analysis.DifferentialClassification;
import io.acra.core.active.analysis.ExpectedDecisionCandidate;
import io.acra.core.active.analysis.ExpectedDecisionSource;
import io.acra.core.active.evidence.ExecutionEvidenceStore;
import io.acra.core.active.evidence.SafetyAuditLog;
import io.acra.core.active.execution.BackoffPolicy;
import io.acra.core.active.execution.DelayController;
import io.acra.core.active.execution.LocalhostHttpTransport;
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
import io.acra.core.active.model.UserMode;
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

public final class Sprint9PropertyPlannerExecutionTestSuite {
    private static final String PROJECT = "acra-s9";
    private static final Instant NOW = Instant.parse("2026-09-24T10:20:00Z");

    private Sprint9PropertyPlannerExecutionTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT9_PROPERTY_PLANNER_EXECUTION PASS assertions=" + assertions);
    }

    public static int run() {
        int assertions = 0;

        Execution secure = execute(18082, "secure");
        TestSupport.assertEquals(io.acra.core.domain.testing.TestState.COMPLETED, secure.result().state(),
                "secure controlled property test completes through the existing active engine");
        assertions++;
        TestSupport.assertEquals(TestContract.PROPERTY, secure.test().category(),
                "generated test remains the existing PROPERTY contract");
        assertions++;
        TestSupport.assertEquals(SafetyClass.STATE_CHANGING, secure.test().mutation().safetyClass(),
                "property update is explicitly classified as state changing");
        assertions++;
        TestSupport.assertTrue(secure.test().safetyPolicy().confirmationRequired(),
                "state-changing property execution requires confirmation");
        assertions++;
        TestSupport.assertEquals(AuthorizationOutcome.DENY, secure.result().observation().observedDecision(),
                "secure fixture denies the privileged property mutation");
        assertions++;
        TestSupport.assertTrue(
                secure.result().observation().differences().classification()
                        != DifferentialClassification.UNEXPECTED_CHANGE,
                "secure fixture does not produce an unexpected authorization change");
        assertions++;

        Execution vulnerable = execute(18081, "vulnerable");
        TestSupport.assertEquals(io.acra.core.domain.testing.TestState.COMPLETED, vulnerable.result().state(),
                "vulnerable controlled property test completes through the existing active engine");
        assertions++;
        TestSupport.assertEquals(AuthorizationOutcome.ALLOW, vulnerable.result().observation().observedDecision(),
                "deliberately vulnerable fixture allows the denied property mutation");
        assertions++;
        TestSupport.assertEquals(DifferentialClassification.UNEXPECTED_CHANGE,
                vulnerable.result().observation().differences().classification(),
                "explicit expected DENY plus observed ALLOW is an unexpected property differential");
        assertions++;
        TestSupport.assertTrue(vulnerable.result().observation().evidenceIds().size() > 0,
                "property execution retains evidence lineage");
        assertions++;

        String rawToken = token("user-a", "tenant-a", "viewer");
        String serialized = new io.acra.core.serialization.DomainSerializer().serialize(vulnerable.test());
        TestSupport.assertNotContains(serialized, rawToken,
                "serialized property test does not expose the raw synthetic bearer token");
        assertions++;

        System.out.println("SPRINT9_PROPERTY_SECURE testId=" + secure.test().testId()
                + " expected=" + secure.test().expectedDecision()
                + " observed=" + secure.result().observation().observedDecision()
                + " differential=" + secure.result().observation().differences().classification());
        System.out.println("SPRINT9_PROPERTY_VULNERABLE testId=" + vulnerable.test().testId()
                + " expected=" + vulnerable.test().expectedDecision()
                + " observed=" + vulnerable.result().observation().observedDecision()
                + " differential=" + vulnerable.result().observation().differences().classification());

        return assertions;
    }

    private static Execution execute(int port, String label) {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        Harness harness = harness(clock, port);
        ActiveEngineWorkspace workspace = new ActiveEngineWorkspace(clock, harness.executor());
        workspace.configureMode(UserMode.RESEARCHER);
        workspace.configureProfile(TestProfile.RESEARCH_EXPERIMENT);

        var planning = workspace.plan(planningInput(port));
        TestSupport.assertEquals(1, planning.plan().tests().size(),
                label + " property plan should contain exactly one controlled property test");
        var test = planning.plan().tests().getFirst();

        configureExecutionGuards(harness, test);
        harness.kill().reset(true, "authorized Sprint 9 localhost " + label + " property execution");

        ExpectedDecisionCandidate expected = new ExpectedDecisionCandidate(
                AuthorizationDecision.DENY,
                ExpectedDecisionSource.EXPLICIT_CONFIGURED_POLICY,
                "s9-property-is-admin-policy",
                List.of("s9-property-policy-evidence"),
                1.0);

        var result = workspace.executeNextLocal(List.of(expected)).orElseThrow();
        return new Execution(test, result, harness.evidence());
    }

    private static PlanningInput planningInput(int port) {
        Endpoint endpoint = endpoint();
        ApiEndpointRecord inventory = new ApiEndpointRecord(
                endpoint, "v1", "LAB", AuthenticationType.BEARER, "ACRA-Lab",
                NOW, NOW, DocumentationStatus.DOCUMENTED, RiskTier.HIGH);

        SafetyPolicy safety = new SafetyPolicy(
                SafetyClass.STATE_CHANGING,
                true,
                false,
                20,
                5,
                2 * 1024 * 1024,
                Set.of(HttpMethod.PATCH));

        return new PlanningInput(
                "PLAN-S9-PROPERTY-" + port,
                List.of(inventory),
                new SecurityContextGraph(),
                new AuthorizationMatrix(),
                List.of(viewerContext()),
                List.of(),
                null,
                target(port),
                Set.of(TestContract.PROPERTY),
                Map.of(),
                Map.of(),
                20,
                5,
                safety,
                SelectionMode.ALL,
                TestProfileDefinition.defaults(TestProfile.RESEARCH_EXPERIMENT),
                ConfigurationSnapshot.of(Map.of(
                        "dataset", "S9-PROPERTY-AUTHORIZATION",
                        "environment", "LAB",
                        "target", "localhost:" + port)),
                List.of(seed(port)),
                NOW);
    }

    private static TestSeed seed(int port) {
        return new TestSeed(
                "S9-AUTO-PROPERTY-IS-ADMIN",
                "1",
                TestContract.PROPERTY,
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
                List.of("s9-property-policy-evidence"),
                List.of(),
                List.of(
                        "method remains PATCH",
                        "authority remains localhost",
                        "authentication context remains user-a",
                        "only one declared JSON property fragment changes",
                        "execution remains controlled ACRA-Lab state-changing"),
                4,
                true,
                false,
                false,
                "controlled explicit property authorization validation");
    }

    private static Mutation mutation() {
        return new Mutation(
                "S9-MUT-PROPERTY-IS-ADMIN",
                MutationType.PROPERTY,
                MutationLocation.BODY,
                "\"display_name\":\"Baseline Name\"",
                "\"is_admin\":true",
                "user-a-property-context",
                "user-a-property-context",
                "replace an allowed profile property with the explicitly denied is_admin property",
                "authorization decision must be DENY",
                SafetyClass.STATE_CHANGING,
                "property:is_admin");
    }

    private static Endpoint endpoint() {
        return new Endpoint(
                "EP-S9-PROFILE-PATCH",
                HttpMethod.PATCH,
                "/api/v1/s9/users/{user}/profile",
                "/api/v1/s9/users/{user}/profile",
                "/api/v1/s9/users/user-a/profile",
                "localhost",
                "v1",
                List.of("Sprint 9 controlled property authorization fixture"));
    }

    private static TargetDescriptor target(int port) {
        return new TargetDescriptor(
                PROJECT,
                "acra-lab-s9-" + port,
                "http",
                "localhost",
                port,
                ExecutionEnvironment.LAB,
                true,
                List.of("/api/v1/s9/users/user-a/profile"),
                Set.of(HttpMethod.PATCH));
    }

    private static RequestDefinition baseline(int port) {
        return request(port, "S9-REQ-BASE", "user-a", "{\"display_name\":\"Baseline Name\"}");
    }

    private static RequestDefinition positive(int port) {
        return request(port, "S9-REQ-POS", "user-a", "{\"display_name\":\"Positive Name\"}");
    }

    private static RequestDefinition negative(int port) {
        HttpRequest request = HttpRequest.of(
                HttpMethod.PATCH,
                "http",
                "localhost",
                port,
                "/api/v1/s9/users/user-a/profile",
                List.of(
                        new HttpHeader("Authorization", "Bearer " + token("user-b", "tenant-b", "viewer")),
                        new HttpHeader("Accept", "application/json"),
                        new HttpHeader("Content-Type", "application/json")),
                "{\"display_name\":\"Negative Name\"}".getBytes(StandardCharsets.UTF_8),
                HttpProtocol.HTTP_1_1);
        return new RequestDefinition("S9-REQ-NEG", request, "user-b-property-context", "profile:user-a");
    }

    private static RequestDefinition request(int port, String id, String principal, String body) {
        HttpRequest request = HttpRequest.of(
                HttpMethod.PATCH,
                "http",
                "localhost",
                port,
                "/api/v1/s9/users/user-a/profile",
                List.of(
                        new HttpHeader("Authorization", "Bearer " + token(principal, "tenant-a", "viewer")),
                        new HttpHeader("Accept", "application/json"),
                        new HttpHeader("Content-Type", "application/json")),
                body.getBytes(StandardCharsets.UTF_8),
                HttpProtocol.HTTP_1_1);
        return new RequestDefinition(id, request, principal + "-property-context", "profile:user-a");
    }

    private static SecurityContextFingerprint viewerContext() {
        return new SecurityContextFingerprint(
                "user-a",
                "viewer",
                "tenant-a",
                "profile:user-a",
                "user-a",
                "UPDATE_PROPERTY",
                "ACTIVE",
                "PATCH /api/v1/s9/users/user-a/profile",
                "RAW",
                "user-a-property-context",
                AuthorizationDecision.DENY,
                AuthorizationDecision.UNKNOWN,
                List.of("s9-property-policy-evidence"));
    }

    private static Harness harness(Clock clock, int port) {
        SafetyAuditLog audit = new SafetyAuditLog(clock);
        KillSwitch kill = new KillSwitch(audit);
        HierarchicalBudgetManager budgets = new HierarchicalBudgetManager(audit);
        HierarchicalConcurrencyController concurrency = new HierarchicalConcurrencyController();
        ScopedRateLimiter rate = new ScopedRateLimiter();
        MutationValidator validator = new MutationValidator(
                PROJECT, Set.of(ExecutionEnvironment.LAB), kill, budgets, concurrency, rate, audit);
        ExecutionEvidenceStore evidence = new ExecutionEvidenceStore(clock, PROJECT);
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
        return new Harness(executor, kill, budgets, concurrency, rate, evidence);
    }

    private static void configureExecutionGuards(
            Harness harness,
            io.acra.core.active.model.SecurityTest test) {

        for (BudgetKey key : List.of(
                new BudgetKey(BudgetScope.GLOBAL, "global"),
                new BudgetKey(BudgetScope.PROJECT, test.target().projectId()),
                new BudgetKey(BudgetScope.TARGET, test.target().targetId()),
                new BudgetKey(BudgetScope.ENDPOINT, test.endpoint().endpointId()),
                new BudgetKey(BudgetScope.TEST, test.testId()),
                new BudgetKey(BudgetScope.CONTEXT,
                        test.targetContext().principal() + "|"
                                + test.targetContext().tenant() + "|"
                                + test.targetContext().role()))) {
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
        String header = encoder.encodeToString(
                "{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = encoder.encodeToString(
                ("{\"sub\":\"" + sub + "\",\"tenant_id\":\"" + tenant
                        + "\",\"role\":\"" + role + "\"}").getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".s9synthetic";
    }

    private record Harness(
            TestExecutor executor,
            KillSwitch kill,
            HierarchicalBudgetManager budgets,
            HierarchicalConcurrencyController concurrency,
            ScopedRateLimiter rate,
            ExecutionEvidenceStore evidence) { }

    private record Execution(
            io.acra.core.active.model.SecurityTest test,
            io.acra.core.active.execution.TestExecutionResult result,
            ExecutionEvidenceStore evidence) { }
}
