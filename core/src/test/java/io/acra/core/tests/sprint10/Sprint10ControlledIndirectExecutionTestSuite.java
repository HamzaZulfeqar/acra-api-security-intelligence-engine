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
import io.acra.core.reference.IndirectReferencePolicy;
import io.acra.core.reference.IndirectReferenceResolution;
import io.acra.core.reference.IndirectReferenceResponseProjector;
import io.acra.core.reference.S10IndirectReferenceAnalysis;
import io.acra.core.reference.S10IndirectReferenceAnalyzer;
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

public final class Sprint10ControlledIndirectExecutionTestSuite {
    private static final String PROJECT = "acra-s10";
    private static final String ENDPOINT_TEMPLATE = "/api/v1/s10/share/{alias}";
    private static final String BASE_PATH = "/api/v1/s10/share/share-a";
    private static final String MUTATED_PATH = "/api/v1/s10/share/share-b";
    private static final Instant NOW = Instant.parse("2026-09-24T19:30:00Z");

    private Sprint10ControlledIndirectExecutionTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT10_CONTROLLED_INDIRECT_EXECUTION PASS assertions=" + assertions);
    }

    public static int run() {
        int assertions = 0;

        HttpRequest built = new RequestBuilder().mutate(baseline(18082).request(), mutation());
        TestSupport.assertEquals(MUTATED_PATH, built.rawTarget(),
                "declared INDIRECT_REFERENCE mutation changes only the fixed alias path segment");
        assertions++;
        TestSupport.assertEquals(HttpMethod.GET, built.method(),
                "indirect-reference mutation preserves GET method");
        assertions++;
        TestSupport.assertEquals("localhost", built.host(),
                "indirect-reference mutation preserves localhost authority");
        assertions++;

        ExecutionResult secure = execute(18082, "secure");
        TestSupport.assertEquals(io.acra.core.domain.testing.TestState.COMPLETED, secure.result().state(),
                "secure indirect-reference test completes through existing active engine");
        assertions++;
        TestSupport.assertTrue(secure.result().observation().observedDecision().deniedEquivalent(),
                "secure foreign alias remains denied-equivalent");
        assertions++;

        S10IndirectReferenceAnalysis secureAnalysis = indirectAnalysis(secure, PROJECT);
        TestSupport.assertEquals(0L, secureAnalysis.candidateCount(),
                "secure resolved-target DENY remains non-candidate");
        assertions++;
        TestSupport.assertEquals(PolicyValidationState.DENIED,
                secureAnalysis.assessments().getFirst().state(),
                "secure foreign resolved target remains explicitly denied");
        assertions++;
        TestSupport.assertEquals("resource-b",
                secureAnalysis.assessments().getFirst().resolvedResourceId(),
                "secure response is assessed against the resolved target resource");
        assertions++;

        ExecutionResult vulnerable = execute(18081, "vulnerable");
        TestSupport.assertEquals(io.acra.core.domain.testing.TestState.COMPLETED, vulnerable.result().state(),
                "vulnerable indirect-reference test completes through existing active engine");
        assertions++;
        TestSupport.assertEquals(AuthorizationOutcome.ALLOW,
                vulnerable.result().observation().observedDecision(),
                "vulnerable foreign alias is incorrectly allowed");
        assertions++;

        S10IndirectReferenceAnalysis vulnerableAnalysis = indirectAnalysis(vulnerable, PROJECT);
        TestSupport.assertEquals(1L, vulnerableAnalysis.candidateCount(),
                "resolved-target DENY plus observed ALLOW creates one review-only candidate");
        assertions++;
        TestSupport.assertTrue(vulnerableAnalysis.assessments().getFirst().violationCandidate(),
                "vulnerable indirect mismatch remains a review candidate");
        assertions++;

        S10IndirectReferenceAnalysis crossProject = indirectAnalysis(vulnerable, "other-project");
        TestSupport.assertEquals(1L, crossProject.inconclusiveCount(),
                "cross-project indirect evidence fails closed");
        assertions++;
        TestSupport.assertEquals(0L, crossProject.candidateCount(),
                "cross-project evidence cannot produce an indirect candidate");
        assertions++;

        TestSupport.assertEquals(TestContract.INDIRECT_REFERENCE, vulnerable.test().category(),
                "active indirect test retains INDIRECT_REFERENCE contract");
        assertions++;
        TestSupport.assertEquals(MutationType.INDIRECT_REFERENCE, vulnerable.test().mutation().type(),
                "active indirect test uses existing INDIRECT_REFERENCE mutation family");
        assertions++;
        TestSupport.assertEquals(MutationLocation.PATH, vulnerable.test().mutation().targetLocation(),
                "active indirect test mutates only the fixed alias path segment");
        assertions++;
        TestSupport.assertEquals(SafetyClass.SAFE_READ_ONLY, vulnerable.test().mutation().safetyClass(),
                "indirect read mutation remains explicitly safe read-only");
        assertions++;

        List<String> evidenceIds = evidenceObjectIds(vulnerable);
        IndirectReferenceResolution resolution = new IndirectReferenceResponseProjector().project(
                vulnerable.result().observation().mutation(),
                "s10-indirect-resolution-vulnerable",
                vulnerable.result().observation().observationId(),
                vulnerable.result().executionId(),
                vulnerable.test().testId(),
                ENDPOINT_TEMPLATE,
                "share-b",
                "ALIAS",
                "READ",
                AuthorizationDecision.ALLOW,
                evidenceIds);
        TestSupport.assertNotContains(new DomainSerializer().serialize(resolution), "share-b",
                "raw indirect alias is never persisted in the resolution model");
        assertions++;
        TestSupport.assertEquals(64, resolution.referenceFingerprint().length(),
                "indirect reference is retained only as SHA-256 fingerprint material");
        assertions++;

        String rawToken = token("user-a", "tenant-a", "viewer");
        TestSupport.assertNotContains(
                new DomainSerializer().serialize(vulnerable.test()),
                rawToken,
                "serialized indirect test excludes raw bearer token");
        assertions++;

        TestSupport.assertTrue(vulnerable.evidence().chain(vulnerable.result().executionId()).stream()
                        .anyMatch(entry -> entry.stage() == EvidenceStage.MUTATION_REQUEST),
                "indirect execution retains mutation-request evidence");
        assertions++;
        TestSupport.assertTrue(vulnerable.evidence().chain(vulnerable.result().executionId()).stream()
                        .anyMatch(entry -> entry.stage() == EvidenceStage.MUTATION_RESPONSE),
                "indirect execution retains mutation-response evidence");
        assertions++;

        System.out.println("SPRINT10_INDIRECT_SECURE expected=DENY observed="
                + secure.result().observation().observedDecision()
                + " candidates=" + secureAnalysis.candidateCount());
        System.out.println("SPRINT10_INDIRECT_VULNERABLE expected=DENY observed="
                + vulnerable.result().observation().observedDecision()
                + " candidates=" + vulnerableAnalysis.candidateCount());
        return assertions;
    }

    private static S10IndirectReferenceAnalysis indirectAnalysis(ExecutionResult execution, String projectId) {
        List<String> evidenceIds = evidenceObjectIds(execution);
        AuthorizationDecision observed = execution.result().observation().observedDecision() == AuthorizationOutcome.ALLOW
                ? AuthorizationDecision.ALLOW : AuthorizationDecision.DENY;

        IndirectReferenceResolution resolution = new IndirectReferenceResponseProjector().project(
                execution.result().observation().mutation(),
                "s10-indirect-resolution-" + execution.result().executionId(),
                execution.result().observation().observationId(),
                execution.result().executionId(),
                execution.test().testId(),
                ENDPOINT_TEMPLATE,
                "share-b",
                "ALIAS",
                "READ",
                observed,
                evidenceIds);

        IndirectReferencePolicy policy = new IndirectReferencePolicy(
                "s10-indirect-policy-resource-b",
                "GT-S10-BATCH-INDIRECT-AUTHORIZATION",
                ENDPOINT_TEMPLATE,
                "resource-b",
                "READ",
                "viewer",
                "tenant-a",
                AuthorizationDecision.DENY,
                evidenceIds);

        return new S10IndirectReferenceAnalyzer(execution.evidence()).analyze(
                actorContext(), List.of(policy), List.of(resolution), projectId);
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
                label + " indirect plan must contain one fixed controlled test");
        var test = planning.plan().tests().getFirst();

        configureExecutionGuards(harness, test);
        harness.kill().reset(true, "authorized Sprint 10 localhost " + label + " indirect execution");

        ExpectedDecisionCandidate expected = new ExpectedDecisionCandidate(
                AuthorizationDecision.DENY,
                ExpectedDecisionSource.EXPLICIT_CONFIGURED_POLICY,
                "s10-indirect-resolved-resource-control",
                List.of("GT-S10-BATCH-INDIRECT-AUTHORIZATION"),
                1.0);

        var result = workspace.executeNextLocal(List.of(expected)).orElseThrow();
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
                Set.of(HttpMethod.GET));

        return new PlanningInput(
                "PLAN-S10-INDIRECT-" + port,
                List.of(inventory),
                new SecurityContextGraph(),
                new io.acra.core.domain.authorization.AuthorizationMatrix(),
                List.of(contextFingerprint()),
                List.of(),
                null,
                target(port),
                Set.of(TestContract.INDIRECT_REFERENCE),
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
                "S10-AUTO-INDIRECT-FOREIGN-READ",
                "1",
                TestContract.INDIRECT_REFERENCE,
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
                List.of("GT-S10-BATCH-INDIRECT-AUTHORIZATION"),
                List.of(),
                List.of(
                        "method remains GET",
                        "authority remains localhost",
                        "route family remains /api/v1/s10/share/",
                        "authentication context remains user-a/viewer/tenant-a",
                        "only fixed alias share-a changes to fixed alias share-b",
                        "no identifier generation or enumeration"),
                4,
                true,
                false,
                false,
                "controlled fixed indirect resolved-target authorization validation");
    }

    private static Mutation mutation() {
        return new Mutation(
                "S10-MUT-INDIRECT-FOREIGN-READ",
                MutationType.INDIRECT_REFERENCE,
                MutationLocation.PATH,
                "share-a",
                "share-b",
                "user-a-indirect-context",
                "user-a-indirect-context",
                "replace the fixed owned alias with the fixed foreign alias from declared S10 ground truth",
                "resolved resource-b remains DENY for user-a even when addressed through an alias",
                SafetyClass.SAFE_READ_ONLY,
                "s10-indirect-share-a-share-b");
    }

    private static Endpoint endpoint() {
        return new Endpoint(
                "EP-S10-INDIRECT-READ",
                HttpMethod.GET,
                ENDPOINT_TEMPLATE,
                ENDPOINT_TEMPLATE,
                ENDPOINT_TEMPLATE,
                "localhost",
                "v1",
                List.of("Sprint 10 fixed alias resolved-target authorization fixture"));
    }

    private static TargetDescriptor target(int port) {
        return new TargetDescriptor(
                PROJECT,
                "acra-lab-s10-indirect-" + port,
                "http",
                "localhost",
                port,
                ExecutionEnvironment.LAB,
                true,
                List.of("/api/v1/s10/share"),
                Set.of(HttpMethod.GET));
    }

    private static RequestDefinition baseline(int port) {
        return definition(port, "S10-INDIRECT-BASE", BASE_PATH, true);
    }

    private static RequestDefinition positive(int port) {
        return definition(port, "S10-INDIRECT-POS", BASE_PATH, true);
    }

    private static RequestDefinition negative(int port) {
        return definition(port, "S10-INDIRECT-NEG", BASE_PATH, false);
    }

    private static RequestDefinition definition(int port, String id, String path, boolean authenticated) {
        List<HttpHeader> headers = authenticated
                ? List.of(
                        new HttpHeader("Authorization", "Bearer " + token("user-a", "tenant-a", "viewer")),
                        new HttpHeader("Accept", "application/json"))
                : List.of(new HttpHeader("Accept", "application/json"));
        HttpRequest request = HttpRequest.of(
                HttpMethod.GET, "http", "localhost", port, path,
                headers, new byte[0], HttpProtocol.HTTP_1_1);
        return new RequestDefinition(
                id,
                request,
                authenticated ? "user-a-indirect-context" : "anonymous-indirect-context",
                "indirect:s10-share");
    }

    private static SecurityContextFingerprint contextFingerprint() {
        return new SecurityContextFingerprint(
                "user-a",
                "viewer",
                "tenant-a",
                "indirect:s10-share",
                "user-a",
                "READ",
                "ACTIVE",
                "GET",
                ENDPOINT_TEMPLATE,
                "user-a-indirect-context",
                AuthorizationDecision.DENY,
                AuthorizationDecision.UNKNOWN,
                List.of("GT-S10-BATCH-INDIRECT-AUTHORIZATION"));
    }

    private static AuthorizationContext actorContext() {
        Principal principal = new Principal("user-a", "User A", AuthenticationType.UNKNOWN, Confidence.unknown());
        Role role = new Role("viewer", "viewer", EvidenceSource.UNKNOWN, Confidence.unknown());
        Tenant tenant = new Tenant("tenant-a", "Tenant A", EvidenceSource.UNKNOWN, Confidence.unknown());
        Resource resource = new Resource(
                "resource-b", "document", null, "user-b", "tenant-b", "ACTIVE", Confidence.unknown());
        Action action = new Action(ActionType.READ, EvidenceSource.UNKNOWN, Confidence.unknown(), "READ");
        return new AuthorizationContext(
                principal,
                role,
                tenant,
                resource,
                "user-b",
                action,
                new WorkflowState("ACTIVE", Confidence.unknown()),
                AuthorizationDecision.UNKNOWN,
                AuthorizationDecision.UNKNOWN,
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
