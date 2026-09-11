package io.acra.burp.tests.sprint4;

import io.acra.burp.scope.ScopeController;
import io.acra.burp.traffic.TrafficIntelligencePipeline;
import io.acra.burp.ui.AcraSuiteTab;
import io.acra.core.active.model.SelectionMode;
import io.acra.core.active.model.TestProfile;
import io.acra.core.active.model.UserMode;
import io.acra.core.active.product.ActiveEngineWorkspace;
import java.awt.Component;
import java.awt.Container;
import java.time.Clock;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public final class Sprint4UiTestSuite {
    private static int tests;

    private Sprint4UiTestSuite() { }

    public static void main(String[] arguments) throws Exception {
        System.setProperty("java.awt.headless", "true");
        AtomicReference<AcraSuiteTab> reference = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> reference.set(new AcraSuiteTab(
                new TrafficIntelligencePipeline(20), new ScopeController(),
                new ActiveEngineWorkspace(Clock.systemUTC(), null))));
        AcraSuiteTab tab = reference.get();
        try {
            SwingUtilities.invokeAndWait(() -> verifyViewsAndBindings(tab));
        } finally {
            SwingUtilities.invokeAndWait(tab::stop);
        }
        System.out.println("PASS Sprint4 UI tests=" + tests);
    }

    private static void verifyViewsAndBindings(AcraSuiteTab tab) {
        JTabbedPane tabs = find(tab.component(), JTabbedPane.class, null);
        Set<String> titles = new TreeSet<>();
        for (int index = 0; index < tabs.getTabCount(); index++) titles.add(tabs.getTitleAt(index));
        Set<String> required = Set.of("Test Plan", "Test Queue", "Test Detail", "Execution",
                "Differential Result", "Evidence", "Safety", "Experiment");
        check(titles.containsAll(required), "all S4 views installed in existing suite tab");
        tests += required.size();

        @SuppressWarnings("unchecked")
        JComboBox<UserMode> mode = (JComboBox<UserMode>) find(tab.component(), JComboBox.class, "s4-user-mode");
        @SuppressWarnings("unchecked")
        JComboBox<TestProfile> profile = (JComboBox<TestProfile>) find(tab.component(), JComboBox.class, "s4-profile");
        @SuppressWarnings("unchecked")
        JComboBox<SelectionMode> selection = (JComboBox<SelectionMode>) find(tab.component(), JComboBox.class,
                "s4-selection-mode");
        JSpinner requestBudget = find(tab.component(), JSpinner.class, "s4-request-budget");
        JSpinner mutationBudget = find(tab.component(), JSpinner.class, "s4-mutation-budget");
        JSpinner concurrency = find(tab.component(), JSpinner.class, "s4-concurrency");
        JSpinner rate = find(tab.component(), JSpinner.class, "s4-rate-limit");
        JButton stopAll = find(tab.component(), JButton.class, "s4-stop-all");
        JButton executeNext = find(tab.component(), JButton.class, "s4-execute-next");

        check(tab.activeWorkspace().mode().mode() == UserMode.BEGINNER, "beginner backend default");
        check(!profile.isEnabled() && !selection.isEnabled(), "beginner restrictions reflected in UI");
        check(!executeNext.isEnabled(), "execution unavailable without injected executor");

        mode.setSelectedItem(UserMode.PROFESSIONAL);
        check(tab.activeWorkspace().mode().mode() == UserMode.PROFESSIONAL, "user mode updates backend");
        check(tab.activeWorkspace().profile().profile() == TestProfile.AUTHORIZATION_DIFFERENTIAL,
                "user mode applies real profile");
        check(tab.activeWorkspace().selectionMode() == SelectionMode.HYBRID, "user mode applies selection");
        check(profile.isEnabled() && selection.isEnabled(), "professional controls enabled");

        profile.setSelectedItem(TestProfile.ROUTING_DIFFERENTIAL);
        selection.setSelectedItem(SelectionMode.USER_SELECTED);
        requestBudget.setValue(50);
        mutationBudget.setValue(10);
        concurrency.setValue(1);
        rate.setValue(2);
        check(tab.activeWorkspace().profile().profile() == TestProfile.ROUTING_DIFFERENTIAL,
                "profile control updates planner configuration");
        check(tab.activeWorkspace().selectionMode() == SelectionMode.USER_SELECTED,
                "selection control updates planner configuration");
        check(tab.activeWorkspace().requestBudget() == 50, "request budget updates backend");
        check(tab.activeWorkspace().mutationBudget() == 10, "mutation budget updates backend");
        check(tab.activeWorkspace().concurrency() == 1, "concurrency updates backend");
        check(tab.activeWorkspace().requestsPerSecond() == 2, "rate limit updates backend");

        stopAll.doClick();
        check(!tab.activeWorkspace().safetyEvents().isEmpty(), "STOP ALL updates safety audit");
        JTextArea safety = find(tab.component(), JTextArea.class, "s4-safety-view");
        check(safety.getText().contains("QUEUE_CANCELLED"), "safety view renders backend event");
        JTextArea plan = find(tab.component(), JTextArea.class, "s4-plan-view");
        check(plan.getText().contains("Network dispatch: 0"), "empty plan declares zero dispatch");
        JTextArea experiment = find(tab.component(), JTextArea.class, "s4-experiment-view");
        check(experiment.getText().contains("NOT MEASURED"), "experiment view does not invent metrics");
        tests += 18;
    }

    private static <T extends Component> T find(Component root, Class<T> type, String name) {
        if (type.isInstance(root) && (name == null || root instanceof JComponent component
                && name.equals(component.getName()))) return type.cast(root);
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                T match = find(child, type, name);
                if (match != null) return match;
            }
        }
        if (root == null) throw new IllegalArgumentException("root required");
        return null;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
