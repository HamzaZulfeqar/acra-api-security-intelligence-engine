package io.acra.burp.ui;

import io.acra.core.domain.finding.FindingLifecycleEvent;
import io.acra.core.domain.finding.GovernedFinding;
import io.acra.core.product.finding.FindingGovernanceQueue;
import io.acra.core.product.finding.FindingGovernanceSnapshot;
import io.acra.core.product.finding.FindingGovernanceWorkspace;
import java.awt.BorderLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.AbstractTableModel;

@SuppressWarnings("serial")
public final class S13FindingGovernancePanel extends JPanel {
    private final FindingGovernanceWorkspace workspace;
    private final JTextArea overview = view("s13-governance-overview");
    private final FindingModel reviewModel = new FindingModel();
    private final FindingModel confirmedModel = new FindingModel();
    private final FindingModel remediationModel = new FindingModel();
    private final FindingModel retestModel = new FindingModel();
    private final FindingModel terminalModel = new FindingModel();
    private final HistoryModel historyModel = new HistoryModel();

    public S13FindingGovernancePanel(FindingGovernanceWorkspace workspace) {
        super(new BorderLayout());
        if (workspace == null) throw new IllegalArgumentException("Sprint 13 workspace required");
        this.workspace = workspace;
        setName("s13-finding-governance-panel");
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        add(buildTabs(), BorderLayout.CENTER);
        refresh();
    }

    public void install(JTabbedPane tabs) {
        if (tabs == null) throw new IllegalArgumentException("tabs required");
        tabs.addTab("Finding Governance", this);
    }

    public FindingGovernanceWorkspace workspace() {
        return workspace;
    }

    public void refresh() {
        FindingGovernanceSnapshot snapshot = workspace.snapshot();
        overview.setText(
                "Governed findings: " + snapshot.findingCount() + "\n"
                        + "Review required: " + snapshot.queueCount(FindingGovernanceQueue.REVIEW_REQUIRED) + "\n"
                        + "Confirmed: " + snapshot.queueCount(FindingGovernanceQueue.CONFIRMED) + "\n"
                        + "Remediation: " + snapshot.queueCount(FindingGovernanceQueue.REMEDIATION) + "\n"
                        + "Retest: " + snapshot.queueCount(FindingGovernanceQueue.RETEST) + "\n"
                        + "Terminal: " + snapshot.queueCount(FindingGovernanceQueue.TERMINAL) + "\n"
                        + "Historically confirmed: " + snapshot.confirmedHistoryCount() + "\n\n"
                        + "Lifecycle boundary: FindingCandidate != confirmed vulnerability.\n"
                        + "Severity/confidence are prioritization context only.\n"
                        + "Transitions require explicit reviewer/decision/evidence through the lifecycle service.\n"
                        + "This UI is read-only: no confirm/remediate/retest/close/publish action control.\n"
                        + "Real Burp desktop runtime validation remains separate and unverified.");
        reviewModel.update(snapshot.queue(FindingGovernanceQueue.REVIEW_REQUIRED));
        confirmedModel.update(snapshot.queue(FindingGovernanceQueue.CONFIRMED));
        remediationModel.update(snapshot.queue(FindingGovernanceQueue.REMEDIATION));
        retestModel.update(snapshot.queue(FindingGovernanceQueue.RETEST));
        terminalModel.update(snapshot.queue(FindingGovernanceQueue.TERMINAL));
        historyModel.update(snapshot.findings());
    }

    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setName("s13-governance-tabs");
        tabs.addTab("Overview", new JScrollPane(overview));
        tabs.addTab("Review Required", table(reviewModel, "s13-governance-review-table"));
        tabs.addTab("Confirmed", table(confirmedModel, "s13-governance-confirmed-table"));
        tabs.addTab("Remediation", table(remediationModel, "s13-governance-remediation-table"));
        tabs.addTab("Retest", table(retestModel, "s13-governance-retest-table"));
        tabs.addTab("Terminal", table(terminalModel, "s13-governance-terminal-table"));
        tabs.addTab("History", table(historyModel, "s13-governance-history-table"));
        return tabs;
    }

    private static JTextArea view(String name) {
        JTextArea area = new JTextArea();
        area.setName(name);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        return area;
    }

    private static JScrollPane table(AbstractTableModel model, String name) {
        JTable table = new JTable(model);
        table.setName(name);
        table.setFillsViewportHeight(true);
        return new JScrollPane(table);
    }

    private static final class FindingModel extends AbstractTableModel {
        private final String[] columns = {
                "Finding", "Candidate", "State", "Severity", "Confidence", "Events", "Evidence", "Confirmed History"
        };
        private List<GovernedFinding> rows = List.of();

        void update(List<GovernedFinding> values) {
            rows = List.copyOf(values == null ? List.of() : values);
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int column) { return columns[column]; }

        @Override
        public Object getValueAt(int row, int column) {
            GovernedFinding value = rows.get(row);
            return switch (column) {
                case 0 -> value.findingId();
                case 1 -> value.sourceCandidateId();
                case 2 -> value.state();
                case 3 -> value.severity();
                case 4 -> value.confidence();
                case 5 -> value.events().size();
                case 6 -> value.evidenceIds().size();
                default -> value.confirmed();
            };
        }
    }

    private record HistoryRow(
            String findingId,
            int sequence,
            String fromState,
            String toState,
            String action,
            String reviewer,
            String decision,
            int evidenceCount,
            String eventId) { }

    private static final class HistoryModel extends AbstractTableModel {
        private final String[] columns = {
                "Finding", "Seq", "From", "To", "Action", "Reviewer Ref", "Decision Ref", "Evidence", "Event"
        };
        private List<HistoryRow> rows = List.of();

        void update(List<GovernedFinding> findings) {
            List<HistoryRow> values = new ArrayList<>();
            for (GovernedFinding finding : findings == null ? List.<GovernedFinding>of() : findings) {
                for (FindingLifecycleEvent event : finding.events()) {
                    values.add(new HistoryRow(
                            finding.findingId(),
                            event.sequence(),
                            event.fromState().name(),
                            event.toState().name(),
                            event.action().name(),
                            event.reviewerReference(),
                            event.decisionReference(),
                            event.evidenceIds().size(),
                            event.eventId()));
                }
            }
            rows = List.copyOf(values);
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int column) { return columns[column]; }

        @Override
        public Object getValueAt(int row, int column) {
            HistoryRow value = rows.get(row);
            return switch (column) {
                case 0 -> value.findingId();
                case 1 -> value.sequence();
                case 2 -> value.fromState();
                case 3 -> value.toState();
                case 4 -> value.action();
                case 5 -> value.reviewer();
                case 6 -> value.decision();
                case 7 -> value.evidenceCount();
                default -> value.eventId();
            };
        }
    }
}
