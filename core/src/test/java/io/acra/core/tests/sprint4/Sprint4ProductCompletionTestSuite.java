package io.acra.core.tests.sprint4;

import io.acra.core.active.analysis.AuthorizationOutcome;
import io.acra.core.active.analysis.DifferentialClassification;
import io.acra.core.active.analysis.ExpectedDecisionCandidate;
import io.acra.core.active.analysis.ExpectedDecisionCandidateFactory;
import io.acra.core.active.analysis.ExpectedDecisionResolution;
import io.acra.core.active.analysis.ExpectedDecisionResolver;
import io.acra.core.active.analysis.ExpectedDecisionSource;
import io.acra.core.active.analysis.MultiWayDifferentialAnalyzer;
import io.acra.core.active.evidence.ExecutionEvidenceStore;
import io.acra.core.active.evidence.ResponseSnapshot;
import io.acra.core.active.evidence.SafetyAuditLog;
import io.acra.core.active.execution.BackoffAction;
import io.acra.core.active.execution.BackoffPolicy;
import io.acra.core.active.execution.ExecutionErrorType;
import io.acra.core.active.execution.ExecutionQueue;
import io.acra.core.active.execution.HttpTransport;
import io.acra.core.active.execution.TestExecutor;
import io.acra.core.active.execution.TestPriority;
import io.acra.core.active.execution.TransportResult;
import io.acra.core.active.model.ActiveControl;
import io.acra.core.active.model.ConfigurationSnapshot;
import io.acra.core.active.model.EvidenceDetail;
import io.acra.core.active.model.ExecutionEnvironment;
import io.acra.core.active.model.Mutation;
import io.acra.core.active.model.MutationLocation;
import io.acra.core.active.model.MutationType;
import io.acra.core.active.model.SafetyClass;
import io.acra.core.active.model.SecurityTest;
import io.acra.core.active.model.SelectionMode;
import io.acra.core.active.model.TargetDescriptor;
import io.acra.core.active.model.TestContract;
import io.acra.core.active.model.TestProfile;
import io.acra.core.active.model.TestProfileDefinition;
import io.acra.core.active.model.UserMode;
import io.acra.core.active.model.UserModeDefinition;
import io.acra.core.active.planning.ActiveCoverageTracker;
import io.acra.core.active.planning.PlanningInput;
import io.acra.core.active.planning.PlanningResult;
import io.acra.core.active.planning.TestPlanner;
import io.acra.core.active.planning.TestSeed;
import io.acra.core.active.product.ActiveEngineWorkspace;
import io.acra.core.active.research.ExperimentRunMetadata;
import io.acra.core.active.research.ResearchCaseDefinition;
import io.acra.core.active.research.ResearchCaseFamily;
import io.acra.core.active.research.ResearchExecutionRecord;
import io.acra.core.active.research.ResearchGroundTruth;
import io.acra.core.active.research.ResearchMetrics;
import io.acra.core.active.research.ResearchPrediction;
import io.acra.core.active.safety.BudgetKey;
import io.acra.core.active.safety.BudgetScope;
import io.acra.core.active.safety.ConcurrencyKey;
import io.acra.core.active.safety.ConcurrencyScope;
import io.acra.core.active.safety.HierarchicalBudgetManager;
import io.acra.core.active.safety.HierarchicalConcurrencyController;
import io.acra.core.active.safety.KillSwitch;
import io.acra.core.active.safety.LocalDevelopmentExecutionPolicy;
import io.acra.core.active.safety.MutationBudgetTracker;
import io.acra.core.active.safety.MutationValidator;
import io.acra.core.active.safety.RateLimitPolicy;
import io.acra.core.active.safety.ScopedRateLimiter;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.endpoint.Endpoint;
import io.acra.core.domain.http.HttpHeader;
import io.acra.core.domain.testing.TestState;
import io.acra.core.tests.TestSupport;
import java.net.ConnectException;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public final class Sprint4ProductCompletionTestSuite {
    private static int tests;

    private Sprint4ProductCompletionTestSuite() {}

    public static void main(String[] args) {
        testProfilesHaveExecutableConfiguration();
        testSelectionModesDrivePlanner();
        testUserModesDriveWorkspace();
        testQueueLifecycleDependenciesAndRetry();
        testLocalDevelopmentPolicyPreservesBoundaries();
        testBackoffAndOperationalFailures();
        testExpectedDecisionResolutionAndUnknownPolicy();
        testCoverageAndEfficiencyAccounting();
        testConfiguredWorkspaceExecutionPath();
        testResearchInfrastructureAndUndefinedMetrics();
        System.out.println("PASS Sprint4 product completion tests=" + tests);
    }

    private static void testProfilesHaveExecutableConfiguration() {
        Map<TestProfile, String> names = Map.of(
                TestProfile.AUTHORIZATION_DIFFERENTIAL, "Authorization Audit",
                TestProfile.ROUTING_DIFFERENTIAL, "Routing Audit",
                TestProfile.CONTEXT_DIFFERENTIAL, "Context Audit",
                TestProfile.RESEARCH_EXPERIMENT, "Research Differential",
                TestProfile.EXPERT_CONTROLLED, "Expert Custom");
        names.forEach((profile, name) -> {
            TestProfileDefinition definition = TestProfileDefinition.defaults(profile);
            eq(name, profile.displayName(), "profile display name");
            yes(!definition.enabledContracts().isEmpty(), "profile enables real contracts");
            yes(!definition.mutationFamilies().isEmpty(), "profile enables real mutation families");
            yes(definition.requestBudget() > 0 && definition.mutationBudget() > 0, "profile has bounded budgets");
            yes(definition.evidenceDetail().ordinal() >= EvidenceDetail.DETAILED.ordinal(), "profile defines evidence detail");
            eq(SafetyClass.SAFE_READ_ONLY, definition.safetyClass(), "profile safety default remains read-only");
            yes(definition.localAutomaticExecution(), "profile permits automatic authorized local execution");
        });

        TestSeed auth = seed("PROFILE-AUTH", "profile-auth", TestContract.CROSS_USER,
                MutationType.IDENTITY_SUBSTITUTION, true, false, false, List.of());
        TestSeed route = seed("PROFILE-ROUTE", "profile-route", TestContract.ROUTE_EQUIVALENCE,
                MutationType.EQUIVALENT_ROUTE_REPRESENTATION, true, false, false, List.of());
        PlanningInput base = planning(List.of(auth, route), SelectionMode.ALL,
                TestProfileDefinition.defaults(TestProfile.AUTHORIZATION_DIFFERENTIAL), 20, 10);
        PlanningResult authorization = new TestPlanner().planWithMetrics(base);
        PlanningResult routing = new TestPlanner().planWithMetrics(planning(List.of(auth, route), SelectionMode.ALL,
                TestProfileDefinition.defaults(TestProfile.ROUTING_DIFFERENTIAL), 20, 10));
        eq(List.of("PROFILE-AUTH"), authorization.plan().tests().stream().map(SecurityTest::testId).toList(),
                "authorization profile changes planned family");
        eq(List.of("PROFILE-ROUTE"), routing.plan().tests().stream().map(SecurityTest::testId).toList(),
                "routing profile changes planned family");
    }

    private static void testSelectionModesDrivePlanner() {
        List<TestSeed> seeds = List.of(
                seed("SELECT-AUTO", "select-auto", TestContract.CROSS_USER, MutationType.IDENTITY_SUBSTITUTION,
                        true, false, false, List.of()),
                seed("SELECT-USER", "select-user", TestContract.CROSS_USER, MutationType.IDENTITY_SUBSTITUTION,
                        false, true, false, List.of()),
                seed("SELECT-BOTH", "select-both", TestContract.CROSS_USER, MutationType.IDENTITY_SUBSTITUTION,
                        true, true, false, List.of()),
                seed("SELECT-NONE", "select-none", TestContract.CROSS_USER, MutationType.IDENTITY_SUBSTITUTION,
                        false, false, false, List.of()));
        eq(Set.of("SELECT-AUTO", "SELECT-BOTH"), selected(seeds, SelectionMode.AUTOMATIC),
                "automatic selects planner recommendations only");
        eq(Set.of("SELECT-USER", "SELECT-BOTH"), selected(seeds, SelectionMode.USER_SELECTED),
                "user-selected mode selects explicit tests only");
        eq(Set.of("SELECT-AUTO", "SELECT-USER", "SELECT-BOTH"), selected(seeds, SelectionMode.HYBRID),
                "hybrid combines automatic and user constraints");
        eq(Set.of("SELECT-AUTO", "SELECT-USER", "SELECT-BOTH", "SELECT-NONE"), selected(seeds, SelectionMode.ALL),
                "all selects every eligible configured test");
    }

    private static void testUserModesDriveWorkspace() {
        UserModeDefinition beginner = UserModeDefinition.defaults(UserMode.BEGINNER);
        UserModeDefinition professional = UserModeDefinition.defaults(UserMode.PROFESSIONAL);
        UserModeDefinition researcher = UserModeDefinition.defaults(UserMode.RESEARCHER);
        UserModeDefinition expert = UserModeDefinition.defaults(UserMode.EXPERT);
        eq(TestProfile.SAFE_LAB, beginner.profile().profile(), "beginner uses safe lab profile");
        eq(SelectionMode.AUTOMATIC, beginner.selectionMode(), "beginner is automatic");
        no(beginner.controls().contains(ActiveControl.CUSTOM_MUTATION), "beginner hides custom mutation");
        eq(TestProfile.AUTHORIZATION_DIFFERENTIAL, professional.profile().profile(), "professional authorization profile");
        yes(professional.controls().contains(ActiveControl.PROFILE), "professional exposes profile control");
        eq(EvidenceDetail.FORENSIC, researcher.evidenceDetail(), "researcher retains forensic evidence");
        eq(SelectionMode.ALL, researcher.selectionMode(), "researcher can evaluate all eligible cases");
        eq(SelectionMode.USER_SELECTED, expert.selectionMode(), "expert defaults to explicit selection");
        yes(expert.controls().containsAll(EnumSet.allOf(ActiveControl.class)), "expert exposes all controls");
        yes(beginner.requestBudget() < professional.requestBudget()
                        && professional.requestBudget() < researcher.requestBudget(),
                "mode depth changes request limits");

        List<TestSeed> seeds = List.of(
                seed("MODE-AUTO", "mode-auto", TestContract.CROSS_USER, MutationType.IDENTITY_SUBSTITUTION,
                        true, false, false, List.of()),
                seed("MODE-USER", "mode-user", TestContract.CROSS_USER, MutationType.IDENTITY_SUBSTITUTION,
                        false, true, false, List.of()));
        ActiveEngineWorkspace workspace = new ActiveEngineWorkspace(Clock.fixed(Sprint4Fixtures.NOW, ZoneOffset.UTC), null);
        eq(List.of("MODE-AUTO"), workspace.plan(planning(seeds, SelectionMode.ALL,
                TestProfileDefinition.defaults(TestProfile.SAFE_LAB), 100, 100)).plan().tests().stream()
                .map(SecurityTest::testId).toList(), "beginner mode changes actual plan");
        throwsType(IllegalStateException.class, () -> workspace.configureProfile(TestProfile.ROUTING_DIFFERENTIAL),
                "beginner cannot bypass hidden profile control");
        workspace.configureMode(UserMode.EXPERT);
        eq(List.of("MODE-USER"), workspace.plan(planning(seeds, SelectionMode.ALL,
                TestProfileDefinition.defaults(TestProfile.SAFE_LAB), 100, 100)).plan().tests().stream()
                .map(SecurityTest::testId).toList(), "expert explicit-selection mapping changes actual plan");
        eq("EXPERT", workspace.planning().orElseThrow().plan().tests().getFirst()
                .configurationSnapshot().values().get("userMode"), "mode captured in reproducible configuration");
    }

    private static void testQueueLifecycleDependenciesAndRetry() {
        ExecutionQueue queue = new ExecutionQueue(new SafetyAuditLog(Clock.fixed(Sprint4Fixtures.NOW, ZoneOffset.UTC)));
        SecurityTest prerequisite = copyTest(Sprint4Fixtures.test("QUEUE-PREREQ", "queue-prereq"), List.of());
        SecurityTest dependent = copyTest(Sprint4Fixtures.test("QUEUE-DEPEND", "queue-dependent"), List.of("QUEUE-PREREQ"));
        queue.enqueue(dependent, priority(100));
        queue.enqueue(prerequisite, priority(20));
        eq("QUEUE-PREREQ", queue.startNext().orElseThrow().testId(), "ready dependency runs before blocked high priority test");
        eq(TestState.COMPLETED, queue.complete("QUEUE-PREREQ").state(), "prerequisite completes");
        eq("QUEUE-DEPEND", queue.startNext().orElseThrow().testId(), "dependent starts after prerequisite");
        eq(TestState.FAILED, queue.fail("QUEUE-DEPEND", "synthetic failure").state(), "failed state recorded");
        eq(TestState.QUEUED, queue.retry("QUEUE-DEPEND").state(), "failed work retries");
        eq("QUEUE-DEPEND", queue.startNext().orElseThrow().testId(), "retry starts deterministically");
        eq(2, queue.snapshot("QUEUE-DEPEND").attempts(), "retry increments attempt only on start");
        eq(TestState.COMPLETED, queue.complete("QUEUE-DEPEND").state(), "retry completes");
        eq(TestState.QUEUED, queue.rerun("QUEUE-DEPEND").state(), "completed work can rerun");
        eq("QUEUE-DEPEND", queue.startNext().orElseThrow().testId(), "rerun starts");
        eq(TestState.CANCELLED, queue.stop("QUEUE-DEPEND", "operator stop").state(), "single stop cancels running work");

        SecurityTest skipped = Sprint4Fixtures.test("QUEUE-SKIP", "queue-skip");
        queue.enqueue(skipped, priority(30));
        eq(TestState.SKIPPED, queue.skip("QUEUE-SKIP", "not required").state(), "skip terminal state");
        eq(TestState.QUEUED, queue.rerun("QUEUE-SKIP").state(), "skipped work can rerun");
        eq(TestState.PAUSED, queue.pause("QUEUE-SKIP").state(), "queued work pauses");
        eq(TestState.QUEUED, queue.resume("QUEUE-SKIP").state(), "paused work resumes");

        SecurityTest failedDependency = Sprint4Fixtures.test("QUEUE-FAIL-ROOT", "queue-fail-root");
        SecurityTest blockedDependent = copyTest(Sprint4Fixtures.test("QUEUE-BLOCKED", "queue-blocked"),
                List.of("QUEUE-FAIL-ROOT"));
        queue.enqueue(failedDependency, priority(90));
        queue.enqueue(blockedDependent, priority(80));
        eq("QUEUE-FAIL-ROOT", queue.startNext().orElseThrow().testId(), "failure root starts");
        queue.fail("QUEUE-FAIL-ROOT", "root failed");
        queue.startNext();
        eq(TestState.BLOCKED, queue.snapshot("QUEUE-BLOCKED").state(), "failed dependency blocks dependent");
        contains(queue.snapshot("QUEUE-BLOCKED").reason(), "QUEUE-FAIL-ROOT", "dependency blocker is traceable");

        SecurityTest missingDependency = copyTest(Sprint4Fixtures.test("QUEUE-MISSING", "queue-missing"),
                List.of("QUEUE-DOES-NOT-EXIST"));
        queue.enqueue(missingDependency, priority(70));
        queue.startNext();
        eq(TestState.BLOCKED, queue.snapshot("QUEUE-MISSING").state(), "missing dependency blocks work");
        contains(queue.snapshot("QUEUE-MISSING").reason(), "QUEUE-DOES-NOT-EXIST",
                "missing dependency is traceable");
    }

    private static void testLocalDevelopmentPolicyPreservesBoundaries() {
        LocalDevelopmentExecutionPolicy policy = new LocalDevelopmentExecutionPolicy();
        SecurityTest local = Sprint4Fixtures.test("LOCAL-CONSENT", "local-consent");
        yes(policy.consentFor(local).activeTestingEnabled(), "authorized localhost LAB execution is automatically consented");
        yes(policy.consentFor(local).confirmationAcknowledged(), "local confirmation is pre-acknowledged once by policy");
        SecurityTest external = withTarget(local, new TargetDescriptor(Sprint4Fixtures.PROJECT, "external", "https",
                "example.test", 443, ExecutionEnvironment.OUT_OF_SCOPE, true, List.of("/"), local.target().allowedMethods()));
        no(policy.consentFor(external).activeTestingEnabled(), "production external execution remains disabled");
        SecurityTest unauthorized = withTarget(local, new TargetDescriptor(Sprint4Fixtures.PROJECT, "unauthorized", "http",
                "localhost", 8080, ExecutionEnvironment.LAB, false, List.of("/api"), local.target().allowedMethods()));
        no(policy.consentFor(unauthorized).targetAuthorized(), "unauthorized localhost remains disabled");
        Mutation destructive = new Mutation("M-DEST", MutationType.IDENTITY_SUBSTITUTION, MutationLocation.IDENTITY,
                "User-A", "User-B", "A", "B", "destructive fixture", "UNKNOWN", SafetyClass.DESTRUCTIVE, "dest");
        no(policy.consentFor(copyTest(local, List.of(), destructive)).activeTestingEnabled(),
                "destructive mutation never receives automatic local consent");
    }

    private static void testBackoffAndOperationalFailures() {
        BackoffPolicy policy = new BackoffPolicy(1, Duration.ofSeconds(2));
        var retry429 = policy.forResponse(429, List.of(new HttpHeader("Retry-After", "10")), 0);
        eq(Duration.ofSeconds(2), retry429.delay(), "Retry-After is bounded by maximum delay");
        yes(retry429.retryAllowed(), "first 429 retry allowed");
        yes(retry429.actions().contains(BackoffAction.REDUCE_CONCURRENCY), "429 asks caller to reduce concurrency");
        no(policy.forResponse(429, List.of(), 1).retryAllowed(), "429 retries are bounded");
        eq(Duration.ofMillis(250), policy.forResponse(503, List.of(), 0).delay(), "503 uses deterministic initial backoff");
        no(policy.forResponse(200, List.of(), 0).retryAllowed(), "successful response does not retry");

        AtomicInteger sends = new AtomicInteger();
        TestExecutor executor = executor(Sprint4Fixtures.test("CONNECTION-FAIL", "connection-fail"),
                (request, timeout, cancellation) -> {
                    sends.incrementAndGet();
                    throw new ConnectException("synthetic refusal");
                }, new BackoffPolicy(1, Duration.ofMillis(10)));
        var result = executor.execute(Sprint4Fixtures.test("CONNECTION-FAIL", "connection-fail"),
                new LocalDevelopmentExecutionPolicy().consentFor(Sprint4Fixtures.test("CONNECTION-FAIL", "connection-fail")),
                List.of(Sprint4Fixtures.groundTruthDeny()));
        eq(TestState.FAILED, result.state(), "connection failure is operational failure");
        eq(ExecutionErrorType.CONNECTION_ERROR, result.failure().type(), "connection failure normalized");
        eq(2, sends.get(), "connection retry count is bounded");
        eq(null, result.observation(), "operational failure creates no security observation");
    }

    private static void testExpectedDecisionResolutionAndUnknownPolicy() {
        ExpectedDecisionResolver resolver = new ExpectedDecisionResolver();
        ExpectedDecisionCandidateFactory factory = new ExpectedDecisionCandidateFactory();
        List<ExpectedDecisionCandidate> candidates = List.of(
                factory.inferred(AuthorizationDecision.ALLOW, "inferred", List.of("E-INF")),
                factory.validatedMetadata(AuthorizationDecision.ALLOW, "metadata", List.of("E-META")),
                factory.configuredPolicy(AuthorizationDecision.DENY, "policy", List.of("E-POLICY")),
                factory.groundTruth(AuthorizationDecision.DENY, "ground-truth", List.of("E-GT")));
        ExpectedDecisionResolution resolved = resolver.resolve(candidates);
        eq(AuthorizationDecision.DENY, resolved.decision(), "ground truth decision selected");
        eq(ExpectedDecisionSource.ACRA_LAB_GROUND_TRUTH, resolved.source(), "ground truth precedence retained");
        eq(List.of("E-GT"), resolved.evidenceIds(), "selected decision evidence retained");

        ExpectedDecisionResolution conflict = resolver.resolve(List.of(
                factory.configuredPolicy(AuthorizationDecision.ALLOW, "p1", List.of("E1")),
                factory.configuredPolicy(AuthorizationDecision.DENY, "p2", List.of("E2"))));
        yes(conflict.conflict(), "same-precedence policy conflict explicit");
        eq(AuthorizationDecision.UNKNOWN, conflict.decision(), "conflict does not fabricate expected result");
        eq(AuthorizationDecision.UNKNOWN, resolver.resolve(List.of()).decision(), "absent policy remains unknown");

        ResponseSnapshot allow = snapshot("allow", 200, "{\"id\":\"1001\"}");
        ResponseSnapshot deny = snapshot("deny", 200, "{\"success\":false,\"message\":\"Access denied\"}");
        var differential = new MultiWayDifferentialAnalyzer().compare(allow, allow, deny, allow,
                resolver.resolve(List.of()));
        eq(DifferentialClassification.INCONCLUSIVE, differential.classification(),
                "unknown expected policy suspends interpretation");
        eq(AuthorizationOutcome.ALLOW, differential.mutationOutcome(), "observed outcome remains separately recorded");
    }

    private static void testCoverageAndEfficiencyAccounting() {
        TestSeed selected = seed("METRIC-SELECTED", "metric-selected", TestContract.CROSS_USER,
                MutationType.IDENTITY_SUBSTITUTION, true, false, false, List.of());
        TestSeed duplicate = seed("METRIC-DUP", "metric-selected", TestContract.CROSS_USER,
                MutationType.IDENTITY_SUBSTITUTION, true, false, false, List.of());
        TestSeed budget = seed("METRIC-BUDGET", "metric-budget", TestContract.CROSS_USER,
                MutationType.IDENTITY_SUBSTITUTION, true, false, false, List.of());
        Endpoint absent = new Endpoint("EP-ABSENT", Sprint4Fixtures.endpoint().method(), "/absent", "/absent",
                "/absent", "localhost", "v1", List.of());
        TestSeed scoped = seed("METRIC-SCOPE", "metric-scope", TestContract.CROSS_USER,
                MutationType.IDENTITY_SUBSTITUTION, true, false, false, List.of(), absent);
        PlanningInput input = planning(List.of(selected, duplicate, budget, scoped), SelectionMode.ALL,
                TestProfileDefinition.defaults(TestProfile.SAFE_LAB), 4, 1);
        PlanningResult result = new TestPlanner().planWithMetrics(input);
        eq(4, result.metrics().candidateTests(), "candidate count recorded");
        eq(3, result.metrics().eligibleTests(), "eligible count recorded");
        eq(1, result.metrics().deduplicatedTests(), "deduplicated count recorded");
        eq(1, result.metrics().scopeFilteredTests(), "scope-filtered count recorded");
        eq(1, result.metrics().budgetFilteredTests(), "budget-filtered count recorded");
        eq(1, result.metrics().plannedTests(), "planned count recorded");
        eq(4, result.metrics().estimatedRequests(), "planned request cost recorded");

        ActiveCoverageTracker tracker = new ActiveCoverageTracker(input, result);
        var before = tracker.snapshot();
        eq(1, before.endpointsEligible(), "eligible endpoint coverage recorded");
        eq(1, before.endpointsTested(), "tested endpoint coverage recorded");
        eq(2, before.contextsEligible(), "eligible contexts recorded");
        eq(2, before.contextsTested(), "tested contexts recorded");
        eq(Set.of(MutationType.IDENTITY_SUBSTITUTION), before.mutationCategoriesTested(),
                "mutation-category coverage recorded");
        eq(0, tracker.efficiency().executedTests(), "execution metric starts at zero, not invented");

        SecurityTest planned = result.plan().tests().getFirst();
        TestExecutor executor = executor(planned, successTransport(), new BackoffPolicy(0, Duration.ofMillis(10)));
        var execution = executor.execute(planned, new LocalDevelopmentExecutionPolicy().consentFor(planned),
                List.of(Sprint4Fixtures.groundTruthDeny()));
        yes(tracker.record(execution), "fresh execution recorded once");
        no(tracker.record(execution), "duplicate execution metric rejected");
        eq(1, tracker.snapshot().testsExecuted(), "executed test coverage recorded");
        eq(1, tracker.efficiency().executedTests(), "request efficiency includes actual execution");
    }

    private static void testResearchInfrastructureAndUndefinedMetrics() {
        ResearchCaseDefinition definition = new ResearchCaseDefinition("FP-PUBLIC-001",
                ResearchCaseFamily.FALSE_POSITIVE_FIXTURE, ResearchGroundTruth.NEGATIVE,
                Sprint4Fixtures.configuration(), Sprint4Fixtures.reproducibility(),
                List.of("ground-truth", "observation", "evidence"));
        eq("FP-PUBLIC-001", definition.caseId(), "research case identity retained");
        eq(ResearchGroundTruth.NEGATIVE, definition.groundTruth(), "independent binary ground truth retained");
        ExperimentRunMetadata metadata = new ExperimentRunMetadata("EXP-DIFF-001", "S4-SYNTHETIC-RESEARCH-V1",
                "ACRA-LAB-S4", 404L, List.of("T1", "T2"), List.of("M1", "M2"),
                Sprint4Fixtures.configuration(), List.of("EXEC-1"), Sprint4Fixtures.NOW);
        eq(List.of("T1", "T2"), metadata.testOrdering(), "deterministic test order recorded");
        eq(404L, metadata.seed(), "research seed recorded");

        List<ResearchExecutionRecord> labelled = List.of(
                record("TP", ResearchGroundTruth.POSITIVE, ResearchPrediction.POSITIVE),
                record("TN", ResearchGroundTruth.NEGATIVE, ResearchPrediction.NEGATIVE),
                record("FP", ResearchGroundTruth.NEGATIVE, ResearchPrediction.POSITIVE),
                record("FN", ResearchGroundTruth.POSITIVE, ResearchPrediction.NEGATIVE));
        ResearchMetrics metrics = ResearchMetrics.from(labelled);
        eq(1, metrics.truePositive(), "TP counted");
        eq(1, metrics.trueNegative(), "TN counted");
        eq(1, metrics.falsePositive(), "FP counted");
        eq(1, metrics.falseNegative(), "FN counted");
        eq(OptionalDouble.of(0.5), metrics.precision(), "precision calculated only from labelled records");
        eq(OptionalDouble.of(0.5), metrics.recall(), "recall calculated only from labelled records");
        eq(OptionalDouble.of(0.5), metrics.f1(), "F1 calculated only from labelled records");
        ResearchMetrics undefined = ResearchMetrics.from(List.of());
        yes(undefined.precision().isEmpty() && undefined.recall().isEmpty() && undefined.f1().isEmpty(),
                "undefined metrics remain N/A");
    }

    private static void testConfiguredWorkspaceExecutionPath() {
        TestSeed seed = seed("WORKSPACE-EXEC", "workspace-exec", TestContract.CROSS_USER,
                MutationType.IDENTITY_SUBSTITUTION, true, false, false, List.of());
        PlanningInput input = planning(List.of(seed), SelectionMode.ALL,
                TestProfileDefinition.defaults(TestProfile.SAFE_LAB), 20, 5);
        SecurityTest expectedTest = new TestPlanner().planWithMetrics(planning(List.of(seed), SelectionMode.AUTOMATIC,
                TestProfileDefinition.defaults(TestProfile.SAFE_LAB), 20, 5)).plan().tests().getFirst();
        ActiveEngineWorkspace workspace = new ActiveEngineWorkspace(
                Clock.fixed(Sprint4Fixtures.NOW, ZoneOffset.UTC),
                executor(expectedTest, successTransport(), new BackoffPolicy(0, Duration.ofMillis(10))));
        eq(1, workspace.plan(input).plan().tests().size(), "configured workspace creates active plan");
        eq(TestState.QUEUED, workspace.queue().snapshot("WORKSPACE-EXEC").state(),
                "configured workspace enqueues planned test");
        var result = workspace.executeNextLocal(List.of(Sprint4Fixtures.groundTruthDeny())).orElseThrow();
        eq(TestState.COMPLETED, result.state(), "configured workspace executes through existing executor");
        eq(TestState.COMPLETED, workspace.queue().snapshot("WORKSPACE-EXEC").state(),
                "configured workspace maps executor result to queue");
        eq(1, workspace.coverage().testsExecuted(), "configured workspace records execution coverage");
    }

    private static Set<String> selected(List<TestSeed> seeds, SelectionMode mode) {
        return Set.copyOf(new TestPlanner().plan(planning(seeds, mode,
                TestProfileDefinition.defaults(TestProfile.SAFE_LAB), 100, 100)).tests().stream()
                .map(SecurityTest::testId).toList());
    }

    private static PlanningInput planning(List<TestSeed> seeds, SelectionMode mode, TestProfileDefinition profile,
                                          int requests, int mutations) {
        PlanningInput original = Sprint4Fixtures.planningInput(List.of());
        return new PlanningInput("PLAN-" + mode + '-' + profile.profile(), original.apiInventory(),
                original.securityContextGraph(), original.authorizationMatrix(), original.securityContexts(),
                original.routes(), original.openApi(), original.target(), EnumSet.allOf(TestContract.class),
                original.riskPriority(), original.contextCoverage(), requests, mutations, original.safetyPolicy(),
                mode, profile, original.configurationSnapshot(), seeds, original.createdAt());
    }

    private static TestSeed seed(String id, String key, TestContract contract, MutationType type,
                                 boolean recommended, boolean selected, boolean excluded, List<String> dependencies) {
        return seed(id, key, contract, type, recommended, selected, excluded, dependencies, Sprint4Fixtures.endpoint());
    }

    private static TestSeed seed(String id, String key, TestContract contract, MutationType type,
                                 boolean recommended, boolean selected, boolean excluded, List<String> dependencies,
                                 Endpoint endpoint) {
        boolean route = type == MutationType.EQUIVALENT_ROUTE_REPRESENTATION;
        Mutation mutation = new Mutation("M-" + id, type,
                route ? MutationLocation.URI_REPRESENTATION : MutationLocation.IDENTITY,
                route ? "/api/v1/documents/1001" : "User-A",
                route ? "/api/v1/documents/%31%30%30%31" : "User-B",
                "source", "target", "controlled fixture",
                "expected difference", SafetyClass.SAFE_READ_ONLY, key);
        return new TestSeed(id, "1", contract, endpoint, Sprint4Fixtures.baseline(), Sprint4Fixtures.positive(),
                Sprint4Fixtures.negative(), mutation, Sprint4Fixtures.sourceContext(), Sprint4Fixtures.targetContext(),
                Sprint4Fixtures.resource(), Sprint4Fixtures.resource(), AuthorizationDecision.DENY,
                List.of("GT-S4"), dependencies, List.of("single difference"), 4,
                recommended, selected, excluded, "phase3 selection fixture");
    }

    private static SecurityTest copyTest(SecurityTest source, List<String> dependencies) {
        return copyTest(source, dependencies, source.mutation());
    }

    private static SecurityTest copyTest(SecurityTest source, List<String> dependencies, Mutation mutation) {
        return new SecurityTest(source.testId(), source.testVersion(), source.category(), source.protocol(), source.target(),
                source.endpoint(), source.method(), source.baselineDefinition(), source.positiveControl(),
                source.negativeControl(), mutation, source.sourceContext(), source.targetContext(), source.sourceResource(),
                source.targetResource(), source.expectedDecision(), source.expectedEvidence(), source.safetyPolicy(),
                source.priority(), source.selectionReason(), source.configurationSnapshot(), dependencies,
                source.reproducibilityMetadata(), source.estimatedRequestCost(), source.invariants());
    }

    private static SecurityTest withTarget(SecurityTest source, TargetDescriptor target) {
        return new SecurityTest(source.testId(), source.testVersion(), source.category(), source.protocol(), target,
                source.endpoint(), source.method(), source.baselineDefinition(), source.positiveControl(),
                source.negativeControl(), source.mutation(), source.sourceContext(), source.targetContext(),
                source.sourceResource(), source.targetResource(), source.expectedDecision(), source.expectedEvidence(),
                source.safetyPolicy(), source.priority(), source.selectionReason(), source.configurationSnapshot(),
                source.dependencies(), source.reproducibilityMetadata(), source.estimatedRequestCost(), source.invariants());
    }

    private static TestPriority priority(int value) {
        return new TestPriority(value, value, value, value, value, value, value);
    }

    private static TestExecutor executor(SecurityTest test, HttpTransport transport, BackoffPolicy backoff) {
        Clock clock = Clock.fixed(Sprint4Fixtures.NOW, ZoneOffset.UTC);
        SafetyAuditLog audit = new SafetyAuditLog(clock);
        KillSwitch kill = new KillSwitch(audit);
        kill.reset(true, "phase3 fixture");
        HierarchicalBudgetManager budgets = new HierarchicalBudgetManager(audit);
        for (BudgetKey key : budgetKeys(test)) budgets.configure(key, 100);
        HierarchicalConcurrencyController concurrency = new HierarchicalConcurrencyController();
        for (ConcurrencyKey key : concurrencyKeys(test)) concurrency.configure(key, 2);
        ScopedRateLimiter rate = new ScopedRateLimiter();
        for (String key : rateKeys(test)) rate.configure(key, new RateLimitPolicy(1000, 5000, 1000, 0));
        MutationValidator validator = new MutationValidator(Sprint4Fixtures.PROJECT, Set.of(ExecutionEnvironment.LAB),
                kill, budgets, concurrency, rate, audit);
        return new TestExecutor(clock, Duration.ofSeconds(2), transport, ignored -> { }, validator, budgets,
                concurrency, rate, backoff, kill, new MutationBudgetTracker(100), audit,
                new ExecutionEvidenceStore(clock));
    }

    private static HttpTransport successTransport() {
        return (request, timeout, cancellation) -> {
            cancellation.throwIfCancelled();
            var response = switch (request.kind()) {
                case BASELINE, POSITIVE_CONTROL -> Sprint4Fixtures.response(200, "{\"id\":\"1001\"}");
                case NEGATIVE_CONTROL, MUTATION -> Sprint4Fixtures.response(200,
                        "{\"success\":false,\"message\":\"Access denied\"}");
            };
            return new TransportResult(response, Map.of(), Duration.ofMillis(1));
        };
    }

    private static List<BudgetKey> budgetKeys(SecurityTest test) {
        return List.of(new BudgetKey(BudgetScope.GLOBAL, "global"),
                new BudgetKey(BudgetScope.PROJECT, test.target().projectId()),
                new BudgetKey(BudgetScope.TARGET, test.target().targetId()),
                new BudgetKey(BudgetScope.ENDPOINT, test.endpoint().endpointId()),
                new BudgetKey(BudgetScope.TEST, test.testId()),
                new BudgetKey(BudgetScope.CONTEXT, test.targetContext().principal() + '|' + test.targetContext().tenant()
                        + '|' + test.targetContext().role()));
    }

    private static List<ConcurrencyKey> concurrencyKeys(SecurityTest test) {
        return List.of(new ConcurrencyKey(ConcurrencyScope.GLOBAL, "global"),
                new ConcurrencyKey(ConcurrencyScope.HOST, test.target().host()),
                new ConcurrencyKey(ConcurrencyScope.TARGET, test.target().targetId()),
                new ConcurrencyKey(ConcurrencyScope.ENDPOINT, test.endpoint().endpointId()));
    }

    private static List<String> rateKeys(SecurityTest test) {
        return List.of("host:" + test.target().host(), "target:" + test.target().targetId(),
                "endpoint:" + test.endpoint().endpointId(), "test:" + test.testId());
    }

    private static ResponseSnapshot snapshot(String id, int status, String body) {
        return ResponseSnapshot.capture(id, id + "-request", Sprint4Fixtures.response(status, body), Map.of(),
                Duration.ofMillis(1), Sprint4Fixtures.NOW);
    }

    private static ResearchExecutionRecord record(String id, ResearchGroundTruth truth, ResearchPrediction prediction) {
        return new ResearchExecutionRecord(id, "EXEC-" + id, "TEST-" + id, "OBS-" + id,
                List.of("EVIDENCE-" + id), truth, prediction);
    }

    private static void yes(boolean value, String message) { tests++; TestSupport.assertTrue(value, message); }
    private static void no(boolean value, String message) { tests++; TestSupport.assertFalse(value, message); }
    private static void eq(Object expected, Object actual, String message) { tests++; TestSupport.assertEquals(expected, actual, message); }
    private static void contains(String value, String expected, String message) { tests++; TestSupport.assertContains(value, expected, message); }
    private static <T extends Throwable> void throwsType(Class<T> type, Runnable operation, String message) {
        tests++;
        TestSupport.assertThrows(type, operation, message);
    }
}
