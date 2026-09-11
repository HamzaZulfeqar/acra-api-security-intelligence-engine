package io.acra.core.active.product;

import io.acra.core.active.analysis.ExpectedDecisionCandidate;
import io.acra.core.active.evidence.SafetyAuditLog;
import io.acra.core.active.execution.ExecutionQueue;
import io.acra.core.active.execution.TestExecutionResult;
import io.acra.core.active.execution.TestExecutor;
import io.acra.core.active.execution.TestPriority;
import io.acra.core.active.model.ActiveControl;
import io.acra.core.active.model.ConfigurationSnapshot;
import io.acra.core.active.model.SafetyPolicy;
import io.acra.core.active.model.SelectionMode;
import io.acra.core.active.model.TestProfile;
import io.acra.core.active.model.TestProfileDefinition;
import io.acra.core.active.model.UserMode;
import io.acra.core.active.model.UserModeDefinition;
import io.acra.core.active.planning.ActiveCoverageSnapshot;
import io.acra.core.active.planning.ActiveCoverageTracker;
import io.acra.core.active.planning.PlanningInput;
import io.acra.core.active.planning.PlanningMetrics;
import io.acra.core.active.planning.PlanningResult;
import io.acra.core.active.planning.RequestEfficiencySnapshot;
import io.acra.core.active.planning.TestPlanner;
import io.acra.core.active.safety.ActiveConsent;
import io.acra.core.active.safety.LocalDevelopmentExecutionPolicy;
import io.acra.core.domain.testing.TestState;
import java.time.Clock;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/** Product-level state shared by the active UI and the existing planner, queue and executor. */
public final class ActiveEngineWorkspace {
    private final TestPlanner planner = new TestPlanner();
    private final TestExecutor executor;
    private final SafetyAuditLog audit;
    private final LocalDevelopmentExecutionPolicy localPolicy = new LocalDevelopmentExecutionPolicy();
    private final List<TestExecutionResult> executions = new ArrayList<>();
    private ExecutionQueue queue;
    private UserModeDefinition mode;
    private TestProfileDefinition profile;
    private SelectionMode selectionMode;
    private int requestBudget;
    private int mutationBudget;
    private int concurrency;
    private int requestsPerSecond;
    private PlanningResult planning;
    private ActiveCoverageTracker coverage;

    public ActiveEngineWorkspace(Clock clock, TestExecutor executor) {
        if (clock == null) throw new IllegalArgumentException("clock required");
        this.executor = executor;
        this.audit = new SafetyAuditLog(clock);
        this.queue = new ExecutionQueue(audit);
        configureMode(UserMode.BEGINNER);
    }

    public synchronized void configureMode(UserMode userMode) {
        mode = UserModeDefinition.defaults(userMode);
        profile = mode.profile();
        selectionMode = mode.selectionMode();
        requestBudget = mode.requestBudget();
        mutationBudget = mode.mutationBudget();
        concurrency = mode.concurrency();
        requestsPerSecond = mode.requestsPerSecond();
    }

    public synchronized void configureProfile(TestProfile value) {
        requireControl(ActiveControl.PROFILE);
        profile = TestProfileDefinition.defaults(value);
        requestBudget = Math.min(requestBudget, profile.requestBudget());
        mutationBudget = Math.min(mutationBudget, profile.mutationBudget());
        concurrency = Math.min(concurrency, profile.concurrency());
        requestsPerSecond = Math.min(requestsPerSecond, profile.requestsPerSecond());
    }

    public synchronized void configureSelectionMode(SelectionMode value) {
        requireControl(ActiveControl.SELECTION_MODE);
        if (value == null) throw new IllegalArgumentException("selection mode required");
        selectionMode = value;
    }

    public synchronized void configureLimits(int requests, int mutations, int concurrent, int rate) {
        requireControl(ActiveControl.REQUEST_BUDGET);
        if (requests < 0 || mutations < 0 || concurrent < 1 || rate < 1) {
            throw new IllegalArgumentException("invalid active limits");
        }
        requestBudget = Math.min(requests, profile.requestBudget());
        mutationBudget = Math.min(mutations, profile.mutationBudget());
        concurrency = Math.min(concurrent, profile.concurrency());
        requestsPerSecond = Math.min(rate, profile.requestsPerSecond());
    }

    public synchronized PlanningResult plan(PlanningInput input) {
        PlanningInput configured = configured(input);
        planning = planner.planWithMetrics(configured);
        coverage = new ActiveCoverageTracker(configured, planning);
        queue = new ExecutionQueue(audit);
        for (var test : planning.plan().tests()) {
            int priority = test.priority();
            queue.enqueue(test, new TestPriority(priority, priority, priority, priority, priority, priority, priority));
        }
        executions.clear();
        return planning;
    }

    public synchronized Optional<TestExecutionResult> executeNextLocal(List<ExpectedDecisionCandidate> expected) {
        if (executor == null) throw new IllegalStateException("active executor is not configured");
        var next = queue.startNext();
        if (next.isEmpty()) return Optional.empty();
        var test = next.orElseThrow();
        ActiveConsent consent = localPolicy.consentFor(test);
        TestExecutionResult result = executor.execute(test, consent, expected);
        transition(test.testId(), result);
        executions.add(result);
        if (coverage != null) coverage.record(result);
        return Optional.of(result);
    }

    public synchronized List<io.acra.core.active.execution.QueueEntrySnapshot> stopAll(String reason) {
        if (executor != null) executor.stopAll(reason);
        return queue.stopAll(reason);
    }

    public synchronized UserModeDefinition mode() { return mode; }
    public synchronized TestProfileDefinition profile() { return profile; }
    public synchronized SelectionMode selectionMode() { return selectionMode; }
    public synchronized int requestBudget() { return requestBudget; }
    public synchronized int mutationBudget() { return mutationBudget; }
    public synchronized int concurrency() { return concurrency; }
    public synchronized int requestsPerSecond() { return requestsPerSecond; }
    public synchronized ExecutionQueue queue() { return queue; }
    public synchronized Optional<PlanningResult> planning() { return Optional.ofNullable(planning); }
    public synchronized List<TestExecutionResult> executions() { return List.copyOf(executions); }
    public synchronized List<io.acra.core.active.evidence.SafetyAuditEvent> safetyEvents() { return audit.entries(); }
    public synchronized boolean executionAvailable() { return executor != null; }

    public synchronized ActiveCoverageSnapshot coverage() {
        return coverage == null ? new ActiveCoverageSnapshot(0, 0, 0, 0, 0, 0, Set.of(), Set.of(),
                0, 0, 0, 0, 0, 0, 0, 0) : coverage.snapshot();
    }

    public synchronized RequestEfficiencySnapshot efficiency() {
        return coverage == null ? new RequestEfficiencySnapshot(0, 0, 0, 0, 0) : coverage.efficiency();
    }

    private PlanningInput configured(PlanningInput input) {
        if (input == null) throw new IllegalArgumentException("planning input required");
        Set<io.acra.core.active.model.TestContract> contracts = EnumSet.noneOf(io.acra.core.active.model.TestContract.class);
        contracts.addAll(input.enabledContracts());
        contracts.retainAll(profile.enabledContracts());
        int requests = Math.min(Math.min(input.requestBudget(), requestBudget), profile.requestBudget());
        int mutations = Math.min(Math.min(input.mutationBudget(), mutationBudget), profile.mutationBudget());
        SafetyPolicy original = input.safetyPolicy();
        SafetyPolicy safety = new SafetyPolicy(original.safetyClass(), original.confirmationRequired(),
                original.destructiveConfirmationRequired(), Math.min(original.requestBudget(), requests),
                Math.min(original.mutationBudget(), mutations), original.maxResponseBytes(), original.allowedMethods());
        TreeMap<String, String> values = new TreeMap<>(input.configurationSnapshot().values());
        values.put("userMode", mode.mode().name());
        values.put("profile", profile.profile().name());
        values.put("selectionMode", selectionMode.name());
        values.put("evidenceDetail", mode.evidenceDetail().name());
        values.put("testDepth", Integer.toString(mode.testDepth()));
        values.put("requestBudget", Integer.toString(requests));
        values.put("mutationBudget", Integer.toString(mutations));
        values.put("concurrency", Integer.toString(concurrency));
        values.put("requestsPerSecond", Integer.toString(requestsPerSecond));
        ConfigurationSnapshot configuration = ConfigurationSnapshot.of(values);
        return new PlanningInput(input.planId(), input.apiInventory(), input.securityContextGraph(),
                input.authorizationMatrix(), input.securityContexts(), input.routes(), input.openApi(), input.target(),
                Set.copyOf(contracts), input.riskPriority(), input.contextCoverage(), requests, mutations, safety,
                selectionMode, profile, configuration, input.seeds(), input.createdAt());
    }

    private void transition(String testId, TestExecutionResult result) {
        switch (result.state()) {
            case COMPLETED -> queue.complete(testId);
            case PAUSED -> queue.pause(testId);
            case BLOCKED -> queue.block(testId, reason(result, "blocked"));
            case FAILED -> queue.fail(testId, reason(result, "failed"));
            case CANCELLED -> queue.cancel(testId, reason(result, "cancelled"));
            default -> throw new IllegalStateException("unexpected executor state " + result.state());
        }
    }

    private static String reason(TestExecutionResult result, String fallback) {
        return result.failure() == null ? fallback : result.failure().message();
    }

    private void requireControl(ActiveControl control) {
        if (!mode.controls().contains(control)) {
            throw new IllegalStateException(control + " is unavailable in " + mode.mode() + " mode");
        }
    }
}
