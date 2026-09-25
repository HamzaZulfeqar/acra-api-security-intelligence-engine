package io.acra.burp.tests.sprint13;

import io.acra.burp.scope.ScopeController;
import io.acra.burp.traffic.TrafficIntelligencePipeline;
import io.acra.burp.ui.AcraSuiteTab;
import io.acra.burp.ui.S13FindingGovernancePanel;
import io.acra.core.active.product.ActiveEngineWorkspace;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.AuthorizationImpactProfile;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.domain.finding.FindingLifecycleAction;
import io.acra.core.domain.finding.FindingLifecycleTransitionRequest;
import io.acra.core.domain.finding.FindingSeverity;
import io.acra.core.engine.AuthorizationSeverityEvaluator;
import io.acra.core.product.authorization.S6AuthorizationWorkspace;
import io.acra.core.product.batchindirect.S10BatchIndirectWorkspace;
import io.acra.core.product.finding.FindingGovernanceWorkspace;
import io.acra.core.product.property.S9PropertyWorkspace;
import io.acra.core.product.reproduction.S12ReproductionWorkspace;
import io.acra.core.product.routing.S8RoutingWorkspace;
import io.acra.core.product.workflow.S7WorkflowWorkspace;
import java.awt.Component;
import java.awt.Container;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public final class Sprint13FindingGovernanceUiTestSuite {
    private static int assertions;

    private Sprint13FindingGovernanceUiTestSuite() { }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        FindingGovernanceWorkspace workspace = fixtureWorkspace();
        AtomicReference<AcraSuiteTab> reference = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> reference.set(new AcraSuiteTab(
                new TrafficIntelligencePipeline(20),
                new ScopeController(),
                new ActiveEngineWorkspace(Clock.systemUTC(), null),
                new S6AuthorizationWorkspace(),
                new S7WorkflowWorkspace(),
                new S8RoutingWorkspace(),
                new S9PropertyWorkspace(),
                new S10BatchIndirectWorkspace(),
                new S12ReproductionWorkspace(),
                workspace)));
        AcraSuiteTab tab = reference.get();
        try {
            SwingUtilities.invokeAndWait(() -> verify(tab));
        } finally {
            SwingUtilities.invokeAndWait(tab::stop);
        }
        System.out.println("SPRINT13_FINDING_GOVERNANCE_UI PASS assertions=" + assertions);
    }

    private static void verify(AcraSuiteTab tab) {
        JTabbedPane top = find(tab.component(), JTabbedPane.class, null);
        check(indexOf(top, "Finding Governance") >= 0,
                "Sprint 13 Finding Governance top-level tab installed");
        assertions++;

        S13FindingGovernancePanel panel = find(
                tab.component(), S13FindingGovernancePanel.class, "s13-finding-governance-panel");
        check(panel != null, "Sprint 13 governance panel installed");
        assertions++;

        JTabbedPane tabs = find(panel, JTabbedPane.class, "s13-governance-tabs");
        for (String title : Set.of(
                "Overview", "Review Required", "Confirmed", "Remediation",
                "Retest", "Terminal", "History")) {
            check(indexOf(tabs, title) >= 0, "Sprint 13 sub-tab installed: " + title);
            assertions++;
        }

        JTextArea overview = find(panel, JTextArea.class, "s13-governance-overview");
        check(overview.getText().contains("Governed findings: 5"),
                "overview renders full finding denominator");
        assertions++;
        check(overview.getText().contains("Review required: 1"),
                "overview renders review-required count");
        assertions++;
        check(overview.getText().contains("Confirmed: 1"),
                "overview renders confirmed count");
        assertions++;
        check(overview.getText().contains("Remediation: 1"),
                "overview renders remediation count");
        assertions++;
        check(overview.getText().contains("Retest: 1"),
                "overview renders retest count");
        assertions++;
        check(overview.getText().contains("Terminal: 1"),
                "overview renders terminal count");
        assertions++;
        check(overview.getText().contains("Historically confirmed: 3"),
                "overview preserves confirmed-history count");
        assertions++;
        check(overview.getText().contains("FindingCandidate != confirmed vulnerability"),
                "overview preserves candidate review boundary");
        assertions++;
        check(overview.getText().contains("Severity/confidence are prioritization context only"),
                "overview states severity does not control lifecycle");
        assertions++;
        check(overview.getText().contains("This UI is read-only"),
                "overview states read-only lifecycle boundary");
        assertions++;
        check(overview.getText().contains("Real Burp desktop runtime validation remains separate and unverified"),
                "headless UI is not mislabelled as desktop validation");
        assertions++;

        check(rows(panel, "s13-governance-review-table") == 1,
                "review-required table renders one finding");
        assertions++;
        check(rows(panel, "s13-governance-confirmed-table") == 1,
                "confirmed table renders one finding");
        assertions++;
        check(rows(panel, "s13-governance-remediation-table") == 1,
                "remediation table renders one finding");
        assertions++;
        check(rows(panel, "s13-governance-retest-table") == 1,
                "retest table renders one finding");
        assertions++;
        check(rows(panel, "s13-governance-terminal-table") == 1,
                "terminal table renders one finding");
        assertions++;
        check(rows(panel, "s13-governance-history-table") == 7,
                "history table renders all append-only lifecycle events");
        assertions++;

        JTable review = find(panel, JTable.class, "s13-governance-review-table");
        check(FindingSeverity.CRITICAL.equals(review.getValueAt(0, 3)),
                "critical finding can remain review-required");
        assertions++;
        check("REVIEW_REQUIRED".equals(String.valueOf(review.getValueAt(0, 2))),
                "critical severity does not auto-confirm lifecycle state");
        assertions++;

        JTable terminal = find(panel, JTable.class, "s13-governance-terminal-table");
        check(Boolean.FALSE.equals(terminal.getValueAt(0, 7)),
                "false-positive terminal finding remains historically unconfirmed");
        assertions++;

        JTable history = find(panel, JTable.class, "s13-governance-history-table");
        check(history.getRowCount() == 7,
                "history row count is deterministic");
        assertions++;
        for (int row = 0; row < history.getRowCount(); row++) {
            check(String.valueOf(history.getValueAt(row, 5)).startsWith("reviewer-"),
                    "history exposes opaque reviewer reference");
            assertions++;
            check(String.valueOf(history.getValueAt(row, 6)).startsWith("decision-"),
                    "history exposes opaque decision reference");
            assertions++;
            check(((Number) history.getValueAt(row, 7)).intValue() == 1,
                    "each fixture lifecycle event retains one evidence reference");
            assertions++;
        }

        check(actionButtonCount(panel) == 0,
                "governance panel exposes no lifecycle or publication action button");
        assertions++;

        check(tab.findingGovernanceWorkspace().snapshot().findingCount() == 5,
                "suite tab exposes the same governance workspace");
        assertions++;
        check(tab.findingGovernanceWorkspace().snapshot().confirmedHistoryCount() == 3,
                "suite tab preserves governance confirmed-history state");
        assertions++;
    }

    private static FindingGovernanceWorkspace fixtureWorkspace() {
        FindingGovernanceWorkspace workspace = new FindingGovernanceWorkspace();

        var review = open(workspace, "review", "document:1301", true);
        check(review.severity() == FindingSeverity.CRITICAL,
                "fixture review finding is critical for severity/state boundary");
        assertions++;

        var confirmed = open(workspace, "confirmed", "document:1302", false);
        transition(workspace, confirmed, FindingLifecycleAction.CONFIRM, "confirmed");

        var remediation = open(workspace, "remediation", "document:1303", false);
        remediation = transition(workspace, remediation, FindingLifecycleAction.CONFIRM, "remediation-confirm");
        transition(workspace, remediation, FindingLifecycleAction.START_REMEDIATION, "remediation-start");

        var retest = open(workspace, "retest", "document:1304", false);
        retest = transition(workspace, retest, FindingLifecycleAction.CONFIRM, "retest-confirm");
        retest = transition(workspace, retest, FindingLifecycleAction.START_REMEDIATION, "retest-start");
        transition(workspace, retest, FindingLifecycleAction.REQUEST_RETEST, "retest-request");

        var terminal = open(workspace, "terminal", "document:1305", false);
        transition(workspace, terminal, FindingLifecycleAction.MARK_FALSE_POSITIVE, "terminal-fp");

        return workspace;
    }

    private static io.acra.core.domain.finding.GovernedFinding open(
            FindingGovernanceWorkspace workspace,
            String suffix,
            String resource,
            boolean critical) {
        FindingCandidate candidate = candidate("candidate-" + suffix, resource);
        return workspace.open(
                candidate,
                new AuthorizationSeverityEvaluator().evaluate(
                        candidate,
                        critical
                                ? new AuthorizationImpactProfile(
                                        true, true, false, true, true, false,
                                        List.of("critical-review-context"))
                                : AuthorizationImpactProfile.none()));
    }

    private static io.acra.core.domain.finding.GovernedFinding transition(
            FindingGovernanceWorkspace workspace,
            io.acra.core.domain.finding.GovernedFinding finding,
            FindingLifecycleAction action,
            String suffix) {
        return workspace.transition(
                finding.findingId(),
                finding.fingerprint(),
                new FindingLifecycleTransitionRequest(
                        action,
                        "reviewer-" + suffix,
                        "decision-" + suffix,
                        List.of("evidence-" + suffix)));
    }

    private static FindingCandidate candidate(String id, String resource) {
        return new FindingCandidate(
                id,
                FindingCandidateState.CANDIDATE,
                "acra-s13-ui",
                List.of("test-" + id),
                List.of("execution-" + id),
                List.of("observation-" + id),
                List.of("assessment-" + id),
                List.of("OBJECT_AUTHORIZATION"),
                "/api/v1/documents/{id}",
                resource,
                "user-a",
                "FOREIGN_TENANT",
                AuthorizationDecision.DENY,
                AuthorizationDecision.ALLOW,
                List.of("evidence-" + id),
                List.of(),
                List.of("policy-" + id),
                "HIGH",
                "Sprint 13 UI review-only fixture",
                FindingFingerprint.of(
                        "/api/v1/documents/{id}",
                        resource,
                        "user-a",
                        "FOREIGN_TENANT",
                        "DENY_TO_ALLOW",
                        "OBJECT_AUTHORIZATION"));
    }

    private static int rows(Component root, String name) {
        JTable table = find(root, JTable.class, name);
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

    private static int actionButtonCount(Component root) {
        if (root == null) return 0;
        int total = 0;
        if (root instanceof JButton button) {
            String text = button.getText() == null ? "" : button.getText().toLowerCase();
            String name = button.getName() == null ? "" : button.getName().toLowerCase();
            for (String token : List.of(
                    "confirm", "remediat", "retest", "resolve", "false positive",
                    "close", "publish", "import", "add issue")) {
                if (text.contains(token) || name.contains(token.replace(" ", "-"))) {
                    total++;
                    break;
                }
            }
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) total += actionButtonCount(child);
        }
        return total;
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
