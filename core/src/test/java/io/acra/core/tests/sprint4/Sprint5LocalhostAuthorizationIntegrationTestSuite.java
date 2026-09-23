package io.acra.core.tests.sprint4;

import io.acra.core.active.evidence.ExecutionEvidenceStore;
import io.acra.core.active.evidence.SafetyAuditLog;
import io.acra.core.active.execution.BackoffPolicy;
import io.acra.core.active.execution.DelayController;
import io.acra.core.active.execution.LocalhostHttpTransport;
import io.acra.core.active.execution.TestExecutor;
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
import io.acra.core.domain.authorization.AuthorizationAnalysisDimension;
import io.acra.core.domain.authorization.AuthorizationAnalysisRequest;
import io.acra.core.domain.finding.AuthorizationImpact;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.ImpactLevel;
import io.acra.core.domain.finding.SeverityLevel;
import io.acra.core.graph.SecurityContextGraph;
import io.acra.core.engine.AuthorizationAnalysisOrchestrator;
import io.acra.core.tests.TestSupport;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * Controlled localhost integration of the S4 execution path into the S5
 * evidence-bound authorization analysis pipeline. This suite never leaves ACRA-Lab.
 */
public final class Sprint5LocalhostAuthorizationIntegrationTestSuite {
    private record Harness(TestExecutor executor, ExecutionEvidenceStore evidence) { }

    private Sprint5LocalhostAuthorizationIntegrationTestSuite() { }

    public static void main(String[] args) {
        int assertions = vulnerableControl() + secureControl();
        System.out.println("SPRINT5_LOCALHOST_AUTHORIZATION PASS assertions=" + assertions);
    }

    private static int vulnerableControl() {
        var test = Sprint4LocalhostFixtures.resourceExperiment(false);
        Harness harness = harness(test);
        var execution = harness.executor().execute(
                test, consent(), List.of(Sprint4LocalhostFixtures.groundTruthDeny()));

        TestSupport.assertTrue(execution.observation() != null,
                "vulnerable laboratory execution must produce an observation");

        AuthorizationAnalysisRequest request = new AuthorizationAnalysisRequest(
                execution.observation(),
                Sprint4LocalhostFixtures.PROJECT,
                "GET /api/v1/s4/documents/{id}",
                Set.of(AuthorizationAnalysisDimension.OBJECT),
                null,
                null,
                "",
                "",
                false,
                false,
                null,
                "",
                null,
                "",
                new AuthorizationImpact(ImpactLevel.HIGH, false, false, true,
                        false, false, false, execution.observation().evidenceIds()));

        var analysis = new AuthorizationAnalysisOrchestrator(harness.evidence()).analyze(request);
        TestSupport.assertTrue(analysis.normalization().valid(),
                "live S4 evidence must authenticate before S5 analysis");
        TestSupport.assertEquals(FindingCandidateState.SUPPORTED, analysis.candidate().state(),
                "vulnerable synthetic object mismatch supports a BOLA candidate");
        TestSupport.assertTrue(analysis.candidate().category().contains("BOLA"),
                "object laboratory case records BOLA support");
        TestSupport.assertFalse(analysis.candidate().category().contains("BFLA"),
                "object laboratory case is not over-classified as BFLA");
        TestSupport.assertEquals(SeverityLevel.HIGH, analysis.severity().level(),
                "explicit HIGH impact remains independent from candidate confidence");
        return 6;
    }

    private static int secureControl() {
        var test = Sprint4LocalhostFixtures.resourceExperiment(true);
        Harness harness = harness(test);
        var execution = harness.executor().execute(
                test, consent(), List.of(Sprint4LocalhostFixtures.groundTruthDeny()));

        TestSupport.assertTrue(execution.observation() != null,
                "secure laboratory execution must produce an observation");

        AuthorizationAnalysisRequest request = new AuthorizationAnalysisRequest(
                execution.observation(),
                Sprint4LocalhostFixtures.PROJECT,
                "GET /api/v1/s4/documents/{id}",
                Set.of(AuthorizationAnalysisDimension.OBJECT),
                null,
                null,
                "",
                "",
                false,
                false,
                null,
                "",
                null,
                "",
                new AuthorizationImpact(ImpactLevel.HIGH, false, false, true,
                        false, false, false, execution.observation().evidenceIds()));

        var analysis = new AuthorizationAnalysisOrchestrator(harness.evidence()).analyze(request);
        TestSupport.assertTrue(analysis.normalization().valid(),
                "secure control evidence must authenticate before S5 analysis");
        TestSupport.assertEquals(FindingCandidateState.REJECTED, analysis.candidate().state(),
                "secure control must not produce a supported candidate");
        TestSupport.assertEquals(SeverityLevel.UNKNOWN, analysis.severity().level(),
                "rejected candidate has no authorization severity");
        return 4;
    }

    private static Harness harness(io.acra.core.active.model.SecurityTest test) {
        Clock clock = Clock.systemUTC();
        SafetyAuditLog audit = new SafetyAuditLog(clock);
        KillSwitch kill = new KillSwitch(audit);
        kill.reset(true, "authorized Sprint 5 localhost laboratory validation");

        HierarchicalBudgetManager budgets = new HierarchicalBudgetManager(audit);
        for (BudgetKey key : budgetKeys(test)) budgets.configure(key, 50);

        HierarchicalConcurrencyController concurrency = new HierarchicalConcurrencyController();
        for (ConcurrencyKey key : concurrencyKeys(test)) concurrency.configure(key, 2);

        ScopedRateLimiter rate = new ScopedRateLimiter();
        RateLimitPolicy policy = new RateLimitPolicy(1000, 5000, 1000, 0);
        for (String key : rateKeys(test)) rate.configure(key, policy);

        MutationBudgetTracker mutationBudget = new MutationBudgetTracker(10);
        MutationValidator validator = new MutationValidator(
                Sprint4LocalhostFixtures.PROJECT,
                Set.of(io.acra.core.active.model.ExecutionEnvironment.LAB),
                kill, budgets, concurrency, rate, audit);

        ExecutionEvidenceStore evidence = new ExecutionEvidenceStore(
                clock, Sprint4LocalhostFixtures.PROJECT);
        SecurityContextGraph graph = new SecurityContextGraph();
        DelayController noDelay = duration -> { };

        TestExecutor executor = new TestExecutor(
                clock,
                Duration.ofSeconds(2),
                new LocalhostHttpTransport(test.target()),
                noDelay,
                validator,
                budgets,
                concurrency,
                rate,
                new BackoffPolicy(0, Duration.ofMillis(25)),
                kill,
                mutationBudget,
                audit,
                evidence,
                graph,
                test.target().projectId());

        return new Harness(executor, evidence);
    }

    private static ActiveConsent consent() {
        return new ActiveConsent(true, true, true, true, false);
    }

    private static List<BudgetKey> budgetKeys(io.acra.core.active.model.SecurityTest test) {
        return List.of(
                new BudgetKey(BudgetScope.GLOBAL, "global"),
                new BudgetKey(BudgetScope.PROJECT, test.target().projectId()),
                new BudgetKey(BudgetScope.TARGET, test.target().targetId()),
                new BudgetKey(BudgetScope.ENDPOINT, test.endpoint().endpointId()),
                new BudgetKey(BudgetScope.TEST, test.testId()),
                new BudgetKey(BudgetScope.CONTEXT,
                        test.targetContext().principal() + "|" + test.targetContext().tenant()
                                + "|" + test.targetContext().role()));
    }

    private static List<ConcurrencyKey> concurrencyKeys(io.acra.core.active.model.SecurityTest test) {
        return List.of(
                new ConcurrencyKey(ConcurrencyScope.GLOBAL, "global"),
                new ConcurrencyKey(ConcurrencyScope.HOST, test.target().host()),
                new ConcurrencyKey(ConcurrencyScope.TARGET, test.target().targetId()),
                new ConcurrencyKey(ConcurrencyScope.ENDPOINT, test.endpoint().endpointId()));
    }

    private static List<String> rateKeys(io.acra.core.active.model.SecurityTest test) {
        return List.of(
                "host:" + test.target().host(),
                "target:" + test.target().targetId(),
                "endpoint:" + test.endpoint().endpointId(),
                "test:" + test.testId());
    }
}
