package io.acra.burp.tests.sprint11;

import io.acra.burp.ui.S11FindingReviewPanel;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.AuthorizationRiskAssessment;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingConfidence;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.domain.finding.FindingLifecycleState;
import io.acra.core.domain.finding.FindingSeverity;
import io.acra.core.product.finding.FindingReviewWorkspace;
import java.awt.Component;
import java.awt.Container;
import java.time.Instant;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;

public final class Sprint11FindingReviewUiTestSuite {
    private static int assertions;
    private static final Instant AT = Instant.parse("2026-09-25T04:00:00Z");

    private Sprint11FindingReviewUiTestSuite() { }

    public static void main(String[] args) {
        assertions = 0;
        run();
        System.out.println("SPRINT11_FINDING_REVIEW_UI PASS assertions=" + assertions);
    }

    private static void run() {
        FindingReviewWorkspace workspace = new FindingReviewWorkspace("acra-s11-ui");
        FindingCandidate confirmedCandidate = candidate(
                "candidate-confirmed",
                "resource-confirmed",
                "password=DummyPassword; UI source rationale");
        var confirmed = workspace.open(
                confirmedCandidate,
                risk(confirmedCandidate.candidateId(), FindingSeverity.HIGH, FindingConfidence.HIGH),
                AT);
        workspace.transition(
                confirmed.findingId(),
                FindingLifecycleState.VALIDATED,
                AT.plusSeconds(10),
                "reviewer-ui-a",
                "Authorization: Bearer ui-review-secret",
                List.of("ui-validate-evidence"));
        workspace.transition(
                confirmed.findingId(),
                FindingLifecycleState.CONFIRMED,
                AT.plusSeconds(20),
                "reviewer-ui-b",
                "Independent confirmation",
                List.of("ui-confirm-evidence"));

        S11FindingReviewPanel panel = new S11FindingReviewPanel(workspace);
        check(panel.workspace() == workspace,
                "panel exposes the injected finding review workspace");
        assertions++;

        JTabbedPane rootTabs = new JTabbedPane();
        panel.install(rootTabs);
        check(indexOf(rootTabs, "Findings & Reproduction") >= 0,
                "Sprint 11 panel installs as Findings & Reproduction");
        assertions++;

        JTabbedPane inner = find(panel, JTabbedPane.class, "s11-finding-review-tabs");
        check(inner != null, "Sprint 11 inner tab set exists");
        assertions++;
        check(indexOf(inner, "Overview") >= 0, "overview tab exists");
        assertions++;
        check(indexOf(inner, "Findings") >= 0, "findings tab exists");
        assertions++;
        check(indexOf(inner, "Review History") >= 0, "review history tab exists");
        assertions++;
        check(indexOf(inner, "JSON Reproduction") >= 0, "JSON reproduction tab exists");
        assertions++;
        check(indexOf(inner, "SARIF") >= 0, "SARIF tab exists");
        assertions++;
        check(indexOf(inner, "Burp Issue Draft") >= 0, "Burp issue draft tab exists");
        assertions++;

        JTextArea overview = find(panel, JTextArea.class, "s11-finding-review-overview");
        check(overview != null && !overview.isEditable(),
                "overview is read-only");
        assertions++;
        check(overview.getText().contains("READ ONLY"),
                "overview declares read-only product boundary");
        assertions++;
        check(overview.getText().contains("Reviewed finding records: 1"),
                "overview reports finding count");
        assertions++;
        check(overview.getText().contains("Confirmed: 1"),
                "overview reports explicit confirmed count");
        assertions++;
        check(overview.getText().contains("Candidate != confirmed vulnerability."),
                "overview preserves candidate-versus-confirmed boundary");
        assertions++;
        check(overview.getText().contains("Burp Issue Draft != published Burp issue."),
                "overview preserves draft-versus-publication boundary");
        assertions++;

        JTable findings = find(panel, JTable.class, "s11-finding-review-table");
        check(findings != null && findings.getRowCount() == 1,
                "findings table renders one reviewed finding");
        assertions++;
        check(findings.getColumnCount() == 11,
                "findings table exposes compact review columns");
        assertions++;
        check(tableContains(findings, "CONFIRMED"),
                "findings table exposes confirmed state");
        assertions++;
        check(tableContains(findings, "HIGH"),
                "findings table exposes independent severity");
        assertions++;

        JTable history = find(panel, JTable.class, "s11-finding-review-history-table");
        check(history != null && history.getRowCount() == 2,
                "review history table renders validation and confirmation transitions");
        assertions++;
        check(tableContains(history, "NEEDS_REVIEW")
                        && tableContains(history, "VALIDATED")
                        && tableContains(history, "CONFIRMED"),
                "review history exposes state progression");
        assertions++;

        JTextArea json = find(panel, JTextArea.class, "s11-finding-json-reproduction-view");
        check(json != null && !json.isEditable(), "JSON reproduction view is read-only");
        assertions++;
        check(json.getText().contains("\"confirmedFinding\":true"),
                "JSON preview preserves explicit confirmation");
        assertions++;
        check(json.getText().contains("\"state\":\"CONFIRMED\""),
                "JSON preview preserves lifecycle state");
        assertions++;

        JTextArea sarif = find(panel, JTextArea.class, "s11-finding-sarif-view");
        check(sarif != null && !sarif.isEditable(), "SARIF view is read-only");
        assertions++;
        check(sarif.getText().contains("\"version\":\"2.1.0\""),
                "SARIF preview declares version 2.1.0");
        assertions++;
        check(sarif.getText().contains("\"kind\":\"fail\""),
                "confirmed finding renders SARIF fail kind");
        assertions++;

        JTextArea burp = find(panel, JTextArea.class, "s11-finding-burp-draft-view");
        check(burp != null && !burp.isEditable(), "Burp issue draft view is read-only");
        assertions++;
        check(burp.getText().contains("\"publicationEligible\":true"),
                "confirmed finding renders publication-eligible Burp draft");
        assertions++;
        check(burp.getText().contains("does not publish"),
                "Burp draft preview retains non-publication limitation");
        assertions++;

        for (JTextArea area : List.of(overview, json, sarif, burp)) {
            check(!area.getText().contains("DummyPassword"),
                    "Sprint 11 UI must not render candidate rationale secret");
            assertions++;
            check(!area.getText().contains("ui-review-secret"),
                    "Sprint 11 UI must not render review reason secret");
            assertions++;
            check(!area.getText().contains("reviewer-ui-a")
                            && !area.getText().contains("reviewer-ui-b"),
                    "Sprint 11 UI must not render reviewer identity");
            assertions++;
        }

        check(!containsActionButton(panel),
                "Sprint 11 findings surface contains no lifecycle or publication action button");
        assertions++;

        FindingCandidate fpCandidate = candidate(
                "candidate-fp",
                "resource-fp",
                "false-positive fixture");
        var fp = workspace.open(
                fpCandidate,
                risk(fpCandidate.candidateId(), FindingSeverity.MEDIUM, FindingConfidence.MEDIUM),
                AT.plusSeconds(30));
        workspace.transition(
                fp.findingId(),
                FindingLifecycleState.FALSE_POSITIVE,
                AT.plusSeconds(40),
                "reviewer-ui-fp",
                "Control evidence disproves candidate",
                List.of("ui-fp-evidence"));
        panel.refresh();

        check(findings.getRowCount() == 2,
                "refresh renders newly added reviewed finding");
        assertions++;
        check(history.getRowCount() == 3,
                "refresh renders appended false-positive transition");
        assertions++;
        check(overview.getText().contains("False positive: 1"),
                "overview counts false-positive disposition");
        assertions++;
        check(overview.getText().contains("Terminal records: 1"),
                "overview counts only false-positive record as terminal");
        assertions++;
    }

    private static FindingCandidate candidate(
            String id,
            String resourceId,
            String rationale) {
        return new FindingCandidate(
                id,
                FindingCandidateState.CANDIDATE,
                "acra-s11-ui",
                List.of("test-" + id),
                List.of("execution-" + id),
                List.of("observation-" + id),
                List.of("assessment-" + id),
                List.of("BATCH_AUTHORIZATION"),
                "/api/v1/s11/findings",
                resourceId,
                "user-a",
                "CROSS_TENANT",
                AuthorizationDecision.DENY,
                AuthorizationDecision.ALLOW,
                List.of("evidence-" + id),
                List.of(),
                List.of("policy-" + id),
                "HIGH",
                rationale,
                FindingFingerprint.of(
                        "/api/v1/s11/findings",
                        resourceId,
                        "user-a",
                        "CROSS_TENANT",
                        "BATCH_AUTHORIZATION",
                        "CANDIDATE"));
    }

    private static AuthorizationRiskAssessment risk(
            String candidateId,
            FindingSeverity severity,
            FindingConfidence confidence) {
        return new AuthorizationRiskAssessment(
                "risk-" + candidateId,
                candidateId,
                severity,
                confidence,
                severity == FindingSeverity.HIGH ? 80 : 60,
                "UI risk fixture",
                List.of("authorization-impact"));
    }

    private static boolean tableContains(JTable table, String needle) {
        for (int row = 0; row < table.getRowCount(); row++) {
            for (int column = 0; column < table.getColumnCount(); column++) {
                if (String.valueOf(table.getValueAt(row, column)).contains(needle)) return true;
            }
        }
        return false;
    }

    private static boolean containsActionButton(Component root) {
        if (root instanceof JButton button) {
            String text = button.getText() == null ? "" : button.getText().trim().toLowerCase();
            if (text.contains("confirm")
                    || text.contains("validate")
                    || text.contains("false positive")
                    || text.contains("accept risk")
                    || text.contains("publish")
                    || text.contains("add issue")
                    || text.contains("reject")) {
                return true;
            }
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                if (containsActionButton(child)) return true;
            }
        }
        return false;
    }

    private static int indexOf(JTabbedPane tabs, String title) {
        if (tabs == null) return -1;
        for (int index = 0; index < tabs.getTabCount(); index++) {
            if (title.equals(tabs.getTitleAt(index))) return index;
        }
        return -1;
    }

    private static <T extends Component> T find(
            Component root,
            Class<T> type,
            String name) {
        if (root == null) return null;
        if (type.isInstance(root)
                && (name == null
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
