package io.acra.burp.ui;

import io.acra.core.batch.BatchItemAuthorizationAssessment;
import io.acra.core.batch.BatchItemObservation;
import io.acra.core.batch.BatchItemPolicy;
import io.acra.core.coverage.S10AuthorizationCoverageEntry;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.product.batchindirect.S10BatchIndirectProductSnapshot;
import io.acra.core.product.batchindirect.S10BatchIndirectWorkspace;
import io.acra.core.reference.IndirectReferenceAuthorizationAssessment;
import io.acra.core.reference.IndirectReferencePolicy;
import io.acra.core.reference.IndirectReferenceResolution;
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
public final class S10BatchIndirectPanel extends JPanel {
    private final S10BatchIndirectWorkspace workspace;
    private final JTextArea overview = view("s10-batch-indirect-overview");
    private final JTextArea reportView = view("s10-batch-indirect-report-view");
    private final JTextArea jsonExportView = view("s10-batch-indirect-json-export-view");
    private final PolicyModel policyModel = new PolicyModel();
    private final ObservationModel observationModel = new ObservationModel();
    private final AssessmentModel assessmentModel = new AssessmentModel();
    private final CandidateModel candidateModel = new CandidateModel();
    private final CoverageModel coverageModel = new CoverageModel();

    public S10BatchIndirectPanel(S10BatchIndirectWorkspace workspace) {
        super(new BorderLayout());
        if (workspace == null) throw new IllegalArgumentException("Sprint 10 workspace required");
        this.workspace = workspace;
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        add(buildTabs(), BorderLayout.CENTER);
        refresh();
    }

    public void install(JTabbedPane tabs) {
        if (tabs == null) throw new IllegalArgumentException("tabs required");
        tabs.addTab("Batch & Indirect", this);
    }

    public S10BatchIndirectWorkspace workspace() {
        return workspace;
    }

    public void refresh() {
        S10BatchIndirectProductSnapshot snapshot = workspace.snapshot();
        renderOverview(snapshot);
        policyModel.update(snapshot.batchPolicies(), snapshot.indirectPolicies());
        observationModel.update(snapshot.batchObservations(), snapshot.indirectResolutions());
        assessmentModel.update(snapshot.batchAssessments(), snapshot.indirectAssessments());
        candidateModel.update(snapshot.candidates());
        coverageModel.update(snapshot.coverageEntries());
        java.time.Instant previewAt = java.time.Instant.EPOCH;
        reportView.setText(workspace.exportMarkdown(previewAt).content());
        jsonExportView.setText(workspace.exportJson(previewAt).content());
    }

    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setName("s10-batch-indirect-tabs");
        tabs.addTab("Overview", new JScrollPane(overview));
        tabs.addTab("Policies", table(policyModel, "s10-batch-indirect-policy-table"));
        tabs.addTab("Observations", table(observationModel, "s10-batch-indirect-observation-table"));
        tabs.addTab("Assessments", table(assessmentModel, "s10-batch-indirect-assessment-table"));
        tabs.addTab("Candidates", table(candidateModel, "s10-batch-indirect-candidate-table"));
        tabs.addTab("Coverage", table(coverageModel, "s10-batch-indirect-coverage-table"));
        tabs.addTab("Report", new JScrollPane(reportView));
        tabs.addTab("JSON Export", new JScrollPane(jsonExportView));
        return tabs;
    }

    private void renderOverview(S10BatchIndirectProductSnapshot snapshot) {
        var coverage = snapshot.coverageSummary();
        StringBuilder text = new StringBuilder(
                "SPRINT 10 BATCH & INDIRECT AUTHORIZATION INTELLIGENCE\n\n");
        text.append("Policy contexts: ").append(coverage.totalPolicyContexts()).append('\n')
                .append("Batch contexts: ").append(coverage.batchPolicyContexts()).append('\n')
                .append("Indirect contexts: ").append(coverage.indirectPolicyContexts()).append('\n')
                .append("Observed contexts: ").append(coverage.observedContexts()).append('\n')
                .append("Assessed contexts: ").append(coverage.assessedContexts()).append('\n')
                .append("Unobserved contexts: ").append(coverage.unobservedContexts()).append('\n')
                .append("Observed / unassessed: ").append(coverage.observedUnassessedContexts()).append('\n')
                .append("Finding candidates: ").append(snapshot.candidateCount()).append('\n')
                .append("Rejected controls: ").append(snapshot.rejectedCount()).append('\n')
                .append("Inconclusive projections: ").append(snapshot.inconclusiveCount()).append('\n')
                .append("\nCandidate != confirmed vulnerability.")
                .append("\nAggregate HTTP success != per-item authorization.")
                .append("\nUnobserved != secure.")
                .append("\nRaw indirect aliases are not rendered by this workspace.")
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
            super("Family", "Policy", "Endpoint", "Resource", "Action", "Role", "Tenant", "Expected", "Evidence");
        }

        void update(List<BatchItemPolicy> batch, List<IndirectReferencePolicy> indirect) {
            List<Object[]> rows = new ArrayList<>();
            for (BatchItemPolicy policy : batch) {
                rows.add(new Object[]{
                        "BATCH_ITEM", policy.policyReference(), policy.endpoint(), policy.resourceId(),
                        policy.action(), policy.roleId(), policy.tenantId(), policy.expectedDecision(),
                        policy.evidenceIds().size()});
            }
            for (IndirectReferencePolicy policy : indirect) {
                rows.add(new Object[]{
                        "INDIRECT_REFERENCE", policy.policyReference(), policy.endpoint(), policy.resolvedResourceId(),
                        policy.action(), policy.roleId(), policy.tenantId(), policy.expectedDecision(),
                        policy.evidenceIds().size()});
            }
            rows(rows);
        }
    }

    private static final class ObservationModel extends RowsModel {
        ObservationModel() {
            super("Family", "Observation", "Execution", "Test", "Endpoint", "Resource",
                    "Action", "Observed", "Reference Fingerprint", "Evidence");
        }

        void update(List<BatchItemObservation> batch, List<IndirectReferenceResolution> indirect) {
            List<Object[]> rows = new ArrayList<>();
            for (BatchItemObservation observation : batch) {
                rows.add(new Object[]{
                        "BATCH_ITEM", observation.itemObservationId(), observation.executionId(), observation.testId(),
                        observation.endpoint(), observation.resourceId(), observation.action(),
                        observation.observedDecision(), "", observation.evidenceIds().size()});
            }
            for (IndirectReferenceResolution resolution : indirect) {
                rows.add(new Object[]{
                        "INDIRECT_REFERENCE", resolution.resolutionId(), resolution.executionId(), resolution.testId(),
                        resolution.endpoint(), resolution.resolvedResourceId(), resolution.action(),
                        resolution.observedDecision(), resolution.referenceFingerprint(), resolution.evidenceIds().size()});
            }
            rows(rows);
        }
    }

    private static final class AssessmentModel extends RowsModel {
        AssessmentModel() {
            super("Family", "Assessment", "Endpoint", "Resource", "Action", "Policy",
                    "Expected", "Observed", "State", "Confidence", "Evidence", "Reasons");
        }

        void update(
                List<BatchItemAuthorizationAssessment> batch,
                List<IndirectReferenceAuthorizationAssessment> indirect) {
            List<Object[]> rows = new ArrayList<>();
            for (BatchItemAuthorizationAssessment assessment : batch) {
                rows.add(new Object[]{
                        "BATCH_ITEM", assessment.assessmentId(), assessment.endpoint(), assessment.resourceId(),
                        assessment.action(), assessment.policyReference(), assessment.expectedDecision(),
                        assessment.observedDecision(), assessment.state(), assessment.confidence(),
                        assessment.evidenceIds().size(), assessment.reasons()});
            }
            for (IndirectReferenceAuthorizationAssessment assessment : indirect) {
                rows.add(new Object[]{
                        "INDIRECT_REFERENCE", assessment.assessmentId(), assessment.endpoint(),
                        assessment.resolvedResourceId(), assessment.action(), assessment.policyReference(),
                        assessment.expectedDecision(), assessment.observedDecision(), assessment.state(),
                        assessment.confidence(), assessment.evidenceIds().size(), assessment.reasons()});
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
            for (FindingCandidate candidate : candidates) {
                rows.add(new Object[]{
                        candidate.candidateId(), candidate.state(), candidate.endpoint(), candidate.resourceId(),
                        candidate.expectedDecision(), candidate.observedDecision(), candidate.confidence(),
                        candidate.policyReferences(), candidate.dimensions(),
                        candidate.supportingEvidenceIds().size(), candidate.rationale()});
            }
            rows(rows);
        }
    }

    private static final class CoverageModel extends RowsModel {
        CoverageModel() {
            super("Coverage", "Family", "Policy", "Endpoint", "Resource", "Action", "Role", "Tenant",
                    "Expected", "Disposition", "Observations", "Assessments", "Findings");
        }

        void update(List<S10AuthorizationCoverageEntry> entries) {
            List<Object[]> rows = new ArrayList<>();
            for (S10AuthorizationCoverageEntry entry : entries) {
                rows.add(new Object[]{
                        entry.coverageId(), entry.family(), entry.policyReference(), entry.endpoint(),
                        entry.resourceId(), entry.action(), entry.roleId(), entry.tenantId(),
                        entry.expectedDecision(), entry.disposition(), entry.observationIds().size(),
                        entry.assessmentIds().size(), entry.findingCandidateIds().size()});
            }
            rows(rows);
        }
    }
}
