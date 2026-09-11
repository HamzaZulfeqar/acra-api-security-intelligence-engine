package io.acra.burp.ui;

import io.acra.core.active.model.ActiveControl;
import io.acra.core.active.model.SelectionMode;
import io.acra.core.active.model.TestProfile;
import io.acra.core.active.model.UserMode;
import io.acra.core.active.product.ActiveEngineWorkspace;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;

/** Swing projection of the existing S4 planner, queue and execution workspace. */
@SuppressWarnings("serial")
public final class ActiveTestingPanel extends JPanel {
    private final ActiveEngineWorkspace workspace;
    private final JComboBox<UserMode> userMode = namedCombo("s4-user-mode", UserMode.values());
    private final JComboBox<TestProfile> profile = namedCombo("s4-profile", TestProfile.values());
    private final JComboBox<SelectionMode> selectionMode = namedCombo("s4-selection-mode", SelectionMode.values());
    private final JSpinner requestBudget = namedSpinner("s4-request-budget", 0, 1_000_000);
    private final JSpinner mutationBudget = namedSpinner("s4-mutation-budget", 0, 1_000_000);
    private final JSpinner concurrency = namedSpinner("s4-concurrency", 1, 1_000);
    private final JSpinner rateLimit = namedSpinner("s4-rate-limit", 1, 1_000_000);
    private final JLabel strategy = namedLabel("s4-strategy");
    private final JLabel categories = namedLabel("s4-category");
    private final JLabel status = namedLabel("s4-ui-status");
    private final JButton stopAll = namedButton("s4-stop-all", "STOP ALL");
    private final JButton executeNext = namedButton("s4-execute-next", "Execute Next");
    private final JTextArea planView = view("s4-plan-view");
    private final JTextArea queueView = view("s4-queue-view");
    private final JTextArea detailView = view("s4-detail-view");
    private final JTextArea executionView = view("s4-execution-view");
    private final JTextArea differentialView = view("s4-differential-view");
    private final JTextArea evidenceView = view("s4-evidence-view");
    private final JTextArea safetyView = view("s4-safety-view");
    private final JTextArea experimentView = view("s4-experiment-view");
    private boolean syncing;

    public ActiveTestingPanel(ActiveEngineWorkspace workspace) {
        super(new BorderLayout());
        if (workspace == null) throw new IllegalArgumentException("active workspace required");
        this.workspace = workspace;
        bindActions();
        synchronizeControls();
    }

    public void install(JTabbedPane tabs) {
        if (tabs == null) throw new IllegalArgumentException("tabs required");
        tabs.addTab("Test Plan", panel(controls(), planView));
        tabs.addTab("Test Queue", panel(queueControls(), queueView));
        tabs.addTab("Test Detail", scroll(detailView));
        tabs.addTab("Execution", scroll(executionView));
        tabs.addTab("Differential Result", scroll(differentialView));
        tabs.addTab("Evidence", scroll(evidenceView));
        tabs.addTab("Safety", scroll(safetyView));
        tabs.addTab("Experiment", scroll(experimentView));
        refresh();
    }

    public void refresh() {
        renderPlan();
        renderQueue();
        renderDetail();
        renderExecutions();
        renderSafety();
        renderExperiment();
    }

    public ActiveEngineWorkspace workspace() {
        return workspace;
    }

    private void bindActions() {
        userMode.addActionListener(event -> {
            if (syncing) return;
            runUiAction(() -> workspace.configureMode((UserMode) userMode.getSelectedItem()));
            synchronizeControls();
            refresh();
        });
        profile.addActionListener(event -> {
            if (syncing) return;
            runUiAction(() -> workspace.configureProfile((TestProfile) profile.getSelectedItem()));
            synchronizeControls();
            refresh();
        });
        selectionMode.addActionListener(event -> {
            if (syncing) return;
            runUiAction(() -> workspace.configureSelectionMode((SelectionMode) selectionMode.getSelectedItem()));
            synchronizeControls();
            refresh();
        });
        requestBudget.addChangeListener(event -> applyLimits());
        mutationBudget.addChangeListener(event -> applyLimits());
        concurrency.addChangeListener(event -> applyLimits());
        rateLimit.addChangeListener(event -> applyLimits());
        stopAll.addActionListener(event -> {
            runUiAction(() -> workspace.stopAll("operator STOP ALL"));
            refresh();
        });
        executeNext.addActionListener(event -> {
            runUiAction(() -> workspace.executeNextLocal(List.of()));
            refresh();
        });
    }

    private void applyLimits() {
        if (syncing) return;
        runUiAction(() -> workspace.configureLimits(number(requestBudget), number(mutationBudget),
                number(concurrency), number(rateLimit)));
        synchronizeControls();
        refresh();
    }

    private void synchronizeControls() {
        syncing = true;
        try {
            var mode = workspace.mode();
            userMode.setSelectedItem(mode.mode());
            profile.setSelectedItem(workspace.profile().profile());
            selectionMode.setSelectedItem(workspace.selectionMode());
            requestBudget.setValue(workspace.requestBudget());
            mutationBudget.setValue(workspace.mutationBudget());
            concurrency.setValue(workspace.concurrency());
            rateLimit.setValue(workspace.requestsPerSecond());
            profile.setEnabled(mode.controls().contains(ActiveControl.PROFILE));
            selectionMode.setEnabled(mode.controls().contains(ActiveControl.SELECTION_MODE));
            requestBudget.setEnabled(mode.controls().contains(ActiveControl.REQUEST_BUDGET));
            mutationBudget.setEnabled(mode.controls().contains(ActiveControl.MUTATION_BUDGET));
            concurrency.setEnabled(mode.controls().contains(ActiveControl.CONCURRENCY));
            rateLimit.setEnabled(mode.controls().contains(ActiveControl.RATE_LIMIT));
            stopAll.setEnabled(mode.controls().contains(ActiveControl.STOP_ALL));
            executeNext.setEnabled(workspace.executionAvailable());
            strategy.setText("Strategy: " + workspace.profile().comparisonModel());
            categories.setText("Categories: " + workspace.profile().enabledContracts());
        } finally {
            syncing = false;
        }
    }

    private JPanel controls() {
        JPanel controls = new JPanel(new GridLayout(0, 2, 6, 4));
        controls.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        controls.add(new JLabel("User mode")); controls.add(userMode);
        controls.add(new JLabel("Profile")); controls.add(profile);
        controls.add(new JLabel("Selection mode")); controls.add(selectionMode);
        controls.add(new JLabel("Request budget")); controls.add(requestBudget);
        controls.add(new JLabel("Mutation budget")); controls.add(mutationBudget);
        controls.add(new JLabel("Concurrency")); controls.add(concurrency);
        controls.add(new JLabel("Rate limit (requests/s)")); controls.add(rateLimit);
        controls.add(strategy); controls.add(categories);
        controls.add(new JLabel("Status")); controls.add(status);
        return controls;
    }

    private JPanel queueControls() {
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING));
        controls.add(executeNext);
        controls.add(stopAll);
        return controls;
    }

    private void renderPlan() {
        StringBuilder text = new StringBuilder();
        text.append("Profile: ").append(workspace.profile().profile().displayName()).append('\n');
        text.append("Selection mode: ").append(workspace.selectionMode()).append('\n');
        text.append("Evidence: ").append(workspace.mode().evidenceDetail()).append('\n');
        text.append("Safety: ").append(workspace.profile().safetyClass()).append('\n');
        workspace.planning().ifPresentOrElse(result -> {
            var metrics = result.metrics();
            text.append("Plan: ").append(result.plan().planId()).append(" fingerprint=")
                    .append(result.plan().fingerprint()).append('\n');
            text.append("Candidates=").append(metrics.candidateTests())
                    .append(" eligible=").append(metrics.eligibleTests())
                    .append(" deduplicated=").append(metrics.deduplicatedTests())
                    .append(" scope-filtered=").append(metrics.scopeFilteredTests())
                    .append(" selection-filtered=").append(metrics.selectionFilteredTests())
                    .append(" budget-filtered=").append(metrics.budgetFilteredTests())
                    .append(" planned=").append(metrics.plannedTests())
                    .append(" requests=").append(metrics.estimatedRequests()).append("\n\n");
            result.plan().tests().forEach(test -> text.append(test.testId()).append(" | ")
                    .append(test.category()).append(" | ").append(test.method()).append(' ')
                    .append(test.endpoint().routeTemplate()).append(" | priority=").append(test.priority()).append('\n'));
        }, () -> text.append("No active test plan. Network dispatch: 0\n"));
        planView.setText(text.toString());
    }

    private void renderQueue() {
        StringBuilder text = new StringBuilder("Test | State | Attempts | Priority | Reason\n");
        workspace.queue().snapshots().forEach(item -> text.append(item.testId()).append(" | ")
                .append(item.state()).append(" | ").append(item.attempts()).append(" | ")
                .append(item.priority().score()).append(" | ").append(item.reason()).append('\n'));
        queueView.setText(text.toString());
    }

    private void renderDetail() {
        StringBuilder text = new StringBuilder();
        workspace.planning().flatMap(result -> result.plan().tests().stream().findFirst()).ifPresentOrElse(test -> {
            text.append("Test: ").append(test.testId()).append('\n');
            text.append("WHY SELECTED\n").append(test.selectionReason()).append("\n\n");
            text.append("WHAT CHANGES\n").append(test.mutation().type()).append(" at ")
                    .append(test.mutation().targetLocation()).append(": ").append(test.mutation().originalValue())
                    .append(" -> ").append(test.mutation().mutatedValue()).append("\n\n");
            text.append("WHAT REMAINS CONSTANT\n").append(test.invariants()).append("\n\n");
            text.append("EXPECTED RESULT\n").append(test.expectedDecision()).append("\n\n");
            text.append("REQUIRED EVIDENCE\n").append(test.expectedEvidence()).append("\n\n");
            text.append("ESTIMATED COST\n").append(test.estimatedRequestCost()).append(" requests\n");
        }, () -> text.append("No selected test."));
        detailView.setText(text.toString());
    }

    private void renderExecutions() {
        StringBuilder execution = new StringBuilder("Execution | Test | State | Failure\n");
        StringBuilder differential = new StringBuilder("Execution | Expected | Observed | Classification\n");
        StringBuilder evidence = new StringBuilder("Execution | Evidence | Stage | Object | Fingerprint\n");
        workspace.executions().forEach(result -> {
            execution.append(result.executionId()).append(" | ").append(result.testId()).append(" | ")
                    .append(result.state()).append(" | ")
                    .append(result.failure() == null ? "" : result.failure().message()).append('\n');
            if (result.observation() != null) {
                var observation = result.observation();
                differential.append(result.executionId()).append(" | ")
                        .append(observation.expectedDecision().decision()).append(" | ")
                        .append(observation.observedDecision()).append(" | ")
                        .append(observation.differences().classification()).append('\n');
            }
            result.evidenceChain().forEach(item -> evidence.append(result.executionId()).append(" | ")
                    .append(item.evidenceId()).append(" | ").append(item.stage()).append(" | ")
                    .append(item.objectId()).append(" | ").append(item.fingerprint()).append('\n'));
        });
        executionView.setText(execution.toString());
        differentialView.setText(differential.toString());
        evidenceView.setText(evidence.toString());
    }

    private void renderSafety() {
        var coverage = workspace.coverage();
        var efficiency = workspace.efficiency();
        StringBuilder text = new StringBuilder();
        text.append("Execution configured: ").append(workspace.executionAvailable()).append('\n');
        text.append("Request budget: ").append(workspace.requestBudget())
                .append(" | Mutation budget: ").append(workspace.mutationBudget())
                .append(" | Concurrency: ").append(workspace.concurrency())
                .append(" | Rate: ").append(workspace.requestsPerSecond()).append("/s\n");
        text.append("Coverage: endpoints ").append(coverage.endpointsTested()).append('/')
                .append(coverage.endpointsEligible()).append(", contexts ").append(coverage.contextsTested())
                .append('/').append(coverage.contextsEligible()).append(", resources ")
                .append(coverage.resourcesTested()).append('/').append(coverage.resourcesEligible()).append('\n');
        text.append("Tests: planned=").append(coverage.testsPlanned()).append(" executed=")
                .append(coverage.testsExecuted()).append(" skipped=").append(coverage.testsSkipped())
                .append(" failed=").append(coverage.testsFailed()).append(" blocked=")
                .append(coverage.testsBlocked()).append(" cancelled=").append(coverage.testsCancelled()).append('\n');
        text.append("Request efficiency: candidates=").append(efficiency.candidateTests())
                .append(" deduplicated=").append(efficiency.deduplicatedTests())
                .append(" scope-filtered=").append(efficiency.scopeFilteredTests())
                .append(" budget-filtered=").append(efficiency.budgetFilteredTests())
                .append(" executed=").append(efficiency.executedTests()).append('\n');
        text.append("Vulnerability coverage: NOT ASSESSED\n\nSafety audit\n");
        workspace.safetyEvents().forEach(event -> text.append(event.timestamp()).append(" | ")
                .append(event.type()).append(" | ").append(event.details()).append('\n'));
        safetyView.setText(text.toString());
    }

    private void renderExperiment() {
        StringBuilder text = new StringBuilder("Test | Version | Engine | Seed | Source references | Configuration\n");
        workspace.planning().ifPresent(result -> result.plan().tests().forEach(test -> {
            var metadata = test.reproducibilityMetadata();
            text.append(test.testId()).append(" | ").append(test.testVersion()).append(" | ")
                    .append(metadata.engineVersion()).append(" | ").append(metadata.deterministicSeed()).append(" | ")
                    .append(metadata.sourceReferences()).append(" | ")
                    .append(test.configurationSnapshot().fingerprint()).append('\n');
        }));
        if (workspace.planning().isEmpty()) text.append("No experiment plan. Metrics: NOT MEASURED\n");
        experimentView.setText(text.toString());
    }

    private void runUiAction(Runnable action) {
        try {
            action.run();
            status.setText("Ready");
        } catch (RuntimeException exception) {
            status.setText(exception.getMessage());
        }
    }

    private static JPanel panel(JPanel north, JTextArea view) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(north, BorderLayout.NORTH);
        panel.add(scroll(view), BorderLayout.CENTER);
        return panel;
    }

    private static JScrollPane scroll(JTextArea area) {
        return new JScrollPane(area);
    }

    private static JTextArea view(String name) {
        JTextArea area = new JTextArea();
        area.setName(name);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        return area;
    }

    private static <T> JComboBox<T> namedCombo(String name, T[] values) {
        JComboBox<T> combo = new JComboBox<>(values);
        combo.setName(name);
        return combo;
    }

    private static JSpinner namedSpinner(String name, int minimum, int maximum) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(minimum, minimum, maximum, 1));
        spinner.setName(name);
        return spinner;
    }

    private static JButton namedButton(String name, String text) {
        JButton button = new JButton(text);
        button.setName(name);
        return button;
    }

    private static JLabel namedLabel(String name) {
        JLabel label = new JLabel();
        label.setName(name);
        return label;
    }

    private static int number(JSpinner spinner) {
        return ((Number) spinner.getValue()).intValue();
    }
}
