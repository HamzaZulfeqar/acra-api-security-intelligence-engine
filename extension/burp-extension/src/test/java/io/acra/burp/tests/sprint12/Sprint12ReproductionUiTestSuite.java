package io.acra.burp.tests.sprint12;

import io.acra.burp.scanner.S12BurpIssuePublicationReceipt;
import io.acra.burp.scope.ScopeController;
import io.acra.burp.traffic.TrafficIntelligencePipeline;
import io.acra.burp.ui.AcraSuiteTab;
import io.acra.burp.ui.S12ReproductionPanel;
import io.acra.core.active.product.ActiveEngineWorkspace;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.product.authorization.S6AuthorizationWorkspace;
import io.acra.core.product.batchindirect.S10BatchIndirectWorkspace;
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

public final class Sprint12ReproductionUiTestSuite {
    private static int assertions;

    private Sprint12ReproductionUiTestSuite() { }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        S12ReproductionWorkspace workspace = fixtureWorkspace();
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
                workspace)));
        AcraSuiteTab tab = reference.get();
        try {
            SwingUtilities.invokeAndWait(() -> verify(tab));
        } finally {
            SwingUtilities.invokeAndWait(tab::stop);
        }
        System.out.println("SPRINT12_REPRODUCTION_UI PASS assertions=" + assertions);
    }

    private static void verify(AcraSuiteTab tab) {
        JTabbedPane top = find(tab.component(), JTabbedPane.class, null);
        check(indexOf(top, "Reproduction") >= 0,
                "Sprint 12 Reproduction top-level tab installed");
        assertions++;

        S12ReproductionPanel panel = find(tab.component(), S12ReproductionPanel.class, "s12-reproduction-panel");
        check(panel != null, "Sprint 12 panel installed");
        assertions++;

        JTabbedPane tabs = find(panel, JTabbedPane.class, "s12-reproduction-tabs");
        for (String title : Set.of(
                "Overview", "Packages", "JSON Export", "SARIF Export",
                "Burp Review", "Publication Receipts")) {
            check(indexOf(tabs, title) >= 0, "Sprint 12 sub-tab installed: " + title);
            assertions++;
        }

        JTextArea overview = find(panel, JTextArea.class, "s12-reproduction-overview");
        check(overview.getText().contains("Reproduction packages: 2"),
                "overview renders two deterministic packages");
        assertions++;
        check(overview.getText().contains("Review candidates: 1"),
                "overview separates review candidate count");
        assertions++;
        check(overview.getText().contains("Rejected controls: 1"),
                "overview separates rejected control count");
        assertions++;
        check(overview.getText().contains("Core publishable projections: 0"),
                "overview preserves non-publishable core boundary");
        assertions++;
        check(overview.getText().contains("Candidate != confirmed vulnerability"),
                "overview states candidate review boundary");
        assertions++;
        check(overview.getText().contains("explicit candidate-bound approval"),
                "overview states explicit approval requirement");
        assertions++;
        check(overview.getText().contains("no automatic publication control"),
                "overview states no automatic publication path");
        assertions++;
        check(overview.getText().contains("Real Burp desktop/site-map runtime validation remains separate"),
                "overview does not mislabel headless UI as real Burp validation");
        assertions++;

        check(rows(panel, "s12-reproduction-package-table") == 2,
                "package table renders both review and rejected packages");
        assertions++;
        check(rows(panel, "s12-reproduction-burp-table") == 2,
                "Burp review table renders both non-publishable projections");
        assertions++;
        check(rows(panel, "s12-reproduction-receipt-table") == 0,
                "publication receipts are empty before explicit publication receipt is supplied");
        assertions++;

        JTable burp = find(panel, JTable.class, "s12-reproduction-burp-table");
        for (int row = 0; row < burp.getRowCount(); row++) {
            check(Boolean.FALSE.equals(burp.getValueAt(row, 4)),
                    "every core Burp projection remains non-publishable");
            assertions++;
        }

        JTextArea json = find(panel, JTextArea.class, "s12-reproduction-json-view");
        check(json.getText().contains("\"packageVersion\":\"s12-reproduction-package-v1\""),
                "JSON preview renders canonical Sprint 12 package version");
        assertions++;
        check(!json.getText().contains("DummyPassword"),
                "JSON preview excludes rationale secret material");
        assertions++;
        check(!json.getText().contains("\"principalId\""),
                "JSON preview excludes raw principal field");
        assertions++;

        JTextArea sarif = find(panel, JTextArea.class, "s12-reproduction-sarif-view");
        check(sarif.getText().contains("\"version\":\"2.1.0\""),
                "SARIF preview renders SARIF 2.1.0");
        assertions++;
        check(sarif.getText().contains("\"level\":\"none\""),
                "SARIF preview preserves non-fail level-none boundary");
        assertions++;
        check(sarif.getText().contains("\"reviewOnly\":true"),
                "SARIF preview explicitly preserves review-only state");
        assertions++;
        check(!sarif.getText().contains("DummyPassword"),
                "SARIF preview excludes rationale secret material");
        assertions++;

        check(count(panel, JButton.class) == 0,
                "Sprint 12 reproduction panel contains no publication button");
        assertions++;

        check(tab.reproductionWorkspace().snapshot().packageCount() == 2,
                "suite tab exposes same two-package reproduction workspace");
        assertions++;
        check(tab.reproductionWorkspace().snapshot().publishableProjectionCount() == 0,
                "workspace exposes zero publishable core projections");
        assertions++;

        S12BurpIssuePublicationReceipt receipt = new S12BurpIssuePublicationReceipt(
                "",
                "s12-ui-candidate",
                "human-ui-approval",
                "https://acra-lab.invalid/api/v1/documents/1002",
                "ACRA authorization candidate review",
                S12BurpIssuePublicationReceipt.IMPORTED_REVIEW_CANDIDATE,
                "");
        panel.recordReceipt(receipt);
        check(rows(panel, "s12-reproduction-receipt-table") == 1,
                "explicit receipt becomes visible without mutating package state");
        assertions++;
        check(overview.getText().contains("Publication receipts: 1"),
                "overview updates explicit publication receipt count");
        assertions++;
        check(tab.reproductionWorkspace().snapshot().candidateCount() == 1,
                "recording receipt does not mutate candidate package state");
        assertions++;
    }

    private static S12ReproductionWorkspace fixtureWorkspace() {
        S12ReproductionWorkspace workspace = new S12ReproductionWorkspace();
        workspace.recordCandidate(finding("s12-ui-candidate", FindingCandidateState.CANDIDATE));
        workspace.recordCandidate(finding("s12-ui-rejected", FindingCandidateState.REJECTED));
        return workspace;
    }

    private static FindingCandidate finding(String id, FindingCandidateState state) {
        return new FindingCandidate(
                id,
                state,
                "acra-s12-ui",
                List.of("test-s12"),
                List.of("execution-s12"),
                List.of("observation-s12"),
                List.of("assessment-s12"),
                List.of("OBJECT_AUTHORIZATION"),
                "/api/v1/documents/1002",
                "document:1002",
                "user-a",
                "FOREIGN_TENANT",
                AuthorizationDecision.DENY,
                state == FindingCandidateState.CANDIDATE
                        ? AuthorizationDecision.ALLOW : AuthorizationDecision.DENY,
                List.of("evidence-" + id),
                List.of(),
                List.of("policy-s12"),
                state == FindingCandidateState.CANDIDATE ? "HIGH" : "MEDIUM",
                "Sprint 12 UI fixture; password=DummyPassword; review-only",
                FindingFingerprint.of(
                        "/api/v1/documents/1002",
                        "document:1002",
                        "user-a",
                        "FOREIGN_TENANT",
                        "OBJECT_AUTHORIZATION",
                        state.name()));
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

    private static <T extends Component> int count(Component root, Class<T> type) {
        if (root == null) return 0;
        int total = type.isInstance(root) ? 1 : 0;
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) total += count(child, type);
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
