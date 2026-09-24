package io.acra.burp.ui;

import io.acra.core.domain.finding.FindingReviewTransition;
import io.acra.core.product.finding.FindingReviewCase;
import io.acra.core.product.finding.FindingReviewSnapshot;
import io.acra.core.product.finding.FindingReviewWorkspace;
import io.acra.core.reporting.finding.FindingBurpIssueDraftGenerator;
import io.acra.core.reporting.finding.FindingReproductionJsonExporter;
import io.acra.core.reporting.finding.FindingReproductionPackage;
import io.acra.core.reporting.finding.FindingReproductionPackageGenerator;
import io.acra.core.reporting.finding.FindingReproductionSarifExporter;
import io.acra.core.serialization.DomainSerializer;
import java.awt.BorderLayout;
import java.awt.Font;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.AbstractTableModel;

@SuppressWarnings("serial")
public final class S11FindingReviewPanel extends JPanel {
    private static final Instant PREVIEW_TIME = Instant.EPOCH;

    private final FindingReviewWorkspace workspace;
    private final JTextArea overview = view("s11-finding-review-overview");
    private final JTextArea jsonView = view("s11-finding-json-reproduction-view");
    private final JTextArea sarifView = view("s11-finding-sarif-view");
    private final JTextArea burpDraftView = view("s11-finding-burp-draft-view");
    private final FindingModel findingModel = new FindingModel();
    private final ReviewTrailModel reviewTrailModel = new ReviewTrailModel();
    private final FindingReproductionPackageGenerator packageGenerator =
            new FindingReproductionPackageGenerator();
    private final FindingReproductionJsonExporter jsonExporter =
            new FindingReproductionJsonExporter();
    private final FindingReproductionSarifExporter sarifExporter =
            new FindingReproductionSarifExporter();
    private final FindingBurpIssueDraftGenerator burpDraftGenerator =
            new FindingBurpIssueDraftGenerator();
    private final DomainSerializer serializer = new DomainSerializer();

    public S11FindingReviewPanel(FindingReviewWorkspace workspace) {
        super(new BorderLayout());
        if (workspace == null) throw new IllegalArgumentException("Sprint 11 workspace required");
        this.workspace = workspace;
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        add(buildTabs(), BorderLayout.CENTER);
        refresh();
    }

    public void install(JTabbedPane tabs) {
        if (tabs == null) throw new IllegalArgumentException("tabs required");
        tabs.addTab("Findings & Reproduction", this);
    }

    public FindingReviewWorkspace workspace() {
        return workspace;
    }

    public void refresh() {
        FindingReviewSnapshot snapshot = workspace.snapshot();
        findingModel.update(snapshot.cases());
        reviewTrailModel.update(snapshot.cases());
        renderOverview(snapshot);
        renderPreview(snapshot);
    }

    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setName("s11-finding-review-tabs");
        tabs.addTab("Overview", new JScrollPane(overview));
        tabs.addTab("Findings", table(findingModel, "s11-finding-review-table"));
        tabs.addTab("Review History", table(reviewTrailModel, "s11-finding-review-history-table"));
        tabs.addTab("JSON Reproduction", new JScrollPane(jsonView));
        tabs.addTab("SARIF", new JScrollPane(sarifView));
        tabs.addTab("Burp Issue Draft", new JScrollPane(burpDraftView));
        return tabs;
    }

    private void renderOverview(FindingReviewSnapshot snapshot) {
        StringBuilder text = new StringBuilder(
                "SPRINT 11 FINDINGS & REPRODUCTION — READ ONLY\n\n");
        text.append("Project: ").append(snapshot.projectId()).append('\n')
                .append("Reviewed finding records: ").append(snapshot.totalCount()).append('\n')
                .append("Needs review: ").append(snapshot.count(
                        io.acra.core.domain.finding.FindingLifecycleState.NEEDS_REVIEW)).append('\n')
                .append("Validated: ").append(snapshot.count(
                        io.acra.core.domain.finding.FindingLifecycleState.VALIDATED)).append('\n')
                .append("Confirmed: ").append(snapshot.count(
                        io.acra.core.domain.finding.FindingLifecycleState.CONFIRMED)).append('\n')
                .append("False positive: ").append(snapshot.count(
                        io.acra.core.domain.finding.FindingLifecycleState.FALSE_POSITIVE)).append('\n')
                .append("Accepted risk: ").append(snapshot.count(
                        io.acra.core.domain.finding.FindingLifecycleState.ACCEPTED_RISK)).append('\n')
                .append("Open review records: ").append(snapshot.openReviewCount()).append('\n')
                .append("Terminal records: ").append(snapshot.terminalCount()).append('\n')
                .append("\nCandidate != confirmed vulnerability.")
                .append("\nSeverity and confidence are independent.")
                .append("\nThis surface does not change lifecycle state.")
                .append("\nJSON/SARIF/Burp views are deterministic read-only projections.")
                .append("\nBurp Issue Draft != published Burp issue.")
                .append("\nReal Burp desktop publication remains a separate runtime validation gate.");
        overview.setText(text.toString());
    }

    private void renderPreview(FindingReviewSnapshot snapshot) {
        if (snapshot.cases().isEmpty()) {
            String empty = "No reviewed finding is available for reproduction preview.";
            jsonView.setText(empty);
            sarifView.setText(empty);
            burpDraftView.setText(empty);
            return;
        }

        FindingReviewCase first = snapshot.cases().get(0);
        FindingReproductionPackage reproduction = packageGenerator.generate(first, PREVIEW_TIME);
        jsonView.setText(jsonExporter.json(reproduction).content());
        sarifView.setText(sarifExporter.sarif(reproduction).content());
        burpDraftView.setText(serializer.serialize(burpDraftGenerator.generate(reproduction)));
    }

    private static JComponent table(AbstractTableModel model, String name) {
        JTable table = new JTable(model);
        table.setName(name);
        table.setAutoCreateRowSorter(true);
        return new JScrollPane(table);
    }

    private static JTextArea view(String name) {
        JTextArea area = new JTextArea();
        area.setName(name);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        return area;
    }

    private abstract static class RowsModel extends AbstractTableModel {
        private List<Object[]> rows = List.of();
        private final String[] columns;

        RowsModel(String... columns) {
            this.columns = columns;
        }

        final void rows(List<Object[]> values) {
            rows = List.copyOf(values == null ? List.of() : values);
            fireTableDataChanged();
        }

        @Override public final int getRowCount() { return rows.size(); }
        @Override public final int getColumnCount() { return columns.length; }
        @Override public final String getColumnName(int column) { return columns[column]; }
        @Override public final Object getValueAt(int row, int column) { return rows.get(row)[column]; }
    }

    private static final class FindingModel extends RowsModel {
        FindingModel() {
            super(
                    "Finding",
                    "Candidate",
                    "State",
                    "Severity",
                    "Confidence",
                    "Endpoint",
                    "Resource",
                    "Expected",
                    "Observed",
                    "Evidence",
                    "Transitions");
        }

        void update(List<FindingReviewCase> cases) {
            List<Object[]> rows = new ArrayList<>();
            for (FindingReviewCase reviewCase : cases) {
                var candidate = reviewCase.candidate();
                var finding = reviewCase.finding();
                rows.add(new Object[]{
                        finding.findingId(),
                        finding.candidateId(),
                        finding.state(),
                        finding.severity(),
                        finding.confidence(),
                        candidate.endpoint(),
                        candidate.resourceId(),
                        candidate.expectedDecision(),
                        candidate.observedDecision(),
                        finding.supportingEvidenceIds().size(),
                        finding.history().size()});
            }
            rows(rows);
        }
    }

    private static final class ReviewTrailModel extends RowsModel {
        ReviewTrailModel() {
            super(
                    "Finding",
                    "Transition",
                    "From",
                    "To",
                    "Occurred",
                    "Evidence");
        }

        void update(List<FindingReviewCase> cases) {
            List<Object[]> rows = new ArrayList<>();
            for (FindingReviewCase reviewCase : cases) {
                for (FindingReviewTransition transition : reviewCase.finding().history()) {
                    rows.add(new Object[]{
                            reviewCase.finding().findingId(),
                            transition.transitionId(),
                            transition.fromState(),
                            transition.toState(),
                            transition.occurredAt(),
                            transition.evidenceIds().size()});
                }
            }
            rows(rows);
        }
    }
}
