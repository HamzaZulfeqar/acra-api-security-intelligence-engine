package io.acra.burp.ui;

import io.acra.burp.scanner.S12BurpIssuePublicationReceipt;
import io.acra.core.product.reproduction.S12ReproductionProductEntry;
import io.acra.core.product.reproduction.S12ReproductionProductSnapshot;
import io.acra.core.product.reproduction.S12ReproductionWorkspace;
import java.awt.BorderLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.AbstractTableModel;

@SuppressWarnings("serial")
public final class S12ReproductionPanel extends JPanel {
    private final S12ReproductionWorkspace workspace;
    private final TreeMap<String, S12BurpIssuePublicationReceipt> receipts = new TreeMap<>();
    private final JTextArea overview = view("s12-reproduction-overview");
    private final JTextArea jsonView = view("s12-reproduction-json-view");
    private final JTextArea sarifView = view("s12-reproduction-sarif-view");
    private final PackageModel packageModel = new PackageModel();
    private final BurpProjectionModel burpModel = new BurpProjectionModel();
    private final ReceiptModel receiptModel = new ReceiptModel();

    public S12ReproductionPanel(S12ReproductionWorkspace workspace) {
        super(new BorderLayout());
        if (workspace == null) throw new IllegalArgumentException("Sprint 12 workspace required");
        this.workspace = workspace;
        setName("s12-reproduction-panel");
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        add(buildTabs(), BorderLayout.CENTER);
        refresh();
    }

    public void install(JTabbedPane tabs) {
        if (tabs == null) throw new IllegalArgumentException("tabs required");
        tabs.addTab("Reproduction", this);
    }

    public S12ReproductionWorkspace workspace() {
        return workspace;
    }

    public synchronized void recordReceipt(S12BurpIssuePublicationReceipt receipt) {
        if (receipt == null) throw new IllegalArgumentException("publication receipt required");
        receipts.put(receipt.receiptId(), receipt);
        refresh();
    }

    public synchronized void refresh() {
        S12ReproductionProductSnapshot snapshot = workspace.snapshot();
        packageModel.update(snapshot.entries());
        burpModel.update(snapshot.entries());
        receiptModel.update(List.copyOf(receipts.values()));
        renderOverview(snapshot);
        if (snapshot.entries().isEmpty()) {
            jsonView.setText("No reproduction package selected.");
            sarifView.setText("No reproduction package selected.");
        } else {
            S12ReproductionProductEntry first = snapshot.entries().getFirst();
            jsonView.setText(first.jsonExport().content());
            sarifView.setText(first.sarifExport().content());
        }
    }

    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setName("s12-reproduction-tabs");
        tabs.addTab("Overview", new JScrollPane(overview));
        tabs.addTab("Packages", table(packageModel, "s12-reproduction-package-table"));
        tabs.addTab("JSON Export", new JScrollPane(jsonView));
        tabs.addTab("SARIF Export", new JScrollPane(sarifView));
        tabs.addTab("Burp Review", table(burpModel, "s12-reproduction-burp-table"));
        tabs.addTab("Publication Receipts", table(receiptModel, "s12-reproduction-receipt-table"));
        return tabs;
    }

    private void renderOverview(S12ReproductionProductSnapshot snapshot) {
        overview.setText(
                "Reproduction packages: " + snapshot.packageCount() + "\n"
                + "Review candidates: " + snapshot.candidateCount() + "\n"
                + "Rejected controls: " + snapshot.rejectedCount() + "\n"
                + "Inconclusive: " + snapshot.inconclusiveCount() + "\n"
                + "Core publishable projections: " + snapshot.publishableProjectionCount() + "\n"
                + "Publication receipts: " + receipts.size() + "\n\n"
                + "Candidate != confirmed vulnerability.\n"
                + "Burp publication requires explicit candidate-bound approval.\n"
                + "This panel has no automatic publication control.\n"
                + "Real Burp desktop/site-map runtime validation remains separate and unverified.");
    }

    private static JScrollPane table(AbstractTableModel model, String name) {
        JTable table = new JTable(model);
        table.setName(name);
        return new JScrollPane(table);
    }

    private static JTextArea view(String name) {
        JTextArea area = new JTextArea();
        area.setName(name);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        return area;
    }

    @SuppressWarnings("serial")
    private static final class PackageModel extends AbstractTableModel {
        private final String[] columns = {
                "Package", "Candidate", "State", "Endpoint", "Resource", "Expected", "Observed", "Evidence"
        };
        private List<S12ReproductionProductEntry> rows = List.of();

        void update(List<S12ReproductionProductEntry> values) {
            rows = List.copyOf(values == null ? List.of() : values);
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int column) { return columns[column]; }

        @Override
        public Object getValueAt(int row, int column) {
            var value = rows.get(row).reproductionPackage();
            return switch (column) {
                case 0 -> value.packageId();
                case 1 -> value.sourceCandidateId();
                case 2 -> value.candidateState();
                case 3 -> value.endpoint();
                case 4 -> value.resourceId();
                case 5 -> value.expectedDecision();
                case 6 -> value.observedDecision();
                default -> value.evidenceIds().size();
            };
        }
    }

    @SuppressWarnings("serial")
    private static final class BurpProjectionModel extends AbstractTableModel {
        private final String[] columns = {
                "Candidate", "Title", "Severity", "Confidence", "Publishable"
        };
        private List<S12ReproductionProductEntry> rows = List.of();

        void update(List<S12ReproductionProductEntry> values) {
            rows = List.copyOf(values == null ? List.of() : values);
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int column) { return columns[column]; }

        @Override
        public Object getValueAt(int row, int column) {
            var value = rows.get(row).burpProjection();
            return switch (column) {
                case 0 -> value.candidateId();
                case 1 -> value.title();
                case 2 -> value.severity();
                case 3 -> value.confidence();
                default -> value.publishable();
            };
        }
    }

    @SuppressWarnings("serial")
    private static final class ReceiptModel extends AbstractTableModel {
        private final String[] columns = {
                "Receipt", "Candidate", "Approval", "URL", "State"
        };
        private List<S12BurpIssuePublicationReceipt> rows = List.of();

        void update(List<S12BurpIssuePublicationReceipt> values) {
            rows = new ArrayList<>(values == null ? List.of() : values);
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int column) { return columns[column]; }

        @Override
        public Object getValueAt(int row, int column) {
            var value = rows.get(row);
            return switch (column) {
                case 0 -> value.receiptId();
                case 1 -> value.candidateId();
                case 2 -> value.approvalReference();
                case 3 -> value.baseUrl();
                default -> value.state();
            };
        }
    }
}
