package io.acra.core.tests.sprint7;

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
import io.acra.core.active.model.SafetyClass;
import io.acra.core.active.model.SafetyPolicy;
import io.acra.core.active.model.SelectionMode;
import io.acra.core.active.model.TargetDescriptor;
import io.acra.core.active.model.TestContract;
import io.acra.core.active.model.TestProfile;
import io.acra.core.active.model.TestProfileDefinition;
import io.acra.core.active.model.UserMode;
import io.acra.core.active.planning.PlanningInput;
import io.acra.core.active.planning.S7WorkflowPlanningBridge;
import io.acra.core.active.planning.S7WorkflowPlanningCandidate;
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
import io.acra.core.domain.authorization.PolicyResolutionState;
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
import io.acra.core.domain.workflow.WorkflowAuthorizationResolution;
import io.acra.core.domain.workflow.WorkflowCoverageStage;
import io.acra.core.engine.S7WorkflowCoverageTracker;
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

public final class Sprint7WorkflowPlannerExecutionIntegrationTestSuite {
    private static final String PROJECT = "acra-s7";
    private static final Instant NOW = Instant.parse("2026-09-24T04:45:00Z");

    private Sprint7WorkflowPlannerExecutionIntegrationTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT7_WORKFLOW_PLANNER_EXECUTION PASS assertions=" + assertions);
    }

    public static int run() {
        int assertions = 0;
        WorkflowAuthorizationResolution baseline = resolution("SUBMITTED", AuthorizationDecision.ALLOW,
                PolicyResolutionState.RESOLVED_ALLOW);
        WorkflowAuthorizationResolution target = resolution("APPROVED", AuthorizationDecision.DENY,
                PolicyResolutionState.RESOLVED_DENY);
        S7WorkflowCoverageTracker coverage = new S7WorkflowCoverageTracker();
        coverage.recordResolution(target);

        var secureAugmentation = new S7WorkflowPlanningBridge().augment(
                planningInput(18082), List.of(candidate(18082, baseline, target)));
        TestSupport.assertEquals(1, secureAugmentation.generatedSeeds().size(),
                "S7 bridge should generate exactly one controlled workflow transition seed");
        assertions++;
        TestSupport.assertTrue(secureAugmentation.skippedReasons().isEmpty(),
                "valid explicit workflow transition should not be skipped");
        assertions++;

        var seed = secureAugmentation.generatedSeeds().getFirst();
        TestSupport.assertEquals(TestContract.TRANSITION, seed.contract(),
                "S7 generated contract should be TRANSITION");
        assertions++;
        TestSupport.assertEquals(MutationType.WORKFLOW_TRANSITION, seed.mutation().type(),
                "S7 generated mutation should be WORKFLOW_TRANSITION");
        assertions++;
        TestSupport.assertEquals(MutationLocation.BODY, seed.mutation().targetLocation(),
                "workflow target state mutation should be body-scoped");
        assertions++;
        TestSupport.assertEquals(SafetyClass.STATE_CHANGING, seed.mutation().safetyClass(),
                "workflow mutation must remain state-changing rather than read-only");
        assertions++;
        TestSupport.assertEquals("SUBMITTED", seed.mutation().originalValue(),
                "source target-state is explicit and non-secret");
        assertions++;
        TestSupport.assertEquals("APPROVED", seed.mutation().mutatedValue(),
                "mutated target-state is explicit and non-secret");
        assertions++;
        coverage.recordPlanned(target, seed);
        TestSupport.assertEquals(WorkflowCoverageStage.PLANNED,
                coverage.matrix().entries().getFirst().stage(),
                "generated workflow transition should advance coverage to PLANNED");
        assertions++;
        TestSupport.assertEquals(1, coverage.matrix().summary().plannedContexts(),
                "one resolved workflow context should be planned");
        assertions++;

        ExecutionResult secure = executeSingle(secureAugmentation.planningInput(), target, 18082, "secure");
        TestSupport.assertEquals(AuthorizationOutcome.DENY, secure.result().observation().observedDecision(),
                "secure ACRA-Lab must deny the unauthorized direct target-state transition");
        assertions++;
        TestSupport.assertEquals(DifferentialClassification.EXPECTED_CHANGE,
                secure.result().observation().differences().classification(),
                "secure workflow mutation should move in the expected deny direction");
        assertions++;
        coverage.recordExecution(target, secure.result());
        TestSupport.assertEquals(WorkflowCoverageStage.OBSERVED,
                coverage.matrix().entries().getFirst().stage(),
                "completed secure execution should advance workflow coverage to OBSERVED");
        assertions++;
        TestSupport.assertEquals(1, coverage.matrix().summary().observedContexts(),
                "secure execution should create one observed workflow coverage context");
        assertions++;

        var vulnerableAugmentation = new S7WorkflowPlanningBridge().augment(
                planningInput(18081), List.of(candidate(18081, baseline, target)));
        coverage.recordPlanned(target, vulnerableAugmentation.generatedSeeds().getFirst());
        ExecutionResult vulnerable = executeSingle(vulnerableAugmentation.planningInput(), target, 18081, "vulnerable");
        TestSupport.assertEquals(AuthorizationOutcome.ALLOW, vulnerable.result().observation().observedDecision(),
                "deliberately vulnerable ACRA-Lab should allow the direct target-state bypass");
        assertions++;
        TestSupport.assertEquals(DifferentialClassification.UNEXPECTED_CHANGE,
                vulnerable.result().observation().differences().classification(),
                "workflow policy deny plus observed allow must be an unexpected differential");
        assertions++;
        TestSupport.assertTrue(vulnerable.result().observation().evidenceIds().size() > 0,
                "live workflow execution should retain evidence references");
        assertions++;
        coverage.recordExecution(target, vulnerable.result());
        TestSupport.assertEquals(2, coverage.matrix().entries().getFirst().executionIds().size(),
                "secure and vulnerable runs should retain two distinct execution references");
        assertions++;
        TestSupport.assertEquals(2, coverage.matrix().entries().getFirst().observationIds().size(),
                "secure and vulnerable runs should retain two distinct observations");
        assertions++;
        TestSupport.assertTrue(!coverage.matrix().entries().getFirst().observationEvidenceIds().isEmpty(),
                "workflow coverage should retain observation evidence references");
        assertions++;

        String rawToken = token("author-a", "tenant-a", "author");
        String serialized = new DomainSerializer().serialize(vulnerable.test());
        TestSupport.assertNotContains(serialized, rawToken,
                "serialized S7 workflow test must not expose raw bearer material");
        assertions++;
        TestSupport.assertFalse(seed.mutation().sourceContext().contains(rawToken)
                        || seed.mutation().targetContext().contains(rawToken),
                "Mutation context fields must remain references/labels only");
        assertions++;

        WorkflowAuthorizationResolution conflicting = resolution("APPROVED", AuthorizationDecision.UNKNOWN,
                PolicyResolutionState.CONFLICTING);
        var conflict = new S7WorkflowPlanningBridge().augment(
                planningInput(18082), List.of(candidate(18082, baseline, conflicting)));
        TestSupport.assertEquals(0, conflict.generatedSeeds().size(),
                "conflicting workflow policy must fail closed before active generation");
        assertions++;
        TestSupport.assertTrue(conflict.skippedReasons().stream().anyMatch(v -> v.contains("WORKFLOW_POLICY_UNRESOLVED")),
                "conflicting policy skip reason should remain explicit");
        assertions++;

        System.out.println("SPRINT7_WORKFLOW_SECURE testId=" + secure.test().testId()
                + " expected=" + secure.test().expectedDecision()
                + " observed=" + secure.result().observation().observedDecision()
                + " differential=" + secure.result().observation().differences().classification());
        System.out.println("SPRINT7_WORKFLOW_VULNERABLE testId=" + vulnerable.test().testId()
                + " expected=" + vulnerable.test().expectedDecision()
                + " observed=" + vulnerable.result().observation().observedDecision()
                + " differential=" + vulnerable.result().observation().differences().classification());
        return assertions;
    }

    private static ExecutionResult executeSingle(PlanningInput input, WorkflowAuthorizationResolution resolution,
                                                 int port, String label) {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        Harness harness = harness(clock, port);
        ActiveEngineWorkspace workspace = new ActiveEngineWorkspace(clock, harness.executor());
        workspace.configureMode(UserMode.RESEARCHER);
        var planning = workspace.plan(input);
        TestSupport.assertEquals(1, planning.plan().tests().size(),
                label + " S7 planning should admit exactly one generated workflow test");
        var test = planning.plan().tests().getFirst();
        configureExecutionGuards(harness, test);
        harness.kill().reset(true, "authorized Sprint 7 localhost " + label + " workflow execution");
        ExpectedDecisionCandidate expected = new ExpectedDecisionCandidate(
                resolution.expectedDecision(), ExpectedDecisionSource.EXPLICIT_CONFIGURED_POLICY,
                "s7-workflow-policy", resolution.evidenceIds(), 1.0);
        var result = workspace.executeNextLocal(List.of(expected)).orElseThrow();
        TestSupport.assertEquals(io.acra.core.domain.testing.TestState.COMPLETED, result.state(),
                label + " workflow test should execute through the existing S4 executor");
        return new ExecutionResult(test, result, workspace);
    }

    private static S7WorkflowPlanningCandidate candidate(int port,
                                                          WorkflowAuthorizationResolution baseline,
                                                          WorkflowAuthorizationResolution target) {
        return new S7WorkflowPlanningCandidate(
                "S7-CANDIDATE-WORKFLOW",
                endpoint(),
                requestDefinition(port, "S7-REQ-WF-BASE", "SUBMITTED", "author-a", "author", "ctx-author-base"),
                requestDefinition(port, "S7-REQ-WF-POS", "SUBMITTED", "author-a", "author", "ctx-author-pos"),
                approvalNegativeDefinition(port),
                sourceContext(),
                targetContext(),
                resource("DRAFT"),
                resource("APPROVED"),
                baseline,
                target,
                Set.of(TestContract.TRANSITION),
                List.of(),
                List.of("workflow remains document-approval", "principal remains author-a", "action remains SUBMIT",
                        "from-state remains DRAFT"),
                false,
                false);
    }

    private static WorkflowAuthorizationResolution resolution(String toState, AuthorizationDecision expected,
                                                               PolicyResolutionState state) {
        return new WorkflowAuthorizationResolution(
                "s7-resolution-" + toState.toLowerCase(),
                "workflow-policy-fixture",
                "document-approval",
                "author-a",
                "tenant-a",
                "document-1",
                "SUBMIT",
                "DRAFT",
                toState,
                List.of("rule-submit"),
                List.of(),
                List.of(),
                expected,
                AuthorizationDecision.UNKNOWN,
                state,
                List.of("s7-policy-evidence", "s7-rule-evidence"),
                List.of());
    }

    private static PlanningInput planningInput(int port) {
        Endpoint endpoint = endpoint();
        ApiEndpointRecord inventory = new ApiEndpointRecord(endpoint, "v1", "LAB", AuthenticationType.BEARER,
                "ACRA-Lab", NOW, NOW, DocumentationStatus.DOCUMENTED, RiskTier.HIGH);
        SafetyPolicy safety = new SafetyPolicy(SafetyClass.STATE_CHANGING, true, true, 20, 5,
                2 * 1024 * 1024, Set.of(HttpMethod.POST));
        return new PlanningInput("PLAN-S7-WORKFLOW-" + port, List.of(inventory), new SecurityContextGraph(),
                new AuthorizationMatrix(), List.of(sourceContext(), targetContext()), List.of(), null,
                target(port), Set.of(TestContract.TRANSITION), Map.of(), Map.of(), 20, 5, safety,
                SelectionMode.ALL, TestProfileDefinition.defaults(TestProfile.RESEARCH_EXPERIMENT),
                ConfigurationSnapshot.of(Map.of("dataset", "S7-WORKFLOW-PLANNING", "environment", "LAB",
                        "target", "localhost:" + port)), List.of(), NOW);
    }

    private static Endpoint endpoint() {
        return new Endpoint("EP-S7-WORKFLOW", HttpMethod.POST,
                "/api/v1/s7/workflows/document-approval/resources/document-1/transition",
                "/api/v1/s7/workflows/{workflow}/resources/{resource}/transition",
                "/api/v1/s7/workflows/{workflow}/resources/{resource}/transition",
                "localhost", "v1", List.of("Sprint 7 controlled workflow transition"));
    }

    private static TargetDescriptor target(int port) {
        return new TargetDescriptor(PROJECT, "acra-lab-s7-" + port, "http", "localhost", port,
                ExecutionEnvironment.LAB, true, List.of("/api/v1/s7"), Set.of(HttpMethod.POST));
    }

    private static RequestDefinition requestDefinition(int port, String id, String toState,
                                                       String principal, String role, String contextRef) {
        String body = "{\"action\":\"SUBMIT\",\"from_state\":\"DRAFT\",\"to_state\":\"" + toState
                + "\",\"approval\":false,\"role_separation\":true}";
        HttpRequest request = HttpRequest.of(HttpMethod.POST, "http", "localhost", port,
                "/api/v1/s7/workflows/document-approval/resources/document-1/transition",
                List.of(new HttpHeader("Authorization", "Bearer " + token(principal, "tenant-a", role)),
                        new HttpHeader("Content-Type", "application/json"),
                        new HttpHeader("Accept", "application/json")),
                body.getBytes(StandardCharsets.UTF_8), HttpProtocol.HTTP_1_1);
        return new RequestDefinition(id, request, contextRef, "workflow-resource:document-1");
    }

    private static RequestDefinition approvalNegativeDefinition(int port) {
        String body = "{\"action\":\"APPROVE\",\"from_state\":\"SUBMITTED\",\"to_state\":\"APPROVED\","
                + "\"approval\":false,\"role_separation\":true}";
        HttpRequest request = HttpRequest.of(HttpMethod.POST, "http", "localhost", port,
                "/api/v1/s7/workflows/document-approval/resources/document-1/transition",
                List.of(new HttpHeader("Authorization", "Bearer " + token("author-a", "tenant-a", "author")),
                        new HttpHeader("Content-Type", "application/json"),
                        new HttpHeader("Accept", "application/json")),
                body.getBytes(StandardCharsets.UTF_8), HttpProtocol.HTTP_1_1);
        return new RequestDefinition("S7-REQ-WF-NEG", request, "ctx-author-neg", "workflow-resource:document-1");
    }

    private static SecurityContextFingerprint sourceContext() {
        return new SecurityContextFingerprint("author-a", "author", "tenant-a", "document-1", "author-a",
                "SUBMIT", "DRAFT", "POST /api/v1/s7/workflows/{workflow}/resources/{resource}/transition", "raw",
                "s7-author-context", AuthorizationDecision.ALLOW, AuthorizationDecision.UNKNOWN,
                List.of("s7-policy-evidence"));
    }

    private static SecurityContextFingerprint targetContext() {
        return new SecurityContextFingerprint("author-a", "author", "tenant-a", "document-1", "author-a",
                "SUBMIT", "APPROVED", "POST /api/v1/s7/workflows/{workflow}/resources/{resource}/transition", "raw",
                "s7-author-context", AuthorizationDecision.DENY, AuthorizationDecision.UNKNOWN,
                List.of("s7-policy-evidence"));
    }

    private static Resource resource(String state) {
        return new Resource("document-1", "workflow-document", null, "author-a", "tenant-a", state,
                Confidence.unknown());
    }

    private static Harness harness(Clock clock, int port) {
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
                new LocalhostHttpTransport(target(port)), noDelay, validator, budgets, concurrency, rate,
                new BackoffPolicy(0, Duration.ofMillis(25)), kill, new MutationBudgetTracker(20), audit, evidence);
        return new Harness(executor, kill, budgets, concurrency, rate);
    }

    private static void configureExecutionGuards(Harness harness, io.acra.core.active.model.SecurityTest test) {
        for (BudgetKey key : List.of(
                new BudgetKey(BudgetScope.GLOBAL, "global"),
                new BudgetKey(BudgetScope.PROJECT, test.target().projectId()),
                new BudgetKey(BudgetScope.TARGET, test.target().targetId()),
                new BudgetKey(BudgetScope.ENDPOINT, test.endpoint().endpointId()),
                new BudgetKey(BudgetScope.TEST, test.testId()),
                new BudgetKey(BudgetScope.CONTEXT, test.targetContext().principal() + '|' + test.targetContext().tenant()
                        + '|' + test.targetContext().role()))) harness.budgets().configure(key, 50);
        for (ConcurrencyKey key : List.of(
                new ConcurrencyKey(ConcurrencyScope.GLOBAL, "global"),
                new ConcurrencyKey(ConcurrencyScope.HOST, test.target().host()),
                new ConcurrencyKey(ConcurrencyScope.TARGET, test.target().targetId()),
                new ConcurrencyKey(ConcurrencyScope.ENDPOINT, test.endpoint().endpointId()))) harness.concurrency().configure(key, 2);
        RateLimitPolicy policy = new RateLimitPolicy(1000, 5000, 1000, 0);
        for (String key : List.of("host:" + test.target().host(), "target:" + test.target().targetId(),
                "endpoint:" + test.endpoint().endpointId(), "test:" + test.testId())) harness.rate().configure(key, policy);
    }

    private static String token(String sub, String tenant, String role) {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String header = encoder.encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}"
                .getBytes(StandardCharsets.UTF_8));
        String payload = encoder.encodeToString(("{\"sub\":\"" + sub + "\",\"tenant_id\":\"" + tenant
                + "\",\"role\":\"" + role + "\"}").getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".s7synthetic";
    }

    private record Harness(TestExecutor executor, KillSwitch kill, HierarchicalBudgetManager budgets,
                           HierarchicalConcurrencyController concurrency, ScopedRateLimiter rate) { }

    private record ExecutionResult(io.acra.core.active.model.SecurityTest test,
                                   io.acra.core.active.execution.TestExecutionResult result,
                                   ActiveEngineWorkspace workspace) { }
}
