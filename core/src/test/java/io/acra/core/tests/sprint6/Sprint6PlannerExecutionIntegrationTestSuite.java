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
import io.acra.core.active.model.MutationLocation;
import io.acra.core.active.model.MutationType;
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

        EffectiveAuthorizationResolution tenantBaseline = resolver.resolve(policy,
                new EffectiveAuthorizationRequest("user-a", "tenant-a", "tenant-a", "report-common", "report",
                        "/api/v1/s6/tenants/tenant-a/reports/report-common", "", "READ_REPORT",
                        AuthorizationDecision.UNKNOWN, false, NOW));
        EffectiveAuthorizationResolution tenantTarget = resolver.resolve(policy,
                new EffectiveAuthorizationRequest("user-a", "tenant-a", "tenant-b", "report-common", "report",
                        "/api/v1/s6/tenants/tenant-b/reports/report-common", "", "READ_REPORT",
                        AuthorizationDecision.UNKNOWN, false, NOW));

        var tenantAugmentation = new S6PolicyPlanningBridge().augment(
                tenantPlanningInput(), List.of(tenantCandidate(tenantBaseline, tenantTarget)));
        TestSupport.assertEquals(1, tenantAugmentation.generatedSeeds().size(),
                "policy bridge should generate one safe CROSS_TENANT seed");
        assertions++;

        var tenantSeed = tenantAugmentation.generatedSeeds().getFirst();
        TestSupport.assertEquals(TestContract.CROSS_TENANT, tenantSeed.contract(),
                "generated tenant contract should be CROSS_TENANT");
        assertions++;
        TestSupport.assertEquals("tenant-a", tenantSeed.mutation().originalValue(),
                "tenant mutation should contain non-secret source tenant");
        assertions++;
        TestSupport.assertEquals("tenant-b", tenantSeed.mutation().mutatedValue(),
                "tenant mutation should contain non-secret target tenant");
        assertions++;

        var tenantResult = executeSingle(tenantAugmentation.planningInput(),
                AuthorizationDecision.DENY, tenantTarget, "tenant");
        TestSupport.assertEquals(AuthorizationOutcome.DENY,
                tenantResult.result().observation().observedDecision(),
                "generated tenant mutation should be denied by secure ACRA-Lab");
        assertions++;
        TestSupport.assertEquals(DifferentialClassification.EXPECTED_CHANGE,
                tenantResult.result().observation().differences().classification(),
                "generated tenant mutation should match expected policy change");
        assertions++;

        EffectiveAuthorizationResolution roleBaseline = resolver.resolve(policy,
                new EffectiveAuthorizationRequest("user-a", "tenant-a", "tenant-a", "admin-summary",
                        "admin-summary", "/api/v1/s6/tenants/tenant-a/admin/summary", "", "READ_ADMIN_SUMMARY",
                        AuthorizationDecision.UNKNOWN, false, NOW));
        EffectiveAuthorizationResolution roleTarget = resolver.resolve(policy,
                new EffectiveAuthorizationRequest("admin-a", "tenant-a", "tenant-a", "admin-summary",
                        "admin-summary", "/api/v1/s6/tenants/tenant-a/admin/summary", "", "READ_ADMIN_SUMMARY",
                        AuthorizationDecision.UNKNOWN, false, NOW));

        var roleAugmentation = new S6PolicyPlanningBridge().augment(
                rolePlanningInput(), List.of(roleCandidate(roleBaseline, roleTarget)));
        TestSupport.assertEquals(1, roleAugmentation.generatedSeeds().size(),
                "policy bridge should generate one credential-safe ROLE_COMPARISON seed");
        assertions++;
        TestSupport.assertTrue(roleAugmentation.skippedReasons().isEmpty(),
                "valid safe role comparison should not be skipped");
        assertions++;

        var roleSeed = roleAugmentation.generatedSeeds().getFirst();
        TestSupport.assertEquals(TestContract.ROLE_COMPARISON, roleSeed.contract(),
                "generated role contract should be ROLE_COMPARISON");
        assertions++;
        TestSupport.assertEquals(MutationType.AUTHENTICATED_CONTEXT_SUBSTITUTION,
                roleSeed.mutation().type(), "role comparison should use authenticated-context substitution");
        assertions++;
        TestSupport.assertEquals(MutationLocation.CONTEXT, roleSeed.mutation().targetLocation(),
                "role comparison mutation should reference CONTEXT rather than token/header value");
        assertions++;
        TestSupport.assertEquals("viewer", roleSeed.mutation().originalValue(),
                "mutation should store non-secret source role label");
        assertions++;
        TestSupport.assertEquals("admin", roleSeed.mutation().mutatedValue(),
                "mutation should store non-secret target role label");
        assertions++;
        TestSupport.assertEquals(roleBaselineRequest().contextRef(), roleSeed.mutation().sourceContext(),
                "mutation source should reference explicit viewer request context");
        assertions++;
        TestSupport.assertEquals(roleAdminRequest().contextRef(), roleSeed.mutation().targetContext(),
                "mutation target should reference explicit admin request context");
        assertions++;

        var roleResult = executeSingle(roleAugmentation.planningInput(),
                AuthorizationDecision.ALLOW, roleTarget, "role");
        TestSupport.assertEquals(AuthorizationOutcome.DENY,
                roleResult.result().observation().differences().baselineOutcome(),
                "viewer baseline should be denied privileged read");
        assertions++;
        TestSupport.assertEquals(AuthorizationOutcome.ALLOW,
                roleResult.result().observation().differences().positiveControlOutcome(),
                "admin positive control should allow privileged read");
        assertions++;
        TestSupport.assertEquals(AuthorizationOutcome.DENY,
                roleResult.result().observation().differences().negativeControlOutcome(),
                "viewer negative control should deny privileged read");
        assertions++;
        TestSupport.assertEquals(AuthorizationOutcome.ALLOW,
                roleResult.result().observation().observedDecision(),
                "credential-safe context substitution should execute with admin context");
        assertions++;
        TestSupport.assertEquals(DifferentialClassification.EXPECTED_CHANGE,
                roleResult.result().observation().differences().classification(),
                "role-context substitution should produce expected policy change");
        assertions++;

        String viewerToken = token("user-a", "tenant-a", "viewer");
        String adminToken = token("admin-a", "tenant-a", "admin");
        String serializedRoleTest = new DomainSerializer().serialize(roleResult.test());
        TestSupport.assertNotContains(serializedRoleTest, viewerToken,
                "serialized role test must not expose viewer credential");
        assertions++;
        TestSupport.assertNotContains(serializedRoleTest, adminToken,
                "serialized role test must not expose admin credential");
        assertions++;
        TestSupport.assertFalse(roleSeed.mutation().originalValue().contains(viewerToken)
                        || roleSeed.mutation().mutatedValue().contains(adminToken)
                        || roleSeed.mutation().sourceContext().contains(viewerToken)
                        || roleSeed.mutation().targetContext().contains(adminToken),
                "Mutation must contain references/labels only, never raw authentication material");
        assertions++;

        System.out.println("SPRINT6_AUTO_ROLE testId=" + roleResult.test().testId()
                + " sourceContextRef=" + roleSeed.mutation().sourceContext()
                + " targetContextRef=" + roleSeed.mutation().targetContext()
                + " expected=" + roleResult.test().expectedDecision()
                + " observed=" + roleResult.result().observation().observedDecision()
                + " differential=" + roleResult.result().observation().differences().classification()
                + " queueState=" + roleResult.workspace().queue().snapshot(roleResult.test().testId()).state());

        return assertions;
    }

    private static ExecutionResult executeSingle(PlanningInput input, AuthorizationDecision expectedDecision,
                                                 EffectiveAuthorizationResolution resolution, String label) {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        Harness harness = harness(clock);
        ActiveEngineWorkspace workspace = new ActiveEngineWorkspace(clock, harness.executor());
        var planning = workspace.plan(input);
        TestSupport.assertEquals(1, planning.plan().tests().size(),
                label + " planning should admit exactly one generated test");
        var test = planning.plan().tests().getFirst();
        configureExecutionGuards(harness, test);
        harness.kill().reset(true, "authorized Sprint 6 localhost " + label + " generated-test execution");
        ExpectedDecisionCandidate expected = new ExpectedDecisionCandidate(
                expectedDecision, ExpectedDecisionSource.EXPLICIT_CONFIGURED_POLICY,
                "s6-auto-policy", resolution.evidenceIds(), 1.0);
        var result = workspace.executeNextLocal(List.of(expected)).orElseThrow();
        TestSupport.assertEquals(io.acra.core.domain.testing.TestState.COMPLETED, result.state(),
                label + " generated test should execute through existing S4 executor");
        TestSupport.assertEquals(io.acra.core.domain.testing.TestState.COMPLETED,
                workspace.queue().snapshot(test.testId()).state(),
                label + " generated queue item should complete");
        return new ExecutionResult(test, result, workspace);
    }

    private static S6PolicyPlanningCandidate tenantCandidate(EffectiveAuthorizationResolution baseline,
                                                              EffectiveAuthorizationResolution target) {
        return new S6PolicyPlanningCandidate(
                "S6-CANDIDATE-CROSS-TENANT",
                tenantEndpoint(),
                tenantBaselineRequest(),
                tenantPositiveRequest(),
                tenantNegativeRequest(),
                tenantSourceContext(),
                tenantTargetContext(),
                tenantSourceResource(),
                tenantTargetResource(),
                baseline,
                target,
                Set.of(TestContract.CROSS_TENANT),
                List.of(),
                List.of("principal remains user-a", "method remains GET", "resource identifier remains report-common"),
                false,
                false);
    }

    private static S6PolicyPlanningCandidate roleCandidate(EffectiveAuthorizationResolution baseline,
                                                            EffectiveAuthorizationResolution target) {
        return new S6PolicyPlanningCandidate(
                "S6-CANDIDATE-ROLE",
                roleEndpoint(),
                roleBaselineRequest(),
                roleAdminRequest(),
                roleViewerNegativeRequest(),
                roleSourceContext(),
                roleTargetContext(),
                roleResource(),
                roleResource(),
                baseline,
                target,
                Set.of(TestContract.ROLE_COMPARISON),
                List.of(),
                List.of("tenant remains tenant-a", "resource remains admin-summary", "method remains GET"),
                false,
                false);
    }

    private static PlanningInput tenantPlanningInput() {
        return planningInput("PLAN-S6-TENANT-AUTO", tenantEndpoint(),
                List.of(tenantSourceContext(), tenantTargetContext()), Set.of(TestContract.CROSS_TENANT));
    }

    private static PlanningInput rolePlanningInput() {
        return planningInput("PLAN-S6-ROLE-AUTO", roleEndpoint(),
                List.of(roleSourceContext(), roleTargetContext()), Set.of(TestContract.ROLE_COMPARISON));
    }

    private static PlanningInput planningInput(String planId, Endpoint endpoint,
                                               List<SecurityContextFingerprint> contexts,
                                               Set<TestContract> contracts) {
        ApiEndpointRecord inventory = new ApiEndpointRecord(endpoint, "v1", "LAB", AuthenticationType.BEARER,
                "ACRA-Lab", NOW, NOW, DocumentationStatus.DOCUMENTED, RiskTier.HIGH);
        return new PlanningInput(planId, List.of(inventory), new SecurityContextGraph(), new AuthorizationMatrix(),
                contexts, List.of(), null, target(), contracts, Map.of(), Map.of(), 20, 5,
                SafetyPolicy.safeLabReadOnly(20, 5), SelectionMode.AUTOMATIC,
                TestProfileDefinition.defaults(TestProfile.SAFE_LAB),
                ConfigurationSnapshot.of(Map.of("dataset", "S6-AUTO-PLANNING", "environment", "LAB",
                        "target", "localhost:18082")), List.of(), NOW);
    }

    private static AuthorizationPolicySnapshot policy() {
        return AuthorizationPolicySnapshot.create(
                "s6-auto-policy", "1", "controlled-local-lab", List.of(),
                List.of(
                        new RoleAssignment("ra-viewer", "user-a", "viewer", "tenant-a",
                                AuthorizationScope.tenant("tenant-a"), true, List.of("e-role-viewer")),
                        new RoleAssignment("ra-admin", "admin-a", "admin", "tenant-a",
                                AuthorizationScope.tenant("tenant-a"), true, List.of("e-role-admin"))),
                List.of(),
                List.of(
                        new Permission("p-read-a", "READ_REPORT", "report", "", "",
                                AuthorizationScope.tenant("tenant-a"), List.of("e-permission-read")),
                        new Permission("p-admin-summary", "READ_ADMIN_SUMMARY", "admin-summary", "", "",
                                AuthorizationScope.tenant("tenant-a"), List.of("e-permission-admin"))),
                List.of(
                        new RolePermissionAssignment("rp-read-a", "viewer", "p-read-a", "tenant-a",
                                List.of("e-rp-read")),
                        new RolePermissionAssignment("rp-admin-summary", "admin", "p-admin-summary", "tenant-a",
                                List.of("e-rp-admin"))),
                List.of(), List.of(), List.of("e-policy"), AuthorizationDecision.DENY, NOW);
    }

    private static Endpoint tenantEndpoint() {
        return new Endpoint("EP-S6-AUTO-TENANT", HttpMethod.GET,
                "/api/v1/s6/tenants/tenant-a/reports/report-common",
                "/api/v1/s6/tenants/{tenant}/reports/report-common",
                "/api/v1/s6/tenants/{tenant}/reports/{report}",
                "localhost", "v1", List.of("Sprint 6 policy-generated tenant test"));
    }

    private static Endpoint roleEndpoint() {
        return new Endpoint("EP-S6-AUTO-ROLE", HttpMethod.GET,
                "/api/v1/s6/tenants/tenant-a/admin/summary",
                "/api/v1/s6/tenants/{tenant}/admin/summary",
                "/api/v1/s6/tenants/{tenant}/admin/summary",
                "localhost", "v1", List.of("Sprint 6 credential-safe role comparison"));
    }

    private static TargetDescriptor target() {
        return new TargetDescriptor(PROJECT, "acra-lab-secure-s6", "http", "localhost", SECURE_PORT,
                ExecutionEnvironment.LAB, true, List.of("/api/v1/s6"), Set.of(HttpMethod.GET));
    }

    private static RequestDefinition tenantBaselineRequest() {
        return requestDefinition("S6-REQ-TENANT-BASE", "/api/v1/s6/tenants/tenant-a/reports/report-common",
                "user-a", "tenant-a", "viewer", "ctx-user-a-tenant-a", "report:report-common");
    }

    private static RequestDefinition tenantPositiveRequest() {
        return requestDefinition("S6-REQ-TENANT-POS", "/api/v1/s6/tenants/tenant-a/reports/report-common",
                "user-a", "tenant-a", "viewer", "ctx-user-a-tenant-a-pos", "report:report-common");
    }

    private static RequestDefinition tenantNegativeRequest() {
        return requestDefinition("S6-REQ-TENANT-NEG", "/api/v1/s6/tenants/tenant-b/reports/report-b",
                "user-a", "tenant-a", "viewer", "ctx-user-a-tenant-b-deny", "report:report-b");
    }

    private static RequestDefinition roleBaselineRequest() {
        return requestDefinition("S6-REQ-ROLE-BASE", "/api/v1/s6/tenants/tenant-a/admin/summary",
                "user-a", "tenant-a", "viewer", "ctx-role-viewer-a", "admin-summary:tenant-a");
    }

    private static RequestDefinition roleAdminRequest() {
        return requestDefinition("S6-REQ-ROLE-ADMIN", "/api/v1/s6/tenants/tenant-a/admin/summary",
                "admin-a", "tenant-a", "admin", "ctx-role-admin-a", "admin-summary:tenant-a");
    }

    private static RequestDefinition roleViewerNegativeRequest() {
        return requestDefinition("S6-REQ-ROLE-NEG", "/api/v1/s6/tenants/tenant-a/admin/summary",
                "user-a", "tenant-a", "viewer", "ctx-role-viewer-neg-a", "admin-summary:tenant-a");
    }

    private static RequestDefinition requestDefinition(String id, String path, String principal, String tenant,
                                                       String role, String contextRef, String resourceRef) {
        String rawToken = token(principal, tenant, role);
        HttpRequest request = HttpRequest.of(HttpMethod.GET, "http", "localhost", SECURE_PORT, path,
                List.of(new HttpHeader("Authorization", "Bearer " + rawToken),
                        new HttpHeader("Accept", "application/json")),
                new byte[0], HttpProtocol.HTTP_1_1);
        return new RequestDefinition(id, request, contextRef, resourceRef);
    }

    private static SecurityContextFingerprint tenantSourceContext() {
        return new SecurityContextFingerprint("user-a", "viewer", "tenant-a", "report:report-common", "manager-a",
                "READ_REPORT", "ACTIVE", "GET /api/v1/s6/tenants/{tenant}/reports/{report}", "raw",
                "s6-user-a", AuthorizationDecision.ALLOW, AuthorizationDecision.UNKNOWN, List.of("e-policy"));
    }

    private static SecurityContextFingerprint tenantTargetContext() {
        return new SecurityContextFingerprint("user-a", "viewer", "tenant-b", "report:report-common", "user-b",
                "READ_REPORT", "ACTIVE", "GET /api/v1/s6/tenants/{tenant}/reports/{report}", "raw",
                "s6-user-a", AuthorizationDecision.DENY, AuthorizationDecision.UNKNOWN, List.of("e-policy"));
    }

    private static SecurityContextFingerprint roleSourceContext() {
        return new SecurityContextFingerprint("user-a", "viewer", "tenant-a", "admin-summary:tenant-a", "",
                "READ_ADMIN_SUMMARY", "ACTIVE", "GET /api/v1/s6/tenants/{tenant}/admin/summary", "raw",
                "s6-viewer-a", AuthorizationDecision.DENY, AuthorizationDecision.UNKNOWN, List.of("e-policy"));
    }

    private static SecurityContextFingerprint roleTargetContext() {
        return new SecurityContextFingerprint("admin-a", "admin", "tenant-a", "admin-summary:tenant-a", "",
                "READ_ADMIN_SUMMARY", "ACTIVE", "GET /api/v1/s6/tenants/{tenant}/admin/summary", "raw",
                "s6-admin-a", AuthorizationDecision.ALLOW, AuthorizationDecision.UNKNOWN, List.of("e-policy"));
    }

    private static Resource tenantSourceResource() {
        return new Resource("report-common", "report", null, "manager-a", "tenant-a", "ACTIVE", Confidence.unknown());
    }

    private static Resource tenantTargetResource() {
        return new Resource("report-common", "report", null, "user-b", "tenant-b", "ACTIVE", Confidence.unknown());
    }

    private static Resource roleResource() {
        return new Resource("admin-summary", "admin-summary", null, "", "tenant-a", "ACTIVE", Confidence.unknown());
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
                new BackoffPolicy(0, Duration.ofMillis(25)), kill, new MutationBudgetTracker(20),
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

    private record Harness(TestExecutor executor, KillSwitch kill, HierarchicalBudgetManager budgets,
                           HierarchicalConcurrencyController concurrency, ScopedRateLimiter rate) { }

    private record ExecutionResult(io.acra.core.active.model.SecurityTest test,
                                   io.acra.core.active.execution.TestExecutionResult result,
                                   ActiveEngineWorkspace workspace) { }
}
