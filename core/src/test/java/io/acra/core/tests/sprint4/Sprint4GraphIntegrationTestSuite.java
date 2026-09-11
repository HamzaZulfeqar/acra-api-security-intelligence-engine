package io.acra.core.tests.sprint4;

import io.acra.core.active.evidence.ExecutionEvidenceStore;
import io.acra.core.active.evidence.Observation;
import io.acra.core.active.evidence.SafetyAuditLog;
import io.acra.core.active.execution.BackoffPolicy;
import io.acra.core.active.execution.HttpTransport;
import io.acra.core.active.execution.RequestVariantKind;
import io.acra.core.active.execution.TestExecutionResult;
import io.acra.core.active.execution.TestExecutor;
import io.acra.core.active.execution.TransportResult;
import io.acra.core.active.graph.GraphHydrationStatus;
import io.acra.core.active.graph.ObservationGraphIntegrator;
import io.acra.core.active.model.ExecutionEnvironment;
import io.acra.core.active.model.SecurityTest;
import io.acra.core.active.model.TargetDescriptor;
import io.acra.core.active.replay.ReplayService;
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
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.common.Confidence;
import io.acra.core.domain.common.ConfidenceBasis;
import io.acra.core.domain.evidence.Evidence;
import io.acra.core.domain.evidence.EvidenceSource;
import io.acra.core.domain.resource.Resource;
import io.acra.core.domain.testing.TestState;
import io.acra.core.graph.GraphEdge;
import io.acra.core.graph.GraphNode;
import io.acra.core.graph.NodeType;
import io.acra.core.graph.RelationType;
import io.acra.core.graph.SecurityContextGraph;
import io.acra.core.recon.SecurityContextFingerprint;
import io.acra.core.serialization.DomainSerializer;
import io.acra.core.tests.TestSupport;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Sprint4GraphIntegrationTestSuite {
    private enum ResponseMode { NORMAL, CONFLICT }

    private static final String PASSWORD_SECRET = "synthetic-password-secret";
    private static final String API_KEY_SECRET = "synthetic-api-key-secret";
    private static final String SESSION_SECRET = "synthetic-session-secret";

    private record Harness(TestExecutor executor, SecurityContextGraph graph, ExecutionEvidenceStore evidence) {}

    private static int tests;

    private Sprint4GraphIntegrationTestSuite() {}

    public static void main(String[] args) {
        testHydrationAndProvenance();
        testIncompleteContextsAreInconclusive();
        testObservedContextConflictsArePreserved();
        testDuplicateAndReplayHistory();
        testGraphSecurityAndProjectIsolation();
        System.out.println("PASS Sprint4 graph integration tests=" + tests);
    }

    private static void testHydrationAndProvenance() {
        SecurityTest test = Sprint4Fixtures.test("S4-GRAPH-001", "graph-hydration");
        Harness harness = harness(test, ResponseMode.NORMAL, Sprint4Fixtures.PROJECT,
                Sprint4Fixtures.PROJECT, new SecurityContextGraph());
        TestExecutionResult execution = execute(harness, test, true);
        var hydration = harness.executor().graphHydration(execution.executionId()).orElseThrow();

        TestSupport.assertEquals(TestState.COMPLETED, execution.state(), "graph source execution completes");
        TestSupport.assertEquals(GraphHydrationStatus.HYDRATED, hydration.status(), "complete observation hydrates graph");
        TestSupport.assertTrue(hydration.graphUpdated(), "hydration reports graph update");
        TestSupport.assertTrue(harness.graph().node("principal:User-B").isPresent(), "target principal node exists");
        TestSupport.assertTrue(harness.graph().node("role:viewer").isPresent(), "target role node exists");
        TestSupport.assertTrue(harness.graph().node("tenant:Tenant-A").isPresent(), "target tenant node exists");
        TestSupport.assertTrue(harness.graph().node("resource:document:1001").isPresent(), "target resource node exists");
        TestSupport.assertTrue(harness.graph().node("principal:User-A").isPresent(), "resource owner node exists");

        GraphEdge access = harness.graph().outgoing("principal:User-B", RelationType.ACCESSES).stream()
                .filter(edge -> edge.target().equals("resource:document:1001")).findFirst().orElseThrow();
        TestSupport.assertTrue(access.confidence().basis() == ConfidenceBasis.EXACT_OBSERVED,
                "attempted access remains exactly observed, not a vulnerability assertion");
        TestSupport.assertTrue(harness.graph().outgoing("principal:User-A", RelationType.OWNS).stream()
                .anyMatch(edge -> edge.target().equals("resource:document:1001")), "configured owner relationship retained");

        List<Evidence> support = access.evidenceIds().stream().map(id -> harness.graph().evidence(id).orElseThrow()).toList();
        TestSupport.assertTrue(support.stream().anyMatch(value -> value.location().equals("s4/MUTATION_RESPONSE")),
                "edge provenance includes mutation response");
        TestSupport.assertTrue(support.stream().anyMatch(value -> value.location().equals("s4/OBSERVATION")),
                "edge provenance includes observation evidence");
        TestSupport.assertTrue(support.stream().allMatch(value -> value.requestId().equals(execution.executionId())),
                "edge provenance retains execution id");
        TestSupport.assertTrue(support.stream().allMatch(value -> value.extractedValue().contains("testId=" + test.testId())),
                "edge provenance retains test id");
        TestSupport.assertTrue(support.stream().allMatch(value -> value.extractedValue()
                        .contains("observationId=" + execution.observation().observationId())),
                "edge provenance retains observation id");
        TestSupport.assertTrue(support.stream().allMatch(value -> value.extractedValue().contains("chainEvidenceId=")),
                "edge provenance retains originating S4 evidence id");
        tests += 16;
    }

    private static void testIncompleteContextsAreInconclusive() {
        SecurityTest original = Sprint4Fixtures.test("S4-GRAPH-INCOMPLETE", "graph-incomplete");
        List<SecurityTest> cases = List.of(
                copy(original, "IDENTITY", context(original, UNKNOWN, null, null, null, null),
                        original.targetResource(), original.expectedDecision()),
                copy(original, "TENANT", context(original, null, null, UNKNOWN, null, null),
                        original.targetResource(), original.expectedDecision()),
                copy(original, "OWNER", context(original, null, null, null, null, UNKNOWN),
                        new Resource(original.targetResource().resourceId(), original.targetResource().resourceType(),
                                null, null, original.targetResource().tenantId(), original.targetResource().state(), Confidence.unknown()),
                        original.expectedDecision()),
                copy(original, "RESOURCE", context(original, null, null, null, UNKNOWN, null), null,
                        original.expectedDecision()),
                copy(original, "POLICY", original.targetContext(), original.targetResource(), AuthorizationDecision.UNKNOWN));

        for (SecurityTest test : cases) {
            Harness harness = harness(test, ResponseMode.NORMAL, Sprint4Fixtures.PROJECT,
                    Sprint4Fixtures.PROJECT, new SecurityContextGraph());
            TestExecutionResult execution = execute(harness, test, test.expectedDecision() != AuthorizationDecision.UNKNOWN);
            var hydration = harness.executor().graphHydration(execution.executionId()).orElseThrow();
            TestSupport.assertEquals(TestState.COMPLETED, execution.state(), "incomplete fixture still produces an observation");
            TestSupport.assertEquals(GraphHydrationStatus.INCONCLUSIVE, hydration.status(),
                    "incomplete context is inconclusive");
            TestSupport.assertEquals(0, harness.graph().nodeCount(), "incomplete context creates no graph nodes");
            TestSupport.assertEquals(0, harness.graph().edgeCount(), "incomplete context creates no graph edges");
        }
        tests += 20;
    }

    private static void testObservedContextConflictsArePreserved() {
        SecurityTest test = Sprint4Fixtures.test("S4-GRAPH-CONFLICT", "graph-conflict");
        Harness harness = harness(test, ResponseMode.CONFLICT, Sprint4Fixtures.PROJECT,
                Sprint4Fixtures.PROJECT, new SecurityContextGraph());
        TestExecutionResult execution = execute(harness, test, true);
        var hydration = harness.executor().graphHydration(execution.executionId()).orElseThrow();

        TestSupport.assertEquals(TestState.COMPLETED, execution.state(), "conflict observation executes normally");
        TestSupport.assertEquals(GraphHydrationStatus.CONFLICTING_CONTEXT, hydration.status(),
                "context mismatch is explicit conflict state");
        TestSupport.assertTrue(harness.graph().node("resource:document:1001").isPresent(), "configured resource preserved");
        TestSupport.assertTrue(harness.graph().node("resource:document:9999").isPresent(), "observed resource preserved");
        TestSupport.assertTrue(harness.graph().node("principal:User-A").isPresent(), "configured owner preserved");
        TestSupport.assertTrue(harness.graph().node("principal:Owner-B").isPresent(), "observed owner preserved");
        TestSupport.assertTrue(harness.graph().node("tenant:Tenant-A").isPresent(), "configured tenant preserved");
        TestSupport.assertTrue(harness.graph().node("tenant:Tenant-B").isPresent(), "observed tenant preserved");
        List<GraphNode> conflicts = harness.graph().nodes().stream().filter(node -> node.type() == NodeType.CONFLICT).toList();
        TestSupport.assertEquals(3, conflicts.size(), "resource owner and tenant conflicts remain distinct");
        TestSupport.assertTrue(conflicts.stream().allMatch(node -> harness.graph()
                        .outgoing(node.id(), RelationType.CONFLICTS_WITH).size() == 2),
                "each conflict links configured and observed facts");
        TestSupport.assertTrue(harness.graph().outgoing("principal:User-A", RelationType.OWNS).stream()
                .anyMatch(edge -> edge.target().equals("resource:document:1001")), "configured owner fact not overwritten");
        TestSupport.assertTrue(harness.graph().outgoing("principal:Owner-B", RelationType.OWNS).stream()
                .anyMatch(edge -> edge.target().equals("resource:document:9999")), "observed owner fact added separately");
        TestSupport.assertContains(String.join(";", hydration.reasons()), "resource", "conflict reason is explicit");
        tests += 13;
    }

    private static void testDuplicateAndReplayHistory() {
        SecurityTest test = Sprint4Fixtures.test("S4-GRAPH-REPLAY", "graph-replay");
        Harness harness = harness(test, ResponseMode.NORMAL, Sprint4Fixtures.PROJECT,
                Sprint4Fixtures.PROJECT, new SecurityContextGraph());
        TestExecutionResult first = execute(harness, test, true);
        int nodes = harness.graph().nodeCount();
        int edges = harness.graph().edgeCount();
        int evidence = harness.graph().evidenceCount();
        String historicalEvidence = harness.graph().evidenceList().getFirst().evidenceId();

        var duplicate = new ObservationGraphIntegrator(Sprint4Fixtures.PROJECT).hydrate(harness.graph(), test, first);
        TestSupport.assertTrue(duplicate.graphUpdated(), "identical observation hydration remains deterministic");
        TestSupport.assertEquals(nodes, harness.graph().nodeCount(), "identical observation creates no duplicate nodes");
        TestSupport.assertEquals(edges, harness.graph().edgeCount(), "identical observation creates no duplicate edges");
        TestSupport.assertEquals(evidence, harness.graph().evidenceCount(), "identical observation creates no duplicate evidence");

        ReplayService replay = new ReplayService(Clock.fixed(Sprint4Fixtures.NOW, ZoneOffset.UTC));
        var replayResult = replay.replay(replay.descriptor(first, test), harness.executor(), consent(),
                List.of(Sprint4Fixtures.groundTruthDeny()));
        TestSupport.assertTrue(replayResult.verified(), "replay produces verified fresh execution");
        TestExecutionResult second = replayResult.freshExecution();
        TestSupport.assertFalse(first.executionId().equals(second.executionId()), "replay has new execution id");
        TestSupport.assertFalse(first.observation().observationId().equals(second.observation().observationId()),
                "replay has new observation id");
        TestSupport.assertEquals(GraphHydrationStatus.HYDRATED,
                harness.executor().graphHydration(second.executionId()).orElseThrow().status(),
                "replay is hydrated through normal executor path");
        TestSupport.assertEquals(nodes, harness.graph().nodeCount(), "replay reuses deterministic entity nodes");
        TestSupport.assertTrue(harness.graph().edgeCount() > edges, "replay adds separate provenance edges");
        TestSupport.assertTrue(harness.graph().evidenceCount() > evidence, "replay adds separate provenance evidence");
        TestSupport.assertTrue(harness.graph().evidence(historicalEvidence).isPresent(), "historical evidence remains unchanged");
        tests += 12;
    }

    private static void testGraphSecurityAndProjectIsolation() {
        SecurityTest test = Sprint4Fixtures.test("S4-GRAPH-SECURITY", "graph-security");
        SecurityContextGraph graph = new SecurityContextGraph();
        Evidence prior = Evidence.create(EvidenceSource.USER_POLICY, "prior-execution", "policy", "tenant=Other",
                "fixture", Confidence.of(ConfidenceBasis.EXPLICIT_METADATA), Sprint4Fixtures.NOW);
        graph.addEvidence(prior);
        graph.addNode(new GraphNode("principal:Unrelated", NodeType.PRINCIPAL, "Unrelated", Map.of()));
        graph.addNode(new GraphNode("tenant:Other", NodeType.TENANT, "Other", Map.of()));
        GraphEdge priorEdge = GraphEdge.create("principal:Unrelated", "tenant:Other", RelationType.BELONGS_TO,
                Confidence.of(ConfidenceBasis.EXPLICIT_METADATA), List.of(prior.evidenceId()), Sprint4Fixtures.NOW);
        graph.addEdge(priorEdge);

        Harness harness = harness(test, ResponseMode.NORMAL, Sprint4Fixtures.PROJECT, Sprint4Fixtures.PROJECT, graph);
        TestExecutionResult execution = execute(harness, test, true);
        String serialized = new DomainSerializer().serialize(graph);
        String serializedObservation = new DomainSerializer().serialize(execution.observation());
        String serializedEvidence = execution.evidenceChain().stream()
                .map(entry -> new DomainSerializer().serialize(harness.evidence().object(entry.objectId())))
                .reduce("", (left, right) -> left + '\n' + right);
        TestSupport.assertNotContains(serialized, "synthetic-user-a-token", "raw source credential never enters graph");
        TestSupport.assertNotContains(serialized, "synthetic-user-b-token", "raw target credential never enters graph");
        TestSupport.assertNotContains(serialized, "Bearer ", "authorization representation never enters graph");
        TestSupport.assertNotContains(serialized, PASSWORD_SECRET, "raw password never enters serialized graph state");
        TestSupport.assertNotContains(serialized, API_KEY_SECRET, "raw API key never enters serialized graph state");
        TestSupport.assertNotContains(serialized, SESSION_SECRET, "raw session secret never enters serialized graph state");
        TestSupport.assertNotContains(serializedObservation, PASSWORD_SECRET, "raw password never enters observation");
        TestSupport.assertNotContains(serializedObservation, API_KEY_SECRET, "raw API key never enters observation");
        TestSupport.assertNotContains(serializedObservation, SESSION_SECRET, "raw session secret never enters observation");
        TestSupport.assertNotContains(serializedEvidence, PASSWORD_SECRET, "raw password never enters evidence chain objects");
        TestSupport.assertNotContains(serializedEvidence, API_KEY_SECRET, "raw API key never enters evidence chain objects");
        TestSupport.assertNotContains(serializedEvidence, SESSION_SECRET, "raw session secret never enters evidence chain objects");
        TestSupport.assertTrue(graph.edges().stream().anyMatch(edge -> edge.equals(priorEdge)),
                "unrelated tenant relationship remains unchanged");
        TestSupport.assertTrue(graph.evidence(prior.evidenceId()).orElseThrow().equals(prior),
                "unrelated graph provenance remains unchanged");

        Observation original = execution.observation();
        List<String> forgedIds = new ArrayList<>(original.evidenceIds());
        forgedIds.add("forged-evidence");
        Observation forged = new Observation(original.observationId(), original.testId(), original.baseline(),
                original.positiveControl(), original.negativeControl(), original.mutation(), original.expectedDecision(),
                original.observedDecision(), original.differences(), original.sourceContext(), original.targetContext(),
                forgedIds, original.confidence(), original.executionFingerprint(), original.createdAt());
        TestExecutionResult forgedResult = new TestExecutionResult(execution.executionId(), execution.testId(),
                execution.state(), execution.validation(), forged, null, execution.evidenceChain());
        int nodesBefore = graph.nodeCount();
        int edgesBefore = graph.edgeCount();
        var rejected = new ObservationGraphIntegrator(Sprint4Fixtures.PROJECT).hydrate(graph, test, forgedResult);
        TestSupport.assertEquals(GraphHydrationStatus.BLOCKED, rejected.status(), "forged observation provenance is blocked");
        TestSupport.assertEquals(nodesBefore, graph.nodeCount(), "forged provenance adds no nodes");
        TestSupport.assertEquals(edgesBefore, graph.edgeCount(), "forged provenance adds no edges");

        SecurityTest otherProject = withProject(test, "other-project");
        Harness isolated = harness(otherProject, ResponseMode.NORMAL, "other-project", Sprint4Fixtures.PROJECT,
                new SecurityContextGraph());
        TestExecutionResult otherExecution = execute(isolated, otherProject, true);
        TestSupport.assertEquals(GraphHydrationStatus.BLOCKED,
                isolated.executor().graphHydration(otherExecution.executionId()).orElseThrow().status(),
                "cross-project graph hydration is blocked");
        TestSupport.assertEquals(0, isolated.graph().nodeCount(), "cross-project execution cannot populate graph");

        SecurityContextGraph collisionGraph = new SecurityContextGraph();
        GraphNode historical = new GraphNode("principal:User-B", NodeType.PRINCIPAL, "Historical-User-B", Map.of());
        collisionGraph.addNode(historical);
        Evidence collisionEvidence = Evidence.create(EvidenceSource.USER_POLICY, "collision-history", "policy",
                "tenant=Historical", "fixture", Confidence.of(ConfidenceBasis.EXPLICIT_METADATA), Sprint4Fixtures.NOW);
        collisionGraph.addEvidence(collisionEvidence);
        collisionGraph.addNode(new GraphNode("principal:Historical", NodeType.PRINCIPAL, "Historical", Map.of()));
        collisionGraph.addNode(new GraphNode("tenant:Historical", NodeType.TENANT, "Historical", Map.of()));
        collisionGraph.addEdge(GraphEdge.create("principal:Historical", "tenant:Historical", RelationType.BELONGS_TO,
                Confidence.of(ConfidenceBasis.EXPLICIT_METADATA), List.of(collisionEvidence.evidenceId()), Sprint4Fixtures.NOW));
        String collisionStateBefore = new DomainSerializer().serialize(collisionGraph);
        Harness collision = harness(test, ResponseMode.NORMAL, Sprint4Fixtures.PROJECT,
                Sprint4Fixtures.PROJECT, collisionGraph);
        TestExecutionResult collisionExecution = execute(collision, test, true);
        var collisionHydration = collision.executor().graphHydration(collisionExecution.executionId()).orElseThrow();
        TestSupport.assertEquals(TestState.COMPLETED, collisionExecution.state(),
                "graph identity conflict does not invalidate completed execution");
        TestSupport.assertEquals(GraphHydrationStatus.BLOCKED, collisionHydration.status(),
                "graph identity conflict blocks only hydration");
        TestSupport.assertEquals(historical, collisionGraph.node("principal:User-B").orElseThrow(),
                "historical graph node is not overwritten");
        TestSupport.assertEquals(3, collisionGraph.nodeCount(), "blocked collision creates no graph nodes");
        TestSupport.assertEquals(1, collisionGraph.edgeCount(), "blocked collision creates no graph edges");
        TestSupport.assertEquals(1, collisionGraph.evidenceCount(), "blocked collision creates no graph evidence");
        TestSupport.assertEquals(collisionStateBefore, new DomainSerializer().serialize(collisionGraph),
                "multi-node and multi-edge hydration conflict is atomic");
        tests += 26;
        System.out.println("S4_GRAPH_NODE_MERGE inserted=PASS equivalent=PASS conflict=BLOCKED atomicity=PASS");
    }

    private static TestExecutionResult execute(Harness harness, SecurityTest test, boolean groundTruth) {
        return harness.executor().execute(test, consent(), groundTruth ? List.of(Sprint4Fixtures.groundTruthDeny()) : List.of());
    }

    private static Harness harness(SecurityTest test, ResponseMode mode, String validatorProject,
                                   String graphProject, SecurityContextGraph graph) {
        Clock clock = Clock.fixed(Sprint4Fixtures.NOW, ZoneOffset.UTC);
        SafetyAuditLog audit = new SafetyAuditLog(clock);
        KillSwitch kill = new KillSwitch(audit);
        kill.reset(true, "S4 graph integration fixture");
        HierarchicalBudgetManager budgets = new HierarchicalBudgetManager(audit);
        for (BudgetKey key : budgetKeys(test)) budgets.configure(key, 100);
        HierarchicalConcurrencyController concurrency = new HierarchicalConcurrencyController();
        for (ConcurrencyKey key : concurrencyKeys(test)) concurrency.configure(key, 2);
        ScopedRateLimiter rate = new ScopedRateLimiter();
        RateLimitPolicy policy = new RateLimitPolicy(1000, 5000, 1000, 0);
        for (String key : rateKeys(test)) rate.configure(key, policy);
        MutationValidator validator = new MutationValidator(validatorProject, Set.of(ExecutionEnvironment.LAB), kill,
                budgets, concurrency, rate, audit);
        ExecutionEvidenceStore evidence = new ExecutionEvidenceStore(clock);
        HttpTransport transport = transport(mode);
        TestExecutor executor = new TestExecutor(clock, Duration.ofSeconds(2), transport, duration -> { }, validator,
                budgets, concurrency, rate, new BackoffPolicy(0, Duration.ofMillis(10)), kill,
                new MutationBudgetTracker(100), audit, evidence, graph, graphProject);
        return new Harness(executor, graph, evidence);
    }

    private static HttpTransport transport(ResponseMode mode) {
        return (request, timeout, cancellation) -> {
            cancellation.throwIfCancelled();
            var response = switch (request.kind()) {
                case BASELINE, POSITIVE_CONTROL -> Sprint4Fixtures.response(200,
                        "{\"id\":\"1001\",\"owner_id\":\"User-A\",\"tenant_id\":\"Tenant-A\"}");
                case NEGATIVE_CONTROL -> Sprint4Fixtures.response(200,
                        "{\"success\":false,\"message\":\"Access denied\"}");
                case MUTATION -> mode == ResponseMode.CONFLICT
                        ? Sprint4Fixtures.response(200,
                                "{\"id\":\"9999\",\"owner_id\":\"Owner-B\",\"tenant_id\":\"Tenant-B\"}")
                        : Sprint4Fixtures.response(200,
                                "{\"success\":false,\"message\":\"Access denied\"}");
            };
            return new TransportResult(response, Map.of(
                    "password", PASSWORD_SECRET,
                    "api_key", API_KEY_SECRET,
                    "session", SESSION_SECRET), Duration.ofMillis(1));
        };
    }

    private static SecurityTest copy(SecurityTest source, String suffix, SecurityContextFingerprint targetContext,
                                     Resource targetResource, AuthorizationDecision expected) {
        return new SecurityTest(source.testId() + '-' + suffix, source.testVersion(), source.category(), source.protocol(),
                source.target(), source.endpoint(), source.method(), source.baselineDefinition(), source.positiveControl(),
                source.negativeControl(), source.mutation(), source.sourceContext(), targetContext, source.sourceResource(),
                targetResource, expected, source.expectedEvidence(), source.safetyPolicy(), source.priority(),
                source.selectionReason(), source.configurationSnapshot(), source.dependencies(), source.reproducibilityMetadata(),
                source.estimatedRequestCost(), source.invariants());
    }

    private static SecurityContextFingerprint context(SecurityTest source, String principal, String role, String tenant,
                                                       String resource, String owner) {
        SecurityContextFingerprint value = source.targetContext();
        return new SecurityContextFingerprint(principal == null ? value.principal() : principal,
                role == null ? value.role() : role,
                tenant == null ? value.tenant() : tenant,
                resource == null ? value.resource() : resource,
                owner == null ? value.owner() : owner,
                value.action(), value.workflow(), value.uriClass(), value.uriForm(), value.tokenContext(),
                value.expected(), value.observed(), value.evidenceIds());
    }

    private static SecurityTest withProject(SecurityTest source, String projectId) {
        TargetDescriptor target = new TargetDescriptor(projectId, source.target().targetId(), source.target().scheme(),
                source.target().host(), source.target().port(), source.target().environment(), source.target().authorized(),
                source.target().allowedPathPrefixes(), source.target().allowedMethods());
        return new SecurityTest(source.testId(), source.testVersion(), source.category(), source.protocol(), target,
                source.endpoint(), source.method(), source.baselineDefinition(), source.positiveControl(), source.negativeControl(),
                source.mutation(), source.sourceContext(), source.targetContext(), source.sourceResource(), source.targetResource(),
                source.expectedDecision(), source.expectedEvidence(), source.safetyPolicy(), source.priority(),
                source.selectionReason(), source.configurationSnapshot(), source.dependencies(), source.reproducibilityMetadata(),
                source.estimatedRequestCost(), source.invariants());
    }

    private static ActiveConsent consent() {
        return new ActiveConsent(true, true, true, true, false);
    }

    private static List<BudgetKey> budgetKeys(SecurityTest test) {
        return List.of(
                new BudgetKey(BudgetScope.GLOBAL, "global"),
                new BudgetKey(BudgetScope.PROJECT, test.target().projectId()),
                new BudgetKey(BudgetScope.TARGET, test.target().targetId()),
                new BudgetKey(BudgetScope.ENDPOINT, test.endpoint().endpointId()),
                new BudgetKey(BudgetScope.TEST, test.testId()),
                new BudgetKey(BudgetScope.CONTEXT, test.targetContext().principal() + '|' + test.targetContext().tenant()
                        + '|' + test.targetContext().role()));
    }

    private static List<ConcurrencyKey> concurrencyKeys(SecurityTest test) {
        return List.of(
                new ConcurrencyKey(ConcurrencyScope.GLOBAL, "global"),
                new ConcurrencyKey(ConcurrencyScope.HOST, test.target().host()),
                new ConcurrencyKey(ConcurrencyScope.TARGET, test.target().targetId()),
                new ConcurrencyKey(ConcurrencyScope.ENDPOINT, test.endpoint().endpointId()));
    }

    private static List<String> rateKeys(SecurityTest test) {
        return List.of("host:" + test.target().host(), "target:" + test.target().targetId(),
                "endpoint:" + test.endpoint().endpointId(), "test:" + test.testId());
    }

    private static final String UNKNOWN = "UNKNOWN";
}
