package io.acra.core.tests.sprint10;

import io.acra.core.active.analysis.AuthorizationOutcome;
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
import io.acra.core.batch.BatchItemObservation;
import io.acra.core.batch.BatchItemPolicy;
import io.acra.core.batch.BatchItemProjectionSpec;
import io.acra.core.batch.BatchItemResponseProjector;
import io.acra.core.batch.S10BatchAuthorizationAnalysis;
import io.acra.core.batch.S10BatchAuthorizationAnalyzer;
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

public final class Sprint10ControlledBatchExecutionTestSuite {
    private static final String PROJECT = "acra-s10";
    private static final String ENDPOINT_PATH = "/api/v1/s10/documents/batch-read";
    private static final Instant NOW = Instant.parse("2026-09-24T13:40:00Z");
    private static final String BASE_BODY = "{\"resource_ids\":[\"resource-a\"]}";
    private static final String MIXED_BODY = "{\"resource_ids\":[\"resource-a\",\"resource-b\"]}";

    private Sprint10ControlledBatchExecutionTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT10_CONTROLLED_BATCH_EXECUTION PASS assertions=" + assertions);
    }

    public static int run() {
        int assertions = 0;

        HttpRequest built = new RequestBuilder().mutate(baseline(18082).request(), mutation());
        TestSupport.assertEquals(MIXED_BODY, new String(built.body(), StandardCharsets.UTF_8),
                "declared BATCH mutation changes only the explicit resource list");
        assertions++;
        TestSupport.assertEquals(HttpMethod.POST, built.method(),
                "batch mutation preserves POST method");
        assertions++;
        TestSupport.assertEquals(ENDPOINT_PATH, built.rawTarget(),
                "batch mutation preserves endpoint");
        assertions++;

        ExecutionResult secure = execute(18082, "secure");
        TestSupport.assertEquals(io.acra.core.domain.testing.TestState.COMPLETED, secure.result().state(),
                "secure batch test completes through existing active engine");
        assertions++;
        TestSupport.assertEquals(AuthorizationOutcome.ALLOW,
                secure.result().observation().observedDecision(),
                "aggregate secure batch HTTP response remains ALLOW/200");
        assertions++;

        S10BatchAuthorizationAnalysis secureAnalysis = batchAnalysis(secure, PROJECT);
        TestSupport.assertEquals(2, secureAnalysis.assessments().size(),
                "secure active response projects two explicit item decisions");
        assertions++;
        TestSupport.assertEquals(0L, secureAnalysis.candidateCount(),
                "secure mixed batch has no item-level authorization candidate");
        assertions++;
        TestSupport.assertTrue(secureAnalysis.mixedObservedDecisions(),
                "secure mixed batch preserves ALLOW and DENY item outcomes despite aggregate ALLOW");
        assertions++;
        TestSupport.assertEquals(PolicyValidationState.ALLOWED,
                secureAnalysis.assessments().get(0).state(),
                "owned item remains allowed");
        assertions++;
        TestSupport.assertEquals(PolicyValidationState.DENIED,
                secureAnalysis.assessments().get(1).state(),
                "foreign item remains denied");
        assertions++;

        ExecutionResult vulnerable = execute(18081, "vulnerable");
        TestSupport.assertEquals(io.acra.core.domain.testing.TestState.COMPLETED, vulnerable.result().state(),
                "vulnerable batch test completes through existing active engine");
        assertions++;
        TestSupport.assertEquals(AuthorizationOutcome.ALLOW,
                vulnerable.result().observation().observedDecision(),
                "aggregate vulnerable batch HTTP response is also ALLOW/200");
        assertions++;

        S10BatchAuthorizationAnalysis vulnerableAnalysis = batchAnalysis(vulnerable, PROJECT);
        TestSupport.assertEquals(1L, vulnerableAnalysis.candidateCount(),
                "per-item analysis exposes one foreign-item DENY-to-ALLOW mismatch");
        assertions++;
        TestSupport.assertTrue(vulnerableAnalysis.assessments().get(1).violationCandidate(),
                "foreign batch item becomes review-only authorization candidate");
        assertions++;
        TestSupport.assertTrue(!vulnerableAnalysis.mixedObservedDecisions(),
                "vulnerable shortcut produces ALLOW for both items");
        assertions++;

        S10BatchAuthorizationAnalysis crossProject = batchAnalysis(vulnerable, "other-project");
        TestSupport.assertEquals(2L, crossProject.inconclusiveCount(),
                "cross-project active batch evidence fails closed for every item");
        assertions++;
        TestSupport.assertEquals(0L, crossProject.candidateCount(),
                "cross-project batch evidence cannot produce a candidate");
        assertions++;

        TestSupport.assertEquals(TestContract.BATCH, vulnerable.test().category(),
                "active batch test retains BATCH contract");
        assertions++;
        TestSupport.assertEquals(MutationType.BATCH, vulnerable.test().mutation().type(),
                "active batch test uses existing BATCH mutation family");
        assertions++;
        TestSupport.assertEquals(MutationLocation.BODY, vulnerable.test().mutation().targetLocation(),
                "active batch test changes only the request body");
        assertions++;
        TestSupport.assertEquals(SafetyClass.SAFE_READ_ONLY, vulnerable.test().mutation().safetyClass(),
                "non-persistent batch-read mutation remains explicitly safe read-only");
        assertions++;
        TestSupport.assertContains(vulnerable.result().observation().mutation().response().bodyUtf8(),
                "\"persisted\":false",
                "active batch fixture confirms non-persistent execution");
        assertions++;

        String rawToken = token("user-a", "tenant-a", "viewer");
        TestSupport.assertNotContains(
                new io.acra.core.serialization.DomainSerializer().serialize(vulnerable.test()),
                rawToken,
                "serialized batch test excludes raw bearer token");
        assertions++;

        TestSupport.assertTrue(vulnerable.evidence().chain(vulnerable.result().executionId()).stream()
                        .anyMatch(entry -> entry.stage() == EvidenceStage.MUTATION_REQUEST),
                "batch execution retains mutation-request evidence");
        assertions++;
        TestSupport.assertTrue(vulnerable.evidence().chain(vulnerable.result().executionId()).stream()
                        .anyMatch(entry -> entry.stage() == EvidenceStage.MUTATION_RESPONSE),
                "batch execution retains mutation-response evidence");
        assertions++;

        System.out.println("SPRINT10_BATCH_SECURE expectedAggregate=ALLOW observedAggregate="
                + secure.result().observation().observedDecision()
                + " itemCandidates=" + secureAnalysis.candidateCount());
        System.out.println("SPRINT10_BATCH_VULNERABLE expectedAggregate=ALLOW observedAggregate="
                + vulnerable.result().observation().observedDecision()
                + " itemCandidates=" + vulnerableAnalysis.candidateCount());
        return assertions;
    }

    private static S10BatchAuthorizationAnalysis batchAnalysis(ExecutionResult execution, String projectId) {
        List<String> evidenceIds = evidenceObjectIds(execution);
        List<BatchItemObservation> observations = new BatchItemResponseProjector().project(
                execution.result().observation().mutation(),
                execution.result().observation().observationId(),
                execution.result().executionId(),
                execution.test().testId(),
                "s10-controlled-batch",
                ENDPOINT_PATH,
                List.of(
                        new BatchItemProjectionSpec("item-a", "resource-a", "READ"),
                        new BatchItemProjectionSpec("item-b", "resource-b", "READ")),
                evidenceIds);

        List<BatchItemPolicy> policies = List.of(
                new BatchItemPolicy(
                        "s10-batch-policy-a", "GT-S10-BATCH-INDIRECT-AUTHORIZATION",
                        ENDPOINT_PATH, "resource-a", "READ", "viewer", "tenant-a",
                        AuthorizationDecision.ALLOW, evidenceIds),
                new BatchItemPolicy(
                        "s10-batch-policy-b", "GT-S10-BATCH-INDIRECT-AUTHORIZATION",
                        ENDPOINT_PATH, "resource-b", "READ", "viewer", "tenant-a",
                        AuthorizationDecision.DENY, evidenceIds));

        return new S10BatchAuthorizationAnalyzer(execution.evidence()).analyze(
                actorContext(), policies, observations, projectId);
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
                label + " batch plan must contain one fixed controlled test");
        var test = planning.plan().tests().getFirst();

        configureExecutionGuards(harness, test);
        harness.kill().reset(true, "authorized Sprint 10 localhost " + label + " batch execution");

        ExpectedDecisionCandidate expectedAggregate = new ExpectedDecisionCandidate(
                AuthorizationDecision.ALLOW,
                ExpectedDecisionSource.EXPLICIT_CONFIGURED_POLICY,
                "s10-batch-aggregate-control",
                List.of("GT-S10-BATCH-INDIRECT-AUTHORIZATION"),
                1.0);

        var result = workspace.executeNextLocal(List.of(expectedAggregate)).orElseThrow();
        return new ExecutionResult(test, result, harness.evidence());
    }

    private static PlanningInput planningInput(int port) {
        Endpoint endpoint = endpoint();
        ApiEndpointRecord inventory = new ApiEndpointRecord(
                endpoint, "v1", "LAB", AuthenticationType.BEARER, "ACRA-Lab",
                NOW, NOW, DocumentationStatus.DOCUMENTED, RiskTier.HIGH);

        SafetyPolicy safety = new SafetyPolicy(
                SafetyClass.SAFE_READ_ONLY,
                true,
                true,
                20,
                5,
                2 * 1024 * 1024,
                Set.of(HttpMethod.POST));

        return new PlanningInput(
                "PLAN-S10-BATCH-" + port,
                List.of(inventory),
                new SecurityContextGraph(),
                new io.acra.core.domain.authorization.AuthorizationMatrix(),
                List.of(contextFingerprint()),
                List.of(),
                null,
                target(port),
                Set.of(TestContract.BATCH),
                Map.of(),
                Map.of(),
                20,
                5,
                safety,
                SelectionMode.ALL,
                TestProfileDefinition.defaults(TestProfile.RESEARCH_EXPERIMENT),
                ConfigurationSnapshot.of(Map.of(
                        "dataset", "GT-S10-BATCH-INDIRECT-AUTHORIZATION",
                        "environment", "LAB",
                        "target", "localhost:" + port)),
                List.of(seed(port)),
                NOW);
    }

    private static TestSeed seed(int port) {
        return new TestSeed(
                "S10-AUTO-BATCH-MIXED-READ",
                "1",
                TestContract.BATCH,
                endpoint(),
                baseline(port),
                positive(port),
                negative(port),
                mutation(),
                contextFingerprint(),
                contextFingerprint(),
                null,
                null,
                AuthorizationDecision.ALLOW,
                List.of("GT-S10-BATCH-INDIRECT-AUTHORIZATION"),
                List.of(),
                List.of(
                        "method remains POST",
                        "authority remains localhost",
                        "endpoint remains fixed",
                        "authentication context remains user-a/viewer/tenant-a",
                        "only the explicit resource_ids list changes",
                        "fixture remains non-persistent"),
                4,
                true,
                false,
                false,
                "controlled fixed batch item-authorization validation");
    }

    private static Mutation mutation() {
        return new Mutation(
                "S10-MUT-BATCH-MIXED-READ",
                MutationType.BATCH,
                MutationLocation.BODY,
                BASE_BODY,
                MIXED_BODY,
                "user-a-batch-context",
                "user-a-batch-context",
                "add the fixed foreign resource-b to the owned resource-a read batch",
                "aggregate request remains allowed but each item must retain its own authorization decision",
                SafetyClass.SAFE_READ_ONLY,
                "s10-batch-resource-a-resource-b");
    }

    private static Endpoint endpoint() {
        return new Endpoint(
                "EP-S10-BATCH-READ",
                HttpMethod.POST,
                ENDPOINT_PATH,
                ENDPOINT_PATH,
                ENDPOINT_PATH,
                "localhost",
                "v1",
                List.of("Sprint 10 controlled non-persistent batch-read fixture"));
    }

    private static TargetDescriptor target(int port) {
        return new TargetDescriptor(
                PROJECT,
                "acra-lab-s10-batch-" + port,
                "http",
                "localhost",
                port,
                ExecutionEnvironment.LAB,
                true,
                List.of("/api/v1/s10"),
                Set.of(HttpMethod.POST));
    }

    private static RequestDefinition baseline(int port) {
        return definition(port, "S10-BATCH-BASE", true);
    }

    private static RequestDefinition positive(int port) {
        return definition(port, "S10-BATCH-POS", true);
    }

    private static RequestDefinition negative(int port) {
        return definition(port, "S10-BATCH-NEG", false);
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
                HttpMethod.POST, "http", "localhost", port, ENDPOINT_PATH,
                headers, BASE_BODY.getBytes(StandardCharsets.UTF_8), HttpProtocol.HTTP_1_1);
        return new RequestDefinition(
                id, request,
                authenticated ? "user-a-batch-context" : "anonymous-batch-context",
                "batch:s10-documents");
    }

    private static SecurityContextFingerprint contextFingerprint() {
        return new SecurityContextFingerprint(
                "user-a", "viewer", "tenant-a", "batch:s10-documents", "user-a",
                "READ", "ACTIVE", "POST /api/v1/s10/documents/batch-read", "RAW",
                "user-a-batch-context", AuthorizationDecision.ALLOW, AuthorizationDecision.UNKNOWN,
                List.of("GT-S10-BATCH-INDIRECT-AUTHORIZATION"));
    }

    private static AuthorizationContext actorContext() {
        Principal principal = new Principal("user-a", "User A", AuthenticationType.UNKNOWN, Confidence.unknown());
        Role role = new Role("viewer", "viewer", EvidenceSource.UNKNOWN, Confidence.unknown());
        Tenant tenant = new Tenant("tenant-a", "Tenant A", EvidenceSource.UNKNOWN, Confidence.unknown());
        Resource resource = new Resource(
                "resource-a", "document", null, "user-a", "tenant-a", "ACTIVE", Confidence.unknown());
        Action action = new Action(ActionType.READ, EvidenceSource.UNKNOWN, Confidence.unknown(), "READ");
        return new AuthorizationContext(
                principal, role, tenant, resource, "user-a", action,
                new WorkflowState("ACTIVE", Confidence.unknown()),
                AuthorizationDecision.UNKNOWN, AuthorizationDecision.UNKNOWN, List.of(), ContextStatus.RESOLVED);
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
                clock, Duration.ofSeconds(2), new LocalhostHttpTransport(target(port)), noDelay,
                validator, budgets, concurrency, rate,
                new BackoffPolicy(0, Duration.ofMillis(25)), kill,
                new MutationBudgetTracker(20), audit, evidence);
        return new Harness(executor, kill, budgets, concurrency, rate, evidence);
    }

    private static void configureExecutionGuards(
            Harness harness, io.acra.core.active.model.SecurityTest test) {
        for (BudgetKey key : List.of(
                new BudgetKey(BudgetScope.GLOBAL, "global"),
                new BudgetKey(BudgetScope.PROJECT, test.target().projectId()),
                new BudgetKey(BudgetScope.TARGET, test.target().targetId()),
                new BudgetKey(BudgetScope.ENDPOINT, test.endpoint().endpointId()),
                new BudgetKey(BudgetScope.TEST, test.testId()),
                new BudgetKey(BudgetScope.CONTEXT,
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
        return header + "." + payload + ".s10synthetic";
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
