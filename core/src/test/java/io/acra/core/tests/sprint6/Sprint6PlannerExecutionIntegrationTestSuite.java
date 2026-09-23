package io.acra.core.tests.sprint6;

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
import io.acra.core.active.model.RequestDefinition;
import io.acra.core.active.model.SafetyPolicy;
import io.acra.core.active.model.SelectionMode;
import io.acra.core.active.model.TargetDescriptor;
import io.acra.core.active.model.TestContract;
import io.acra.core.active.model.TestProfile;
import io.acra.core.active.model.TestProfileDefinition;
import io.acra.core.active.planning.PlanningInput;
import io.acra.core.active.planning.S6PolicyPlanningBridge;
import io.acra.core.active.planning.S6PolicyPlanningCandidate;
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
import io.acra.core.domain.authorization.AuthorizationPolicySnapshot;
import io.acra.core.domain.authorization.AuthorizationScope;
import io.acra.core.domain.authorization.EffectiveAuthorizationRequest;
import io.acra.core.domain.authorization.EffectiveAuthorizationResolution;
import io.acra.core.domain.authorization.Permission;
import io.acra.core.domain.authorization.RoleAssignment;
import io.acra.core.domain.authorization.RolePermissionAssignment;
import io.acra.core.domain.common.Confidence;
import io.acra.core.domain.endpoint.ApiEndpointRecord;
import io.acra.core.domain.endpoint.DocumentationStatus;
import io.acra.core.domain.endpoint.Endpoint;
import io.acra.core.domain.endpoint.RiskTier;
import io.acra.core.domain.http.HttpHeader;
import io.acra.core.domain.http.HttpMethod;
import io.acra.core.domain.http.HttpProtocol;
import io.acra.core.domain.http.HttpRequest;
import io.acra.core.domain.identity.AuthenticationType;
import io.acra.core.domain.resource.Resource;
import io.acra.core.engine.EffectiveAuthorizationResolver;
import io.acra.core.graph.SecurityContextGraph;
import io.acra.core.recon.SecurityContextFingerprint;
import io.acra.core.serialization.DomainSerializer;
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

public final class Sprint6PlannerExecutionIntegrationTestSuite {
    private static final String PROJECT = "acra-s6";
    private static final int SECURE_PORT = 18082;
    private static final Instant NOW = Instant.parse("2026-09-23T15:15:00Z");

    private Sprint6PlannerExecutionIntegrationTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT6_PLANNER_EXECUTION PASS assertions=" + assertions);
    }

    public static int run() {
        int assertions = 0;
        AuthorizationPolicySnapshot policy = policy();
        EffectiveAuthorizationResolver resolver = new EffectiveAuthorizationResolver();

        EffectiveAuthorizationResolution baselineResolution = resolver.resolve(policy,
                new EffectiveAuthorizationRequest("user-a", "tenant-a", "tenant-a", "report-common", "report",
                        "/api/v1/s6/tenants/tenant-a/reports/report-common", "", "READ_REPORT",
                        AuthorizationDecision.UNKNOWN, false, NOW));
        EffectiveAuthorizationResolution targetResolution = resolver.resolve(policy,
                new EffectiveAuthorizationRequest("user-a", "tenant-a", "tenant-b", "report-common", "report",
                        "/api/v1/s6/tenants/tenant-b/reports/report-common", "", "READ_REPORT",
                        AuthorizationDecision.UNKNOWN, false, NOW));

        S6PolicyPlanningCandidate candidate = candidate(baselineResolution, targetResolution);
        PlanningInput base = planningInput();
        var augmentation = new S6PolicyPlanningBridge().augment(base, List.of(candidate));

        TestSupport.assertEquals(1, augmentation.generatedSeeds().size(),
                "policy bridge should generate one safe CROSS_TENANT seed");
        assertions++;
        TestSupport.assertTrue(augmentation.skippedReasons().stream()
                        .anyMatch(reason -> reason.contains("ROLE_COMPARISON_REQUIRES_CREDENTIAL_SAFE_CONTEXT_SUBSTITUTION")),
                "role comparison must fail closed instead of copying credentials into a Mutation");
        assertions++;
        var generated = augmentation.generatedSeeds().getFirst();
        TestSupport.assertEquals(TestContract.CROSS_TENANT, generated.contract(),
                "generated contract should be CROSS_TENANT");
        assertions++;
        TestSupport.assertEquals("tenant-a", generated.mutation().originalValue(),
                "generated mutation should contain non-secret tenant value");
        assertions++;
        TestSupport.assertEquals("tenant-b", generated.mutation().mutatedValue(),
                "generated mutation should target explicit tenant context");
        assertions++;

        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        Harness harness = harness(clock);
        ActiveEngineWorkspace workspace = new ActiveEngineWorkspace(clock, harness.executor());
        var planning = workspace.plan(augmentation.planningInput());
        TestSupport.assertEquals(1, planning.plan().tests().size(),
                "existing S4 planner should admit generated S6 test");
        assertions++;
        TestSupport.assertEquals(1, workspace.queue().snapshots().size(),
                "generated S6 test should enter existing execution queue");
        assertions++;

        var test = planning.plan().tests().getFirst();
        configureExecutionGuards(harness, test);
        harness.kill().reset(true, "authorized Sprint 6 localhost generated-test execution");
        ExpectedDecisionCandidate expected = new ExpectedDecisionCandidate(
                AuthorizationDecision.DENY, ExpectedDecisionSource.EXPLICIT_CONFIGURED_POLICY,
                policy.policyId(), targetResolution.evidenceIds(), 1.0);
        var result = workspace.executeNextLocal(List.of(expected)).orElseThrow();

        TestSupport.assertEquals(io.acra.core.domain.testing.TestState.COMPLETED, result.state(),
                "policy-generated test should execute through the existing S4 executor");
        assertions++;
        TestSupport.assertEquals(AuthorizationOutcome.ALLOW,
                result.observation().differences().baselineOutcome(),
                "baseline same-tenant control should allow");
        assertions++;
        TestSupport.assertEquals(AuthorizationOutcome.DENY,
                result.observation().differences().negativeControlOutcome(),
                "explicit cross-tenant negative control should deny");
        assertions++;
        TestSupport.assertEquals(AuthorizationOutcome.DENY,
                result.observation().observedDecision(),
                "generated tenant mutation should be denied by secure ACRA-Lab");
        assertions++;
        TestSupport.assertEquals(DifferentialClassification.EXPECTED_CHANGE,
                result.observation().differences().classification(),
                "generated cross-tenant mutation should match expected policy change");
        assertions++;
        TestSupport.assertEquals(io.acra.core.domain.testing.TestState.COMPLETED,
                workspace.queue().snapshot(test.testId()).state(),
                "queue should close the generated test as completed");
        assertions++;

        String rawToken = token("user-a", "tenant-a", "viewer");
        String serializedTest = new DomainSerializer().serialize(test);
        TestSupport.assertNotContains(serializedTest, rawToken,
                "generated test serialization must not expose synthetic credentials");
        assertions++;
        TestSupport.assertFalse(test.mutation().originalValue().contains(rawToken)
                        || test.mutation().mutatedValue().contains(rawToken),
                "Mutation values must never contain credential material");
        assertions++;

        System.out.println("SPRINT6_AUTO_PLAN testId=" + test.testId()
                + " contract=" + test.category()
                + " expected=" + test.expectedDecision()
                + " differential=" + result.observation().differences().classification()
                + " queueState=" + workspace.queue().snapshot(test.testId()).state());
        return assertions;
    }

    private static S6PolicyPlanningCandidate candidate(EffectiveAuthorizationResolution baseline,
                                                        EffectiveAuthorizationResolution target) {
        Endpoint endpoint = endpoint();
        return new S6PolicyPlanningCandidate(
                "S6-CANDIDATE-CROSS-TENANT",
                endpoint,
                baselineDefinition(),
                positiveDefinition(),
                negativeDefinition(),
                sourceContext(),
                targetContext(),
                sourceResource(),
                targetResource(),
                baseline,
                target,
                Set.of(TestContract.CROSS_TENANT, TestContract.ROLE_COMPARISON),
                List.of(),
                List.of("principal remains user-a", "method remains GET", "resource identifier remains report-common"),
                false,
                false);
    }

    private static PlanningInput planningInput() {
        ApiEndpointRecord inventory = new ApiEndpointRecord(endpoint(), "v1", "LAB", AuthenticationType.BEARER,
                "ACRA-Lab", NOW, NOW, DocumentationStatus.DOCUMENTED, RiskTier.HIGH);
        return new PlanningInput(
                "PLAN-S6-POLICY-AUTO",
                List.of(inventory),
                new SecurityContextGraph(),
                new AuthorizationMatrix(),
                List.of(sourceContext(), targetContext()),
                List.of(),
                null,
                target(),
                Set.of(TestContract.CROSS_TENANT, TestContract.ROLE_COMPARISON),
                Map.of(),
                Map.of(),
                20,
                5,
                SafetyPolicy.safeLabReadOnly(20, 5),
                SelectionMode.AUTOMATIC,
                TestProfileDefinition.defaults(TestProfile.SAFE_LAB),
                ConfigurationSnapshot.of(Map.of(
                        "dataset", "GT-S6-TENANT-RBAC",
                        "environment", "LAB",
                        "target", "localhost:18082")),
                List.of(),
                NOW);
    }

    private static AuthorizationPolicySnapshot policy() {
        return AuthorizationPolicySnapshot.create(
                "s6-auto-policy", "1", "controlled-local-lab", List.of(),
                List.of(new RoleAssignment("ra-viewer", "user-a", "viewer", "tenant-a",
                        AuthorizationScope.tenant("tenant-a"), true, List.of("e-role"))),
                List.of(),
                List.of(new Permission("p-read-a", "READ_REPORT", "report", "", "",
                        AuthorizationScope.tenant("tenant-a"), List.of("e-permission"))),
                List.of(new RolePermissionAssignment("rp-read-a", "viewer", "p-read-a", "tenant-a",
                        List.of("e-role-permission"))),
                List.of(), List.of(),
                List.of("e-policy"), AuthorizationDecision.DENY, NOW);
    }

    private static Endpoint endpoint() {
        return new Endpoint("EP-S6-AUTO-TENANT", HttpMethod.GET,
                "/api/v1/s6/tenants/tenant-a/reports/report-common",
                "/api/v1/s6/tenants/{tenant}/reports/report-common",
                "/api/v1/s6/tenants/{tenant}/reports/{report}",
                "localhost", "v1", List.of("Sprint 6 policy-generated tenant test"));
    }

    private static TargetDescriptor target() {
        return new TargetDescriptor(PROJECT, "acra-lab-secure-s6", "http", "localhost", SECURE_PORT,
                ExecutionEnvironment.LAB, true, List.of("/api/v1/s6"), Set.of(HttpMethod.GET));
    }

    private static RequestDefinition baselineDefinition() {
        return new RequestDefinition("S6-REQ-BASE",
                request("/api/v1/s6/tenants/tenant-a/reports/report-common"),
                "ctx-user-a-tenant-a", "report:report-common");
    }

    private static RequestDefinition positiveDefinition() {
        return new RequestDefinition("S6-REQ-POS",
                request("/api/v1/s6/tenants/tenant-a/reports/report-common"),
                "ctx-user-a-tenant-a", "report:report-common");
    }

    private static RequestDefinition negativeDefinition() {
        return new RequestDefinition("S6-REQ-NEG",
                request("/api/v1/s6/tenants/tenant-b/reports/report-b"),
                "ctx-user-a-tenant-b", "report:report-b");
    }

    private static HttpRequest request(String path) {
        String token = token("user-a", "tenant-a", "viewer");
        return HttpRequest.of(HttpMethod.GET, "http", "localhost", SECURE_PORT, path,
                List.of(new HttpHeader("Authorization", "Bearer " + token),
                        new HttpHeader("X-S6-Role", "viewer"),
                        new HttpHeader("Accept", "application/json")),
                new byte[0], HttpProtocol.HTTP_1_1);
    }

    private static SecurityContextFingerprint sourceContext() {
        return new SecurityContextFingerprint("user-a", "viewer", "tenant-a", "report:report-common", "manager-a",
                "READ_REPORT", "ACTIVE", "GET /api/v1/s6/tenants/{tenant}/reports/{report}", "raw",
                "s6-user-a", AuthorizationDecision.ALLOW, AuthorizationDecision.UNKNOWN, List.of("e-policy"));
    }

    private static SecurityContextFingerprint targetContext() {
        return new SecurityContextFingerprint("user-a", "tenant-admin", "tenant-b", "report:report-common", "user-b",
                "READ_REPORT", "ACTIVE", "GET /api/v1/s6/tenants/{tenant}/reports/{report}", "raw",
                "s6-user-a", AuthorizationDecision.DENY, AuthorizationDecision.UNKNOWN, List.of("e-policy"));
    }

    private static Resource sourceResource() {
        return new Resource("report-common", "report", null, "manager-a", "tenant-a", "ACTIVE", Confidence.unknown());
    }

    private static Resource targetResource() {
        return new Resource("report-common", "report", null, "user-b", "tenant-b", "ACTIVE", Confidence.unknown());
    }

    private static Harness harness(Clock clock) {
        SafetyAuditLog audit = new SafetyAuditLog(clock);
        KillSwitch kill = new KillSwitch(audit);
        HierarchicalBudgetManager budgets = new HierarchicalBudgetManager(audit);
        HierarchicalConcurrencyController concurrency = new HierarchicalConcurrencyController();
        ScopedRateLimiter rate = new ScopedRateLimiter();
        MutationValidator validator = new MutationValidator(PROJECT, Set.of(ExecutionEnvironment.LAB),
                kill, budgets, concurrency, rate, audit);
        ExecutionEvidenceStore evidence = new ExecutionEvidenceStore(clock);
        DelayController noDelay = duration -> { };
        TestExecutor executor = new TestExecutor(clock, Duration.ofSeconds(2),
                new LocalhostHttpTransport(target()), noDelay, validator, budgets, concurrency, rate,
                new BackoffPolicy(0, Duration.ofMillis(25)), kill, new MutationBudgetTracker(10),
                audit, evidence);
        return new Harness(executor, kill, budgets, concurrency, rate);
    }

    private static void configureExecutionGuards(Harness harness, io.acra.core.active.model.SecurityTest test) {
        for (BudgetKey key : budgetKeys(test)) harness.budgets().configure(key, 50);
        for (ConcurrencyKey key : concurrencyKeys(test)) harness.concurrency().configure(key, 2);
        RateLimitPolicy policy = new RateLimitPolicy(1000, 5000, 1000, 0);
        for (String key : rateKeys(test)) harness.rate().configure(key, policy);
    }

    private static List<BudgetKey> budgetKeys(io.acra.core.active.model.SecurityTest test) {
        return List.of(
                new BudgetKey(BudgetScope.GLOBAL, "global"),
                new BudgetKey(BudgetScope.PROJECT, test.target().projectId()),
                new BudgetKey(BudgetScope.TARGET, test.target().targetId()),
                new BudgetKey(BudgetScope.ENDPOINT, test.endpoint().endpointId()),
                new BudgetKey(BudgetScope.TEST, test.testId()),
                new BudgetKey(BudgetScope.CONTEXT,
                        test.targetContext().principal() + '|' + test.targetContext().tenant() + '|'
                                + test.targetContext().role()));
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

    private static String token(String sub, String tenant, String role) {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String header = encoder.encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}"
                .getBytes(StandardCharsets.UTF_8));
        String payload = encoder.encodeToString(("{\"sub\":\"" + sub + "\",\"tenant_id\":\"" + tenant
                + "\",\"role\":\"" + role + "\"}").getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".s6synthetic";
    }

    private record Harness(
            TestExecutor executor,
            KillSwitch kill,
            HierarchicalBudgetManager budgets,
            HierarchicalConcurrencyController concurrency,
            ScopedRateLimiter rate) { }
}
