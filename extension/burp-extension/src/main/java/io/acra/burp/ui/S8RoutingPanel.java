package io.acra.burp.ui;

import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.product.routing.S8RoutingProductSnapshot;
import io.acra.core.product.routing.S8RoutingWorkspace;
import io.acra.core.route.RouteAuthorizationAssessment;
import io.acra.core.route.RouteBoundaryTransition;
import io.acra.core.route.RouteNormalizationTrace;
import io.acra.core.route.RouteSecurityBoundaryTrace;
import io.acra.core.route.RouteStageObservation;
import java.awt.BorderLayout;
import java.awt.Font;
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
public final class S8RoutingPanel extends JPanel {
    private final S8RoutingWorkspace workspace;
    private final JTextArea overview = view("s8-routing-overview");
    private final StageTraceModel stageTraceModel = new StageTraceModel();
    private final BoundaryModel boundaryModel = new BoundaryModel();
    private final AssessmentModel assessmentModel = new AssessmentModel();
    private final CandidateModel candidateModel = new CandidateModel();

    public S8RoutingPanel(S8RoutingWorkspace workspace) {
        super(new BorderLayout());
        if (workspace == null) throw new IllegalArgumentException("routing workspace required");
        this.workspace = workspace;
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        add(buildTabs(), BorderLayout.CENTER);
        refresh();
    }

    public void install(JTabbedPane tabs) {
        if (tabs == null) throw new IllegalArgumentException("tabs required");
        tabs.addTab("Routing", this);
    }

    public S8RoutingWorkspace workspace() {
        return workspace;
    }

    public void refresh() {
        S8RoutingProductSnapshot snapshot = workspace.snapshot();
        renderOverview(snapshot);
        stageTraceModel.update(snapshot.normalizationTraces());
        boundaryModel.update(snapshot.boundaryTraces());
        assessmentModel.update(snapshot.assessments());
        candidateModel.update(snapshot.candidates());
    }

    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setName("s8-routing-tabs");
        tabs.addTab("Overview", new JScrollPane(overview));
        tabs.addTab("Stage Traces", table(stageTraceModel, "s8-routing-stage-table"));
        tabs.addTab("Boundary Matrix", table(boundaryModel, "s8-routing-boundary-table"));
        tabs.addTab("Assessments", table(assessmentModel, "s8-routing-assessment-table"));
        tabs.addTab("Candidates", table(candidateModel, "s8-routing-candidate-table"));
        return tabs;
    }

    private void renderOverview(S8RoutingProductSnapshot snapshot) {
        long complete = snapshot.normalizationTraces().stream()
                .filter(value -> value.state().name().equals("COMPLETE")).count();
        long combined = snapshot.boundaryTraces().stream()
                .flatMap(value -> value.transitions().stream())
                .filter(value -> value.state().name().equals("COMBINED_DIVERGENCE")).count();
        long inconclusive = snapshot.boundaryTraces().stream()
                .flatMap(value -> value.transitions().stream())
                .filter(value -> value.state().name().equals("INCONCLUSIVE")).count();

        StringBuilder text = new StringBuilder("SPRINT 8 ROUTING NORMALIZATION & AUTHORIZATION-PATH INTELLIGENCE\n\n");
        text.append("Normalization traces: ").append(snapshot.normalizationTraces().size()).append('\n')
                .append("Complete traces: ").append(complete).append('\n')
                .append("Boundary traces: ").append(snapshot.boundaryTraces().size()).append('\n')
                .append("Combined route + authorization differentials: ").append(combined).append('\n')
                .append("Inconclusive boundary transitions: ").append(inconclusive).append('\n')
                .append("Assessments: ").append(snapshot.assessments().size()).append('\n')
                .append("Inconclusive assessments: ").append(snapshot.inconclusiveAssessmentCount()).append('\n')
                .append("Finding candidates: ").append(snapshot.candidateCount()).append('\n')
                .append("\nCandidate != confirmed vulnerability.")
                .append("\nRouting divergence != vulnerability severity.")
                .append("\nUnknown processing stages remain unknown.");
        overview.setText(text.toString());
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

    private static final class StageTraceModel extends RowsModel {
        StageTraceModel() {
            super("Trace", "Trace State", "Stage", "Path", "Source", "Evidence", "Missing Stages");
        }

        void update(List<RouteNormalizationTrace> traces) {
            List<Object[]> rows = new ArrayList<>();
            for (RouteNormalizationTrace trace : traces) {
                for (RouteStageObservation observation : trace.observations()) {
                    rows.add(new Object[]{
                            trace.traceId(),
                            trace.state(),
                            observation.stage(),
                            observation.path(),
                            observation.source(),
                            observation.evidenceIds().size(),
                            trace.missingStages()});
                }
            }
            rows(rows);
        }
    }

    private static final class BoundaryModel extends RowsModel {
        BoundaryModel() {
            super("Trace", "From", "To", "Path", "Method", "Host", "API Version",
                    "Authorization", "State", "Evidence", "Reasons");
        }

        void update(List<RouteSecurityBoundaryTrace> traces) {
            List<Object[]> rows = new ArrayList<>();
            for (RouteSecurityBoundaryTrace trace : traces) {
                for (RouteBoundaryTransition transition : trace.transitions()) {
                    rows.add(new Object[]{
                            trace.traceId(),
                            transition.fromStage(),
                            transition.toStage(),
                            transition.pathDivergence(),
                            transition.methodChanged(),
                            transition.hostChanged(),
                            transition.apiVersionChanged(),
                            transition.authorizationChanged(),
                            transition.state(),
                            transition.evidenceIds().size(),
                            transition.reasons()});
                }
            }
            rows(rows);
        }
    }

    private static final class AssessmentModel extends RowsModel {
        AssessmentModel() {
            super("Assessment", "From", "To", "Path Divergence", "Expected", "Observed",
                    "State", "Evidence", "Rationale");
        }

        void update(List<RouteAuthorizationAssessment> assessments) {
            List<Object[]> rows = new ArrayList<>();
            for (RouteAuthorizationAssessment assessment : assessments) {
                rows.add(new Object[]{
                        assessment.assessmentId(),
                        assessment.fromStage(),
                        assessment.toStage(),
                        assessment.pathDivergence(),
                        assessment.expectedDecision(),
                        assessment.observedDecision(),
                        assessment.state(),
                        assessment.evidenceIds().size(),
                        assessment.rationale()});
            }
            rows(rows);
        }
    }

    private static final class CandidateModel extends RowsModel {
        CandidateModel() {
            super("Candidate", "State", "Endpoint", "Principal", "Tenant", "Expected", "Observed",
                    "Dimensions", "Confidence", "Evidence", "Rationale");
        }

        void update(List<FindingCandidate> candidates) {
            List<Object[]> rows = new ArrayList<>();
            for (FindingCandidate candidate : candidates) {
                rows.add(new Object[]{
                        candidate.candidateId(),
                        candidate.state(),
                        candidate.endpoint(),
                        candidate.principalId(),
                        candidate.tenantRelationship(),
                        candidate.expectedDecision(),
                        candidate.observedDecision(),
                        candidate.dimensions(),
                        candidate.confidence(),
                        candidate.supportingEvidenceIds().size(),
                        candidate.rationale()});
            }
            rows(rows);
        }
    }
}
