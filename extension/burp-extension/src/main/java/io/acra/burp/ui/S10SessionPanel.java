package io.acra.burp.ui;

import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.product.session.S10SessionProductSnapshot;
import io.acra.core.product.session.S10SessionWorkspace;
import io.acra.core.session.AuthenticationSessionObservation;
import io.acra.core.session.SessionCorrelationResult;
import io.acra.core.session.SessionCoverageEntry;
import io.acra.core.session.SessionSecurityAssessment;
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
public final class S10SessionPanel extends JPanel {
    private final S10SessionWorkspace workspace;
    private final JTextArea overview = view("s10-session-overview");
    private final ObservationModel observationModel = new ObservationModel();
    private final CorrelationModel correlationModel = new CorrelationModel();
    private final AssessmentModel assessmentModel = new AssessmentModel();
    private final CandidateModel candidateModel = new CandidateModel();
    private final CoverageModel coverageModel = new CoverageModel();

    public S10SessionPanel(S10SessionWorkspace workspace) {
        super(new BorderLayout());
        if (workspace == null) throw new IllegalArgumentException("session workspace required");
        this.workspace = workspace;
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        add(buildTabs(), BorderLayout.CENTER);
        refresh();
    }

    public void install(JTabbedPane tabs) {
        if (tabs == null) throw new IllegalArgumentException("tabs required");
        tabs.addTab("Authentication", this);
    }

    public S10SessionWorkspace workspace() {
        return workspace;
    }

    public void refresh() {
        S10SessionProductSnapshot snapshot = workspace.snapshot();
        renderOverview(snapshot);
        observationModel.update(snapshot.observations());
        correlationModel.update(snapshot.correlations());
        assessmentModel.update(snapshot.assessments());
        candidateModel.update(snapshot.candidates());
        coverageModel.update(snapshot.coverageEntries());
    }

    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setName("s10-session-tabs");
        tabs.addTab("Overview", new JScrollPane(overview));
        tabs.addTab("Sessions", table(observationModel, "s10-session-observation-table"));
        tabs.addTab("Correlations", table(correlationModel, "s10-session-correlation-table"));
        tabs.addTab("Assessments", table(assessmentModel, "s10-session-assessment-table"));
        tabs.addTab("Candidates", table(candidateModel, "s10-session-candidate-table"));
        tabs.addTab("Coverage", table(coverageModel, "s10-session-coverage-table"));
        return tabs;
    }

    private void renderOverview(S10SessionProductSnapshot snapshot) {
        var coverage = snapshot.coverageSummary();
        StringBuilder text = new StringBuilder(
                "SPRINT 10 AUTHENTICATION, SESSION & TOKEN-CONTEXT INTELLIGENCE\n\n");
        text.append("Session observations: ").append(snapshot.observations().size()).append('\n')
                .append("Correlations: ").append(snapshot.correlations().size()).append('\n')
                .append("Assessments: ").append(snapshot.assessments().size()).append('\n')
                .append("Coverage targets: ").append(coverage.totalTargets()).append('\n')
                .append("Observed targets: ").append(coverage.observedTargets()).append('\n')
                .append("Correlated targets: ").append(coverage.correlatedTargets()).append('\n')
                .append("Assessed targets: ").append(coverage.assessedTargets()).append('\n')
                .append("Unobserved targets: ").append(coverage.unobservedTargets()).append('\n')
                .append("Observed / uncorrelated: ").append(coverage.observedUncorrelatedTargets()).append('\n')
                .append("Correlated / unassessed: ").append(coverage.correlatedUnassessedTargets()).append('\n')
                .append("Finding candidates: ").append(snapshot.candidateCount()).append('\n')
                .append("Rejected controls: ").append(snapshot.rejectedCount()).append('\n')
                .append("Inconclusive projections: ").append(snapshot.inconclusiveCount()).append('\n')
                .append("\nToken fingerprint != verified principal.")
                .append("\nCandidate != confirmed vulnerability.")
                .append("\nUnobserved != secure.")
                .append("\nRaw authentication material is not rendered by this workspace.")
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

    private static final class ObservationModel extends RowsModel {
        ObservationModel() {
            super("Observation", "Session", "Principal", "Role", "Tenant", "Scopes",
                    "Authentication", "Identity State", "Observed", "Evidence");
        }

        void update(List<AuthenticationSessionObservation> observations) {
            List<Object[]> rows = new ArrayList<>();
            for (var observation : observations) {
                rows.add(new Object[]{
                        observation.observationId(),
                        observation.sessionId(),
                        observation.principalId(),
                        observation.roleId(),
                        observation.tenantId(),
                        observation.scopes(),
                        observation.authenticationType(),
                        observation.identityState(),
                        observation.observedAt(),
                        observation.evidenceIds().size()});
            }
            rows(rows);
        }
    }

    private static final class CorrelationModel extends RowsModel {
        CorrelationModel() {
            super("Correlation", "Session", "Previous", "Current", "State", "Token Rotated",
                    "Previous Verified", "Current Verified", "Drift", "Evidence", "Reasons");
        }

        void update(List<SessionCorrelationResult> correlations) {
            List<Object[]> rows = new ArrayList<>();
            for (var correlation : correlations) {
                rows.add(new Object[]{
                        correlation.correlationId(),
                        correlation.sessionId(),
                        correlation.previousObservationId(),
                        correlation.currentObservationId(),
                        correlation.state(),
                        correlation.tokenRotated(),
                        correlation.previousIdentityVerified(),
                        correlation.currentIdentityVerified(),
                        correlation.driftDimensions(),
                        correlation.evidenceIds().size(),
                        correlation.reasons()});
            }
            rows(rows);
        }
    }

    private static final class AssessmentModel extends RowsModel {
        AssessmentModel() {
            super("Assessment", "Session", "State", "Token Rotated", "Identity Verified",
                    "Drift", "Evidence", "Rationale", "Reasons");
        }

        void update(List<SessionSecurityAssessment> assessments) {
            List<Object[]> rows = new ArrayList<>();
            for (var assessment : assessments) {
                rows.add(new Object[]{
                        assessment.assessmentId(),
                        assessment.sessionId(),
                        assessment.state(),
                        assessment.tokenRotated(),
                        assessment.identityVerified(),
                        assessment.driftDimensions(),
                        assessment.evidenceIds().size(),
                        assessment.rationale(),
                        assessment.reasons()});
            }
            rows(rows);
        }
    }

    private static final class CandidateModel extends RowsModel {
        CandidateModel() {
            super("Candidate", "State", "Endpoint", "Context", "Principal", "Tenant",
                    "Confidence", "Rule", "Dimensions", "Evidence", "Rationale");
        }

        void update(List<FindingCandidate> candidates) {
            List<Object[]> rows = new ArrayList<>();
            for (var candidate : candidates) {
                rows.add(new Object[]{
                        candidate.candidateId(),
                        candidate.state(),
                        candidate.endpoint(),
                        candidate.resourceId(),
                        candidate.principalId(),
                        candidate.tenantRelationship(),
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
            super("Coverage", "Target", "Session", "Objective", "Rule", "Disposition",
                    "Observations", "Correlations", "Assessments", "Findings");
        }

        void update(List<SessionCoverageEntry> entries) {
            List<Object[]> rows = new ArrayList<>();
            for (var entry : entries) {
                rows.add(new Object[]{
                        entry.target().coverageId(),
                        entry.target().targetId(),
                        entry.target().sessionId(),
                        entry.target().objective(),
                        entry.target().ruleReference(),
                        entry.disposition(),
                        entry.observationIds().size(),
                        entry.correlationIds().size(),
                        entry.assessmentIds().size(),
                        entry.findingCandidateIds().size()});
            }
            rows(rows);
        }
    }
}
