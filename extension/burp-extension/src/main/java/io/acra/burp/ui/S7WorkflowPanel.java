package io.acra.burp.ui;

import io.acra.core.domain.authorization.PolicyResolutionState;
import io.acra.core.domain.workflow.S7WorkflowAnalysisResult;
import io.acra.core.domain.workflow.WorkflowAuthorizationResolution;
import io.acra.core.domain.workflow.WorkflowPolicySnapshot;
import io.acra.core.domain.workflow.WorkflowTransitionCoverageEntry;
import io.acra.core.domain.workflow.WorkflowTransitionRule;
import io.acra.core.product.workflow.S7WorkflowProductSnapshot;
import io.acra.core.product.workflow.S7WorkflowWorkspace;
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
public final class S7WorkflowPanel extends JPanel {
    private final S7WorkflowWorkspace workspace;
    private final JTextArea overview = view("s7-workflow-overview");
    private final JTextArea reportView = view("s7-workflow-report-view");
    private final JTextArea jsonExportView = view("s7-workflow-json-export-view");
    private final WorkflowMapModel workflowMapModel = new WorkflowMapModel();
    private final TransitionMatrixModel transitionMatrixModel = new TransitionMatrixModel();
    private final ConflictModel conflictModel = new ConflictModel();
    private final CoverageModel coverageModel = new CoverageModel();

    public S7WorkflowPanel(S7WorkflowWorkspace workspace) {
        super(new BorderLayout());
        if (workspace == null) throw new IllegalArgumentException("workflow workspace required");
        this.workspace = workspace;
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        add(buildTabs(), BorderLayout.CENTER);
        refresh();
    }

    public void install(JTabbedPane tabs) {
        if (tabs == null) throw new IllegalArgumentException("tabs required");
        tabs.addTab("Workflow", this);
    }

    public S7WorkflowWorkspace workspace() {
        return workspace;
    }

    public void refresh() {
        S7WorkflowProductSnapshot snapshot = workspace.snapshot();
        renderOverview(snapshot);
        workflowMapModel.update(snapshot.policy());
        transitionMatrixModel.update(snapshot.resolutions());
        conflictModel.update(snapshot.resolutions());
        coverageModel.update(snapshot.coverageEntries());
        java.time.Instant previewAt = snapshot.policy() == null
                ? java.time.Instant.EPOCH : snapshot.policy().capturedAt();
        reportView.setText(workspace.exportMarkdown(previewAt).content());
        jsonExportView.setText(workspace.exportJson(previewAt).content());
    }

    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setName("s7-workflow-tabs");
        tabs.addTab("Overview", new JScrollPane(overview));
        tabs.addTab("Workflow Map", table(workflowMapModel, "s7-workflow-map-table"));
        tabs.addTab("Transition Matrix", table(transitionMatrixModel, "s7-transition-matrix-table"));
        tabs.addTab("Policy Conflicts", table(conflictModel, "s7-workflow-conflicts-table"));
        tabs.addTab("Coverage", table(coverageModel, "s7-workflow-coverage-table"));
        tabs.addTab("Report", new JScrollPane(reportView));
        tabs.addTab("JSON Export", new JScrollPane(jsonExportView));
        return tabs;
    }

    private void renderOverview(S7WorkflowProductSnapshot snapshot) {
        StringBuilder text = new StringBuilder("SPRINT 7 WORKFLOW AUTHORIZATION INTELLIGENCE\n\n");
        WorkflowPolicySnapshot policy = snapshot.policy();
        if (policy == null) {
            text.append("Workflow policy: NOT LOADED\n")
                    .append("Resolutions: ").append(snapshot.resolutions().size()).append('\n')
                    .append("No workflow authorization verdict is inferred without explicit policy/context evidence.");
            overview.setText(text.toString());
            return;
        }
        long conflicts = snapshot.resolutions().stream()
                .filter(value -> value.state() == PolicyResolutionState.CONFLICTING).count();
        long candidates = snapshot.analyses().stream()
                .filter(S7WorkflowPanel::candidate).count();
        var coverage = snapshot.coverageSummary();
        text.append("Policy: ").append(policy.policyId()).append(" v").append(policy.version()).append('\n')
                .append("Source: ").append(policy.source()).append('\n')
                .append("Fingerprint: ").append(policy.fingerprint()).append('\n')
                .append("Transition rules: ").append(policy.transitionRules().size()).append('\n')
                .append("Token bindings: ").append(policy.tokenBindings().size()).append('\n')
                .append("Resolutions: ").append(snapshot.resolutions().size()).append('\n')
                .append("Policy conflicts: ").append(conflicts).append('\n')
                .append("Finding candidates: ").append(candidates).append('\n')
                .append("Coverage contexts: ").append(coverage.totalContexts()).append('\n')
                .append("Resolved: ").append(coverage.resolvedContexts())
                .append(" | Planned: ").append(coverage.plannedContexts())
                .append(" | Attempted: ").append(coverage.attemptedContexts())
                .append(" | Observed: ").append(coverage.observedContexts()).append('\n')
                .append("Observation coverage: ")
                .append(String.format(java.util.Locale.ROOT, "%.2f", coverage.observationRatio())).append('\n')
                .append("\nCandidate != confirmed vulnerability. Coverage != vulnerability severity.");
        overview.setText(text.toString());
    }

    private static boolean candidate(S7WorkflowAnalysisResult result) {
        return result != null && result.findingCandidate() != null
                && result.findingCandidate().state().name().equals("CANDIDATE");
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

    private static final class WorkflowMapModel extends RowsModel {
        WorkflowMapModel() {
            super("Rule", "Workflow", "From", "Action", "To", "Effect", "Roles", "Approval", "SoD",
                    "Delegation", "Token Binding", "Evidence");
        }

        void update(WorkflowPolicySnapshot policy) {
            List<Object[]> rows = new ArrayList<>();
            if (policy != null) {
                for (WorkflowTransitionRule rule : policy.transitionRules()) {
                    rows.add(new Object[]{rule.ruleId(), rule.workflowId(), rule.fromState(), rule.action(),
                            rule.toState(), rule.effect(), rule.requiredRoleIds(), rule.approvalRequired(),
                            rule.roleSeparationRequired(), rule.delegationAllowed(), rule.tokenBindingId(),
                            rule.evidenceIds().size()});
                }
            }
            rows(rows);
        }
    }

    private static final class TransitionMatrixModel extends RowsModel {
        TransitionMatrixModel() {
            super("Resolution", "Workflow", "Principal", "Tenant", "Resource", "From", "Action", "To",
                    "Expected", "Observed", "State", "Rules", "Evidence");
        }

        void update(List<WorkflowAuthorizationResolution> resolutions) {
            List<Object[]> rows = new ArrayList<>();
            for (WorkflowAuthorizationResolution value : resolutions) {
                rows.add(new Object[]{value.resolutionId(), value.workflowId(), value.principalId(),
                        value.tenantId(), value.resourceId(), value.fromState(), value.action(), value.toState(),
                        value.expectedDecision(), value.observedDecision(), value.state(), value.matchedRuleIds(),
                        value.evidenceIds().size()});
            }
            rows(rows);
        }
    }

    private static final class ConflictModel extends RowsModel {
        ConflictModel() {
            super("Resolution", "Workflow", "Principal", "Transition", "State", "Reasons", "Evidence");
        }

        void update(List<WorkflowAuthorizationResolution> resolutions) {
            List<Object[]> rows = new ArrayList<>();
            for (WorkflowAuthorizationResolution value : resolutions) {
                if (value.state() != PolicyResolutionState.CONFLICTING) continue;
                rows.add(new Object[]{value.resolutionId(), value.workflowId(), value.principalId(),
                        value.fromState() + " --" + value.action() + "--> " + value.toState(),
                        value.state(), value.reasons(), value.evidenceIds().size()});
            }
            rows(rows);
        }
    }

    private static final class CoverageModel extends RowsModel {
        CoverageModel() {
            super("Coverage", "Workflow", "Principal", "Tenant", "Resource", "Transition", "Expected",
                    "Resolution", "Lifecycle", "Tests", "Executions", "Observations", "Evidence");
        }

        void update(List<WorkflowTransitionCoverageEntry> entries) {
            List<Object[]> rows = new ArrayList<>();
            for (WorkflowTransitionCoverageEntry value : entries) {
                rows.add(new Object[]{value.coverageId(), value.workflowId(), value.principalId(),
                        value.tenantId(), value.resourceId(),
                        value.fromState() + " --" + value.action() + "--> " + value.toState(),
                        value.expectedDecision(), value.resolutionState(), value.stage(),
                        value.plannedTestIds().size(), value.executionIds().size(), value.observationIds().size(),
                        value.policyEvidenceIds().size() + value.observationEvidenceIds().size()});
            }
            rows(rows);
        }
    }
}
