package io.acra.core.tests.sprint9;

import io.acra.core.active.analysis.AuthorizationOutcome;
import io.acra.core.active.analysis.DifferentialClassification;
import io.acra.core.active.analysis.ExpectedDecisionCandidate;
import io.acra.core.active.analysis.ExpectedDecisionSource;
import io.acra.core.active.evidence.EvidenceChainEntry;
import io.acra.core.active.evidence.EvidenceStage;
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
import io.acra.core.domain.authorization.Action;
import io.acra.core.domain.authorization.ActionType;
import io.acra.core.domain.authorization.AuthorizationContext;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.ContextStatus;
import io.acra.core.domain.authorization.PolicyValidationState;
import io.acra.core.domain.common.Confidence;
import io.acra.core.domain.endpoint.ApiEndpointRecord;
import io.acra.core.domain.endpoint.DocumentationStatus;
import io.acra.core.domain.endpoint.Endpoint;
import io.acra.core.domain.endpoint.RiskTier;
import io.acra.core.domain.evidence.EvidenceSource;
import io.acra.core.domain.http.HttpHeader;
import io.acra.core.domain.http.HttpMethod;
import io.acra.core.domain.http.HttpProtocol;
import io.acra.core.domain.http.HttpRequest;
import io.acra.core.domain.identity.AuthenticationType;
import io.acra.core.domain.identity.Principal;
import io.acra.core.domain.identity.Role;
import io.acra.core.domain.resource.Resource;
import io.acra.core.domain.tenant.Tenant;
import io.acra.core.domain.workflow.WorkflowState;
import io.acra.core.engine.PolicyValidationEvaluator;
import io.acra.core.graph.SecurityContextGraph;
import io.acra.core.property.PropertyAccessObservation;
import io.acra.core.property.S9PropertyAuthorizationAnalysis;
import io.acra.core.property.S9PropertyAuthorizationAnalyzer;
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

public final class Sprint9ControlledPropertyExecutionTestSuite {
    private static final String PROJECT = "acra-s9";
    private static final String ENDPOINT_PATH = "/api/v1/s9/users/user-a/profile";
    private static final Instant NOW = Instant.parse("2026-09-24T10:20:00Z");

    private Sprint9ControlledPropertyExecutionTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT9_CONTROLLED_PROPERTY_EXECUTION PASS assertions=" + assertions);
    }

    public static int run() {
        int assertions = 0;

        Mutation mutation = mutation();
        HttpRequest built = new RequestBuilder().mutate(baseline(18082).request(), mutation);
        TestSupport.assertEquals("{\"is_admin\":true}",
                new String(built.body(), StandardCharsets.UTF_8),
                "declared property mutation must change exactly the controlled request body");
        assertions++;
        TestSupport.assertEquals(HttpMethod.PATCH, built.method(),
                "property mutation must preserve PATCH method");
        assertions++;
        TestSupport.assertEquals(ENDPOINT_PATH, built.rawTarget(),
                "property mutation must preserve endpoint");
        assertions++;

        ExecutionResult secure = execute(18082, "secure");
        TestSupport.assertEquals(io.acra.core.domain.testing.TestState.COMPLETED, secure.result().state(),
                "secure controlled property test must complete through existing active engine");
        assertions++;
        TestSupport.assertTrue(secure.result().observation() != null,
                "secure controlled execution must produce an observation");
        assertions++;
        TestSupport.assertEquals(AuthorizationOutcome.DENY,
                secure.result().observation().observedDecision(),
                "secure fixture must deny privileged property update");
        assertions++;
        TestSupport.assertEquals(DifferentialClassification.EXPECTED_CHANGE,
                secure.result().observation().differences().classification(),
                "secure property denial must match explicit DENY expectation");
        assertions++;

        S9PropertyAuthorizationAnalysis secureAnalysis = propertyAnalysis(secure, PROJECT);
        TestSupport.assertEquals(0L, secureAnalysis.candidateCount(),
                "secure property denial must not produce a property violation candidate");
        assertions++;
        TestSupport.assertEquals(PolicyValidationState.DENIED,
                secureAnalysis.assessments().getFirst().state(),
                "secure property assessment remains explicitly denied");
        assertions++;

        ExecutionResult vulnerable = execute(18081, "vulnerable");
        TestSupport.assertEquals(io.acra.core.domain.testing.TestState.COMPLETED, vulnerable.result().state(),
                "vulnerable controlled property test must complete through existing active engine");
        assertions++;
        TestSupport.assertTrue(vulnerable.result().observation() != null,
                "vulnerable controlled execution must produce an observation");
        assertions++;
        TestSupport.assertEquals(AuthorizationOutcome.ALLOW,
                vulnerable.result().observation().observedDecision(),
                "deliberately vulnerable fixture must allow privileged property update");
        assertions++;
        TestSupport.assertEquals(DifferentialClassification.UNEXPECTED_CHANGE,
                vulnerable.result().observation().differences().classification(),
                "expected DENY plus observed ALLOW must remain an unexpected differential");
        assertions++;

        S9PropertyAuthorizationAnalysis vulnerableAnalysis = propertyAnalysis(vulnerable, PROJECT);
        TestSupport.assertEquals(1L, vulnerableAnalysis.candidateCount(),
                "provenance-verified DENY-to-ALLOW property mismatch becomes one review candidate");
        assertions++;
        TestSupport.assertTrue(vulnerableAnalysis.assessments().getFirst().violationCandidate(),
                "controlled vulnerable property assessment must retain review-only candidate semantics");
        assertions++;

        S9PropertyAuthorizationAnalysis crossProject = propertyAnalysis(vulnerable, "other-project");
        TestSupport.assertEquals(1L, crossProject.inconclusiveCount(),
                "cross-project live evidence must fail closed");
        assertions++;
        TestSupport.assertEquals(0L, crossProject.candidateCount(),
                "cross-project live evidence cannot produce a property candidate");
        assertions++;

        TestSupport.assertEquals(TestContract.PROPERTY, vulnerable.test().category(),
                "controlled active test remains PROPERTY contract");
        assertions++;
        TestSupport.assertEquals(MutationType.PROPERTY, vulnerable.test().mutation().type(),
                "controlled active test uses existing PROPERTY mutation family");
        assertions++;
        TestSupport.assertEquals(MutationLocation.BODY, vulnerable.test().mutation().targetLocation(),
                "controlled active test changes only the request body");
        assertions++;
        TestSupport.assertEquals(SafetyClass.STATE_CHANGING, vulnerable.test().mutation().safetyClass(),
                "privileged property update is explicitly classified state-changing");
        assertions++;

        String rawToken = token("user-a", "tenant-a", "viewer");
        String serialized = new io.acra.core.serialization.DomainSerializer().serialize(vulnerable.test());
        TestSupport.assertNotContains(serialized, rawToken,
                "serialized property test must not expose raw bearer token");
        assertions++;

        TestSupport.assertTrue(vulnerable.evidence().chain(vulnerable.result().executionId()).stream()
                        .anyMatch(entry -> entry.stage() == EvidenceStage.MUTATION_REQUEST),
                "active property execution must retain mutation-request evidence");
        assertions++;
        TestSupport.assertTrue(vulnerable.evidence().chain(vulnerable.result().executionId()).stream()
                        .anyMatch(entry -> entry.stage() == EvidenceStage.MUTATION_RESPONSE),
                "active property execution must retain mutation-response evidence");
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

    private static S9PropertyAuthorizationAnalysis propertyAnalysis(ExecutionResult execution, String projectId) {
        List<String> evidenceObjectIds = evidenceObjectIds(execution);
        AuthorizationDecision observed = toDecision(execution.result().observation().observedDecision());

        PropertyAccessObservation propertyObservation = new PropertyAccessObservation(
                execution.result().observation().observationId(),
                execution.result().executionId(),
                execution.test().testId(),
                ENDPOINT_PATH,
                "is_admin",
                PolicyValidationEvaluator.PropertyOperation.UPDATE,
                observed,
                evidenceObjectIds);

        PolicyValidationEvaluator.PropertyPolicy policy = new PolicyValidationEvaluator.PropertyPolicy(
                "s9-policy-is-admin-update",
                "GT-S9-PROPERTY-AUTHORIZATION",
                ENDPOINT_PATH,
                "is_admin",
                PolicyValidationEvaluator.PropertyOperation.UPDATE,
                "viewer",
                "tenant-a",
                AuthorizationDecision.DENY,
                evidenceObjectIds);

        return new S9PropertyAuthorizationAnalyzer(execution.evidence()).analyze(
                context(), List.of(policy), List.of(propertyObservation), projectId);
    }

    private static AuthorizationDecision toDecision(AuthorizationOutcome outcome) {
        return switch (outcome) {
            case ALLOW -> AuthorizationDecision.ALLOW;
            case DENY, SOFT_DENY -> AuthorizationDecision.DENY;
            case ERROR -> AuthorizationDecision.ERROR;
            case PARTIAL -> AuthorizationDecision.CONDITIONAL;
            case UNKNOWN -> AuthorizationDecision.UNKNOWN;
        };
    }

    private static List<String> evidenceObjectIds(ExecutionResult execution) {
        return execution.evidence().chain(execution.result().executionId()).stream()
                .filter(entry -> entry.stage() != EvidenceStage.OBSERVATION)
                .map(EvidenceChainEntry::objectId)
                .toList();
    }

    private static ExecutionResult execute(int port, String label) {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        Harness harness = harness(clock, port);
        ActiveEngineWorkspace workspace = new ActiveEngineWorkspace(clock, harness.executor());
        workspace.configureMode(UserMode.RESEARCHER);
        workspace.configureProfile(TestProfile.RESEARCH_EXPERIMENT);

        var planning = workspace.plan(planningInput(port));
        TestSupport.assertEquals(1, planning.plan().tests().size(),
                label + " property plan must contain exactly one explicit controlled property test");
        var test = planning.plan().tests().getFirst();

        configureExecutionGuards(harness, test);
        harness.kill().reset(true, "authorized Sprint 9 localhost " + label + " property execution");

        ExpectedDecisionCandidate expected = new ExpectedDecisionCandidate(
                AuthorizationDecision.DENY,
                ExpectedDecisionSource.EXPLICIT_CONFIGURED_POLICY,
                "s9-policy-is-admin-update",
                List.of("GT-S9-PROPERTY-AUTHORIZATION"),
                1.0);

        var result = workspace.executeNextLocal(List.of(expected)).orElseThrow();
        return new ExecutionResult(test, result, harness.evidence());
    }

    private static PlanningInput planningInput(int port) {
        Endpoint endpoint = endpoint();
        ApiEndpointRecord inventory = new ApiEndpointRecord(
                endpoint,
                "v1",
                "LAB",
                AuthenticationType.BEARER,
                "ACRA-Lab",
                NOW,
                NOW,
                DocumentationStatus.DOCUMENTED,
                RiskTier.HIGH);

        SafetyPolicy safety = new SafetyPolicy(
                SafetyClass.STATE_CHANGING,
                true,
                true,
                20,
                5,
                2 * 1024 * 1024,
                Set.of(HttpMethod.PATCH));

        return new PlanningInput(
                "PLAN-S9-PROPERTY-" + port,
                List.of(inventory),
                new SecurityContextGraph(),
                new io.acra.core.domain.authorization.AuthorizationMatrix(),
                List.of(contextFingerprint()),
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
                        "dataset", "GT-S9-PROPERTY-AUTHORIZATION",
                        "environment", "LAB",
                        "target", "localhost:" + port)),
                List.of(seed(port)),
                NOW);
    }

    private static TestSeed seed(int port) {
        return new TestSeed(
                "S9-AUTO-PROPERTY-UPDATE",
                "1",
                TestContract.PROPERTY,
                endpoint(),
                baseline(port),
                positive(port),
                negative(port),
                mutation(),
                contextFingerprint(),
                contextFingerprint(),
                null,
                null,
                AuthorizationDecision.DENY,
                List.of("GT-S9-PROPERTY-AUTHORIZATION"),
                List.of(),
                List.of(
                        "method remains PATCH",
                        "authority remains localhost",
                        "authenticated context remains user-a/viewer/tenant-a",
                        "endpoint remains fixed",
                        "only the explicit request body property changes"),
                4,
                true,
                false,
                false,
                "controlled explicit property-authorization validation");
    }

    private static Mutation mutation() {
        return new Mutation(
                "S9-MUT-PROPERTY-IS-ADMIN",
                MutationType.PROPERTY,
                MutationLocation.BODY,
                "{\"display_name\":\"User A\"}",
                "{\"is_admin\":true}",
                "user-a-property-context",
                "user-a-property-context",
                "replace explicitly allowed profile field with explicitly denied privileged field",
                "authorization decision for is_admin UPDATE must remain DENY",
                SafetyClass.STATE_CHANGING,
                "s9-property-is-admin-update");
    }

    private static Endpoint endpoint() {
        return new Endpoint(
                "EP-S9-PROPERTY-PROFILE",
                HttpMethod.PATCH,
                ENDPOINT_PATH,
                "/api/v1/s9/users/{user}/profile",
                "/api/v1/s9/users/{user}/profile",
                "localhost",
                "v1",
                List.of("Sprint 9 controlled property-authorization fixture"));
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
                List.of("/api/v1/s9"),
                Set.of(HttpMethod.PATCH));
    }

    private static RequestDefinition baseline(int port) {
        return definition(port, "S9-REQ-BASE", true);
    }

    private static RequestDefinition positive(int port) {
        return definition(port, "S9-REQ-POS", true);
    }

    private static RequestDefinition negative(int port) {
        return definition(port, "S9-REQ-NEG", false);
    }

    private static RequestDefinition definition(int port, String id, boolean authenticated) {
        List<HttpHeader> headers = authenticated
                ? List.of(
                        new HttpHeader("Authorization", "Bearer " + token("user-a", "tenant-a", "viewer")),
                        new HttpHeader("Accept", "application/json"),
                        new HttpHeader("Content-Type", "application/json"))
                : List.of(
                        new HttpHeader("Accept", "application/json"),
                        new HttpHeader("Content-Type", "application/json"));

        HttpRequest request = HttpRequest.of(
                HttpMethod.PATCH,
                "http",
                "localhost",
                port,
                ENDPOINT_PATH,
                headers,
                "{\"display_name\":\"User A\"}".getBytes(StandardCharsets.UTF_8),
                HttpProtocol.HTTP_1_1);

        return new RequestDefinition(
                id,
                request,
                authenticated ? "user-a-property-context" : "anonymous-property-context",
                "profile:user-a");
    }

    private static SecurityContextFingerprint contextFingerprint() {
        return new SecurityContextFingerprint(
                "user-a",
                "viewer",
                "tenant-a",
                "user-a",
                "user-a",
                "PROFILE_UPDATE",
                "ACTIVE",
                "PATCH /api/v1/s9/users/{user}/profile",
                "RAW",
                "user-a-property-context",
                AuthorizationDecision.DENY,
                AuthorizationDecision.UNKNOWN,
                List.of("GT-S9-PROPERTY-AUTHORIZATION"));
    }

    private static AuthorizationContext context() {
        Principal principal = new Principal(
                "user-a", "User A", AuthenticationType.UNKNOWN, Confidence.unknown());
        Role role = new Role("viewer", "viewer", EvidenceSource.UNKNOWN, Confidence.unknown());
        Tenant tenant = new Tenant("tenant-a", "Tenant A", EvidenceSource.UNKNOWN, Confidence.unknown());
        Resource resource = new Resource(
                "user-a",
                "user-profile",
                null,
                "user-a",
                "tenant-a",
                "ACTIVE",
                Confidence.unknown());
        Action action = new Action(
                ActionType.UPDATE, EvidenceSource.UNKNOWN, Confidence.unknown(), "PROFILE_UPDATE");

        return new AuthorizationContext(
                principal,
                role,
                tenant,
                resource,
                "user-a",
                action,
                new WorkflowState("ACTIVE", Confidence.unknown()),
                AuthorizationDecision.ALLOW,
                AuthorizationDecision.ALLOW,
                List.of(),
                ContextStatus.RESOLVED);
    }

    private static Harness harness(Clock clock, int port) {
        SafetyAuditLog audit = new SafetyAuditLog(clock);
        KillSwitch kill = new KillSwitch(audit);
        HierarchicalBudgetManager budgets = new HierarchicalBudgetManager(audit);
        HierarchicalConcurrencyController concurrency = new HierarchicalConcurrencyController();
        ScopedRateLimiter rate = new ScopedRateLimiter();
        MutationValidator validator = new MutationValidator(
                PROJECT,
                Set.of(ExecutionEnvironment.LAB),
                kill,
                budgets,
                concurrency,
                rate,
                audit);
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
                new BudgetKey(
                        BudgetScope.CONTEXT,
                        test.targetContext().principal() + '|'
                                + test.targetContext().tenant() + '|'
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

    private record ExecutionResult(
            io.acra.core.active.model.SecurityTest test,
            io.acra.core.active.execution.TestExecutionResult result,
            ExecutionEvidenceStore evidence) { }
}
