package io.acra.burp.ui;

import io.acra.core.domain.authorization.PropertyAuthorizationAssessment;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.engine.PolicyValidationEvaluator;
import io.acra.core.product.property.S9PropertyProductSnapshot;
import io.acra.core.product.property.S9PropertyWorkspace;
import io.acra.core.property.PropertyAccessObservation;
import io.acra.core.property.PropertyAuthorizationCoverageEntry;
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
public final class S9PropertyPanel extends JPanel {
    private final S9PropertyWorkspace workspace;
    private final JTextArea overview = view("s9-property-overview");
    private final JTextArea reportView = view("s9-property-report-view");
    private final JTextArea jsonExportView = view("s9-property-json-export-view");
    private final PolicyModel policyModel = new PolicyModel();
    private final ObservationModel observationModel = new ObservationModel();
    private final AssessmentModel assessmentModel = new AssessmentModel();
    private final CandidateModel candidateModel = new CandidateModel();
    private final CoverageModel coverageModel = new CoverageModel();

    public S9PropertyPanel(S9PropertyWorkspace workspace) {
        super(new BorderLayout());
        if (workspace == null) throw new IllegalArgumentException("property workspace required");
        this.workspace = workspace;
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        add(buildTabs(), BorderLayout.CENTER);
        refresh();
    }

    public void install(JTabbedPane tabs) {
        if (tabs == null) throw new IllegalArgumentException("tabs required");
        tabs.addTab("Properties", this);
    }

    public S9PropertyWorkspace workspace() {
        return workspace;
    }

    public void refresh() {
        S9PropertyProductSnapshot snapshot = workspace.snapshot();
        renderOverview(snapshot);
        policyModel.update(snapshot.policies());
        observationModel.update(snapshot.observations());
        assessmentModel.update(snapshot.assessments());
        candidateModel.update(snapshot.candidates());
        coverageModel.update(snapshot.coverageEntries());
        java.time.Instant previewAt = java.time.Instant.EPOCH;
        reportView.setText(workspace.exportMarkdown(previewAt).content());
        jsonExportView.setText(workspace.exportJson(previewAt).content());
    }

    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setName("s9-property-tabs");
        tabs.addTab("Overview", new JScrollPane(overview));
        tabs.addTab("Policies", table(policyModel, "s9-property-policy-table"));
        tabs.addTab("Observations", table(observationModel, "s9-property-observation-table"));
        tabs.addTab("Assessments", table(assessmentModel, "s9-property-assessment-table"));
        tabs.addTab("Candidates", table(candidateModel, "s9-property-candidate-table"));
        tabs.addTab("Coverage", table(coverageModel, "s9-property-coverage-table"));
        tabs.addTab("Report", new JScrollPane(reportView));
        tabs.addTab("JSON Export", new JScrollPane(jsonExportView));
        return tabs;
    }

    private void renderOverview(S9PropertyProductSnapshot snapshot) {
        var coverage = snapshot.coverageSummary();
        StringBuilder text = new StringBuilder(
                "SPRINT 9 PROPERTY-LEVEL AUTHORIZATION & FIELD POLICY INTELLIGENCE\n\n");
        text.append("Policy contexts: ").append(coverage.totalPolicyContexts()).append('\n')
                .append("READ contexts: ").append(coverage.readPolicyContexts()).append('\n')
                .append("UPDATE contexts: ").append(coverage.updatePolicyContexts()).append('\n')
                .append("Observed contexts: ").append(coverage.observedContexts()).append('\n')
                .append("Assessed contexts: ").append(coverage.assessedContexts()).append('\n')
                .append("Unobserved contexts: ").append(coverage.unobservedContexts()).append('\n')
                .append("Observed / unassessed: ").append(coverage.observedUnassessedContexts()).append('\n')
                .append("Finding candidates: ").append(snapshot.candidateCount()).append('\n')
                .append("Rejected controls: ").append(snapshot.rejectedCount()).append('\n')
                .append("Inconclusive projections: ").append(snapshot.inconclusiveCount()).append('\n')
                .append("\nCandidate != confirmed vulnerability.")
                .append("\nUnobserved != secure.")
                .append("\nProperty values are not rendered by this workspace.")
                .append("\nReal Burp desktop runtime validation remains separate from headless UI verification.");
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

    private static final class PolicyModel extends RowsModel {
        PolicyModel() {
            super("Policy", "Endpoint", "Property", "Operation", "Role", "Tenant", "Expected", "Evidence");
        }

        void update(List<PolicyValidationEvaluator.PropertyPolicy> policies) {
            List<Object[]> rows = new ArrayList<>();
            for (var policy : policies) {
                rows.add(new Object[]{
                        policy.policyReference(),
                        policy.endpoint(),
                        policy.property(),
                        policy.operation(),
                        policy.roleId(),
                        policy.tenantId(),
                        policy.expectedDecision(),
                        policy.evidenceIds().size()});
            }
            rows(rows);
        }
    }

    private static final class ObservationModel extends RowsModel {
        ObservationModel() {
            super("Observation", "Execution", "Test", "Endpoint", "Property", "Operation", "Observed", "Evidence");
        }

        void update(List<PropertyAccessObservation> observations) {
            List<Object[]> rows = new ArrayList<>();
            for (var observation : observations) {
                rows.add(new Object[]{
                        observation.observationId(),
                        observation.executionId(),
                        observation.testId(),
                        observation.endpoint(),
                        observation.property(),
                        observation.operation(),
                        observation.observedDecision(),
                        observation.evidenceIds().size()});
            }
            rows(rows);
        }
    }

    private static final class AssessmentModel extends RowsModel {
        AssessmentModel() {
            super("Assessment", "Endpoint", "Property", "Operation", "Policy", "Expected", "Observed",
                    "State", "Confidence", "Evidence", "Reasons");
        }

        void update(List<PropertyAuthorizationAssessment> assessments) {
            List<Object[]> rows = new ArrayList<>();
            for (var assessment : assessments) {
                rows.add(new Object[]{
                        assessment.assessmentId(),
                        assessment.endpoint(),
                        assessment.property(),
                        assessment.operation(),
                        assessment.policyReference(),
                        assessment.expectedDecision(),
                        assessment.observedDecision(),
                        assessment.state(),
                        assessment.confidence(),
                        assessment.evidenceIds().size(),
                        assessment.reasons()});
            }
            rows(rows);
        }
    }

    private static final class CandidateModel extends RowsModel {
        CandidateModel() {
            super("Candidate", "State", "Endpoint", "Resource", "Expected", "Observed",
                    "Confidence", "Policy", "Dimensions", "Evidence", "Rationale");
        }

        void update(List<FindingCandidate> candidates) {
            List<Object[]> rows = new ArrayList<>();
            for (var candidate : candidates) {
                rows.add(new Object[]{
                        candidate.candidateId(),
                        candidate.state(),
                        candidate.endpoint(),
                        candidate.resourceId(),
                        candidate.expectedDecision(),
                        candidate.observedDecision(),
                        candidate.confidence(),
                        candidate.policyReferences(),
                        candidate.dimensions(),
                        candidate.supportingEvidenceIds().size(),
                        candidate.rationale()});
            }
            rows(rows);
        }
    }

    private static final class CoverageModel extends RowsModel {
        CoverageModel() {
            super("Coverage", "Policy", "Endpoint", "Property", "Operation", "Role", "Tenant", "Expected",
                    "Disposition", "Observations", "Assessments", "Findings");
        }

        void update(List<PropertyAuthorizationCoverageEntry> entries) {
            List<Object[]> rows = new ArrayList<>();
            for (var entry : entries) {
                rows.add(new Object[]{
                        entry.coverageId(),
                        entry.policyReference(),
                        entry.endpoint(),
                        entry.property(),
                        entry.operation(),
                        entry.roleId(),
                        entry.tenantId(),
                        entry.expectedDecision(),
                        entry.disposition(),
                        entry.observationIds().size(),
                        entry.assessmentIds().size(),
                        entry.findingCandidateIds().size()});
            }
            rows(rows);
        }
    }
}
