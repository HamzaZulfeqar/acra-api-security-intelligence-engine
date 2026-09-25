package io.acra.burp.tests.sprint7;

import io.acra.burp.scope.ScopeController;
import io.acra.burp.traffic.TrafficIntelligencePipeline;
import io.acra.burp.ui.AcraSuiteTab;
import io.acra.core.active.product.ActiveEngineWorkspace;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.AuthorizationRuleEffect;
import io.acra.core.domain.authorization.PolicyResolutionState;
import io.acra.core.domain.workflow.WorkflowAuthorizationResolution;
import io.acra.core.domain.workflow.WorkflowPolicySnapshot;
import io.acra.core.domain.workflow.WorkflowTransitionCoverageEntry;
import io.acra.core.domain.workflow.WorkflowTransitionCoverageMatrix;
import io.acra.core.domain.workflow.WorkflowTransitionRule;
import io.acra.core.product.authorization.S6AuthorizationWorkspace;
import io.acra.core.product.workflow.S7WorkflowWorkspace;
import java.awt.Component;
import java.awt.Container;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public final class Sprint7WorkflowUiTestSuite {
    private static final Instant NOW = Instant.parse("2026-09-24T05:30:00Z");
    private static int assertions;

    private Sprint7WorkflowUiTestSuite() { }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        S7WorkflowWorkspace workflow = fixtureWorkspace();
        AtomicReference<AcraSuiteTab> reference = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> reference.set(new AcraSuiteTab(
                new TrafficIntelligencePipeline(20),
                new ScopeController(),
                new ActiveEngineWorkspace(Clock.systemUTC(), null),
                new S6AuthorizationWorkspace(),
                workflow)));
        AcraSuiteTab tab = reference.get();
        try {
            SwingUtilities.invokeAndWait(() -> verify(tab));
        } finally {
            SwingUtilities.invokeAndWait(tab::stop);
        }
        System.out.println("SPRINT7_WORKFLOW_UI PASS assertions=" + assertions);
    }

    private static void verify(AcraSuiteTab tab) {
        JTabbedPane top = find(tab.component(), JTabbedPane.class, null);
        check(indexOf(top, "Workflow") >= 0, "Workflow top-level tab installed");
        assertions++;

        JTabbedPane workflowTabs = find(tab.component(), JTabbedPane.class, "s7-workflow-tabs");
        for (String title : Set.of("Overview", "Workflow Map", "Transition Matrix", "Policy Conflicts", "Coverage", "Report", "JSON Export")) {
            check(indexOf(workflowTabs, title) >= 0, "workflow sub-tab installed: " + title);
            assertions++;
        }

        JTextArea overview = find(tab.component(), JTextArea.class, "s7-workflow-overview");
        check(overview.getText().contains("s7-ui-policy"), "overview renders workflow policy");
        assertions++;
        check(overview.getText().contains("Candidate != confirmed vulnerability"),
                "overview preserves candidate boundary");
        assertions++;
        check(overview.getText().contains("Coverage != vulnerability severity"),
                "overview separates coverage from risk/severity");
        assertions++;

        check(rows(tab, "s7-workflow-map-table") == 2, "workflow map renders two explicit rules");
        assertions++;
        check(rows(tab, "s7-transition-matrix-table") == 2, "transition matrix renders two resolutions");
        assertions++;
        check(rows(tab, "s7-workflow-conflicts-table") == 1, "conflict view renders only conflicting resolution");
        assertions++;
        check(rows(tab, "s7-workflow-coverage-table") == 2, "coverage table renders deterministic contexts");
        assertions++;
        check(tab.workflowWorkspace().snapshot().coverageSummary().observedContexts() == 1,
                "workflow workspace exposes one observed coverage context");
        assertions++;

        JTextArea report = find(tab.component(), JTextArea.class, "s7-workflow-report-view");
        check(report.getText().contains("ACRA Sprint 7 Workflow Authorization Report"),
                "workflow report view renders canonical Markdown");
        assertions++;
        check(report.getText().contains("Confirmed findings: 0"),
                "workflow report view preserves review-only candidate boundary");
        assertions++;

        JTextArea json = find(tab.component(), JTextArea.class, "s7-workflow-json-export-view");
        check(json.getText().contains("s7-workflow-report-v1"),
                "workflow JSON export view renders canonical report version");
        assertions++;
        check(json.getText().contains("\"confirmedFindingCount\":0"),
                "workflow JSON export does not auto-promote findings");
        assertions++;
    }

    private static S7WorkflowWorkspace fixtureWorkspace() {
        S7WorkflowWorkspace workspace = new S7WorkflowWorkspace();
        WorkflowPolicySnapshot policy = WorkflowPolicySnapshot.create(
                "s7-ui-policy",
                "1",
                "controlled-ui-fixture",
                List.of(
                        new WorkflowTransitionRule(
                                "submit-rule", "document-approval", "DRAFT", "SUBMITTED", "SUBMIT",
                                "tenant-a", List.of("author"), false, false, false, false, "",
                                AuthorizationRuleEffect.ALLOW, 10, "explicit-ui-fixture", List.of("e-submit")),
                        new WorkflowTransitionRule(
                                "approve-deny", "document-approval", "SUBMITTED", "APPROVED", "APPROVE",
                                "tenant-a", List.of("approver"), true, true, false, false, "",
                                AuthorizationRuleEffect.DENY, null, "", List.of("e-approve"))),
                List.of(),
                List.of("e-policy"),
                AuthorizationDecision.DENY,
                NOW);
        workspace.loadPolicy(policy);

        WorkflowAuthorizationResolution resolved = resolution(
                "resolution-observed", policy.fingerprint(), "SUBMIT", "DRAFT", "SUBMITTED",
                AuthorizationDecision.ALLOW, PolicyResolutionState.RESOLVED_ALLOW, List.of("submit-rule"));
        WorkflowAuthorizationResolution conflict = resolution(
                "resolution-conflict", policy.fingerprint(), "APPROVE", "SUBMITTED", "APPROVED",
                AuthorizationDecision.UNKNOWN, PolicyResolutionState.CONFLICTING, List.of("approve-deny"));
        workspace.recordResolution(resolved);
        workspace.recordResolution(conflict);

        WorkflowTransitionCoverageMatrix matrix = new WorkflowTransitionCoverageMatrix();
        matrix.put(WorkflowTransitionCoverageEntry.from(resolved)
                .withPlannedTest("S7-UI-TEST-1")
                .withExecution("S7-UI-EXEC-1", "S7-UI-OBS-1", List.of("e-observation")));
        matrix.put(WorkflowTransitionCoverageEntry.from(conflict));
        workspace.replaceCoverage(matrix);
        return workspace;
    }

    private static WorkflowAuthorizationResolution resolution(
            String id,
            String policyFingerprint,
            String action,
            String from,
            String to,
            AuthorizationDecision expected,
            PolicyResolutionState state,
            List<String> rules) {
        return new WorkflowAuthorizationResolution(
                id, policyFingerprint, "document-approval", "author-a", "tenant-a", "document-1",
                action, from, to, rules, List.of(), List.of(), expected, AuthorizationDecision.UNKNOWN,
                state, List.of("e-policy", "e-" + id),
                state == PolicyResolutionState.CONFLICTING ? List.of("unproven precedence") : List.of());
    }

    private static int rows(AcraSuiteTab tab, String name) {
        JTable table = find(tab.component(), JTable.class, name);
        if (table == null) throw new AssertionError("table missing: " + name);
        return table.getRowCount();
    }

    private static int indexOf(JTabbedPane tabs, String title) {
        if (tabs == null) return -1;
        for (int index = 0; index < tabs.getTabCount(); index++) {
            if (title.equals(tabs.getTitleAt(index))) return index;
        }
        return -1;
    }

    private static <T extends Component> T find(Component root, Class<T> type, String name) {
        if (root == null) return null;
        if (type.isInstance(root) && (name == null
                || root instanceof JComponent component && name.equals(component.getName()))) {
            return type.cast(root);
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                T match = find(child, type, name);
                if (match != null) return match;
            }
        }
        return null;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
