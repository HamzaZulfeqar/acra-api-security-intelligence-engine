package io.acra.burp.ui;

import io.acra.core.domain.authorization.AuthorizationPolicySnapshot;
import io.acra.core.domain.authorization.AuthorizationPolicyCoverage;
import io.acra.core.domain.authorization.AuthorizationRule;
import io.acra.core.domain.authorization.Delegation;
import io.acra.core.domain.authorization.EffectiveAuthorizationMatrixEntry;
import io.acra.core.domain.authorization.Permission;
import io.acra.core.domain.authorization.PolicyConflictAssessment;
import io.acra.core.domain.authorization.RoleAssignment;
import io.acra.core.domain.authorization.RoleInheritance;
import io.acra.core.domain.authorization.RolePermissionAssignment;
import io.acra.core.domain.authorization.S6AuthorizationAnalysisResult;
import io.acra.core.domain.authorization.TenantMembership;
import io.acra.core.product.authorization.S6AuthorizationProductSnapshot;
import io.acra.core.product.authorization.S6AuthorizationWorkspace;
import java.awt.BorderLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.AbstractTableModel;

@SuppressWarnings("serial")
public final class S6AuthorizationPanel extends JPanel {
    private final S6AuthorizationWorkspace workspace;
    private final JTextArea overview = view("s6-auth-overview");
    private final JTextArea policyView = view("s6-policy-view");
    private final JTextArea reportView = view("s6-report-view");
    private final JTextArea jsonExportView = view("s6-json-export-view");
    private final TenantModel tenantModel = new TenantModel();
    private final RoleModel roleModel = new RoleModel();
    private final HierarchyModel hierarchyModel = new HierarchyModel();
    private final PermissionModel permissionModel = new PermissionModel();
    private final MatrixModel matrixModel = new MatrixModel();
    private final ConflictModel conflictModel = new ConflictModel();
    private final CoverageModel coverageModel = new CoverageModel();

    public S6AuthorizationPanel(S6AuthorizationWorkspace workspace) {
        super(new BorderLayout());
        if (workspace == null) throw new IllegalArgumentException("authorization workspace required");
        this.workspace = workspace;
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        add(buildTabs(), BorderLayout.CENTER);
        refresh();
    }

    public void install(JTabbedPane tabs) {
        if (tabs == null) throw new IllegalArgumentException("tabs required");
        tabs.addTab("Authorization", this);
    }

    public S6AuthorizationWorkspace workspace() {
        return workspace;
    }

    public void refresh() {
        S6AuthorizationProductSnapshot snapshot = workspace.snapshot();
        renderOverview(snapshot);
        renderPolicy(snapshot.policy());
        tenantModel.update(snapshot.policy());
        roleModel.update(snapshot.policy());
        hierarchyModel.update(snapshot.policy());
        permissionModel.update(snapshot.policy());
        matrixModel.update(snapshot.analyses());
        conflictModel.update(snapshot.analyses());
        coverageModel.update(snapshot.analyses());
        java.time.Instant previewAt=snapshot.policy()==null?java.time.Instant.EPOCH:snapshot.policy().capturedAt();
        reportView.setText(workspace.exportMarkdown(previewAt).content());
        jsonExportView.setText(workspace.exportJson(previewAt).content());
    }

    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setName("s6-authorization-tabs");
        tabs.addTab("Overview", scroll(overview));
        tabs.addTab("Policy", scroll(policyView));
        tabs.addTab("Tenant Map", table(tenantModel, "s6-tenant-map-table"));
        tabs.addTab("Roles", table(roleModel, "s6-roles-table"));
        tabs.addTab("Role Hierarchy", table(hierarchyModel, "s6-role-hierarchy-table"));
        tabs.addTab("Permissions", table(permissionModel, "s6-permissions-table"));
        tabs.addTab("Effective Permissions", table(matrixModel, "s6-effective-permissions-table"));
        tabs.addTab("Policy Conflicts", table(conflictModel, "s6-policy-conflicts-table"));
        tabs.addTab("Coverage", table(coverageModel, "s6-coverage-table"));
        tabs.addTab("Report", scroll(reportView));
        tabs.addTab("JSON Export", scroll(jsonExportView));
        return tabs;
    }

    private void renderOverview(S6AuthorizationProductSnapshot snapshot) {
        AuthorizationPolicySnapshot policy = snapshot.policy();
        StringBuilder text = new StringBuilder();
        text.append("SPRINT 6 AUTHORIZATION INTELLIGENCE\n\n");
        if (policy == null) {
            text.append("Policy: NOT LOADED\n")
                    .append("Analyses: ").append(snapshot.analyses().size()).append('\n')
                    .append("No authorization verdict is inferred without explicit policy/context evidence.");
            overview.setText(text.toString());
            return;
        }
        long conflicts = snapshot.analyses().stream()
                .filter(value -> value.policyConflict() != null && value.policyConflict().conflicting()).count();
        long candidates = snapshot.analyses().stream()
                .filter(value -> value.findingCandidate() != null
                        && value.findingCandidate().state().name().equals("CANDIDATE")).count();
        text.append("Policy: ").append(policy.policyId()).append(" v").append(policy.version()).append('\n')
                .append("Source: ").append(policy.source()).append('\n')
                .append("Default decision: ").append(policy.defaultDecision()).append('\n')
                .append("Fingerprint: ").append(policy.fingerprint()).append('\n')
                .append("Tenant memberships: ").append(policy.memberships().size()).append('\n')
                .append("Role assignments: ").append(policy.roleAssignments().size()).append('\n')
                .append("Role inheritance edges: ").append(policy.roleInheritances().size()).append('\n')
                .append("Permissions: ").append(policy.permissions().size()).append('\n')
                .append("Rules: ").append(policy.rules().size()).append('\n')
                .append("Delegations: ").append(policy.delegations().size()).append('\n')
                .append("Effective analyses: ").append(snapshot.analyses().size()).append('\n')
                .append("Policy conflicts: ").append(conflicts).append('\n')
                .append("Finding candidates: ").append(candidates).append('\n')
                .append("\nCandidate != confirmed vulnerability.");
        overview.setText(text.toString());
    }

    private void renderPolicy(AuthorizationPolicySnapshot policy) {
        if (policy == null) {
            policyView.setText("No policy loaded.");
            return;
        }
        StringBuilder text = new StringBuilder();
        text.append("POLICY METADATA\n")
                .append("ID: ").append(policy.policyId()).append('\n')
                .append("Version: ").append(policy.version()).append('\n')
                .append("Source: ").append(policy.source()).append('\n')
                .append("Default: ").append(policy.defaultDecision()).append('\n')
                .append("Captured: ").append(policy.capturedAt()).append('\n')
                .append("Evidence: ").append(policy.evidenceIds()).append("\n\nRULES\n");
        for (AuthorizationRule rule : policy.rules()) {
            text.append(rule.ruleId()).append(" | ").append(rule.effect())
                    .append(" | principal=").append(rule.principalId())
                    .append(" | role=").append(rule.roleId())
                    .append(" | permission=").append(rule.permissionId())
                    .append(" | tenant=").append(rule.tenantId())
                    .append(" | scope=").append(rule.scope().type())
                    .append(" | precedence=").append(rule.precedence())
                    .append(" | evidence=").append(rule.evidenceIds()).append('\n');
        }
        text.append("\nDELEGATIONS\n");
        for (Delegation delegation : policy.delegations()) {
            text.append(delegation.delegationId())
                    .append(" | ").append(delegation.delegatorPrincipalId())
                    .append(" -> ").append(delegation.delegatePrincipalId())
                    .append(" | ").append(delegation.sourceTenantId())
                    .append(" -> ").append(delegation.targetTenantId())
                    .append(" | role=").append(delegation.roleId())
                    .append(" | actions=").append(delegation.actions())
                    .append(" | scope=").append(delegation.scope().type())
                    .append('\n');
        }
        policyView.setText(text.toString());
    }

    private static JComponent table(AbstractTableModel model, String name) {
        JTable table = new JTable(model);
        table.setName(name);
        table.setAutoCreateRowSorter(true);
        return new JScrollPane(table);
    }

    private static JScrollPane scroll(JTextArea area) {
        return new JScrollPane(area);
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

    private static final class TenantModel extends RowsModel {
        TenantModel() {
            super("Kind", "Principal", "Tenant", "Role / Type", "Scope", "Active", "Evidence");
        }

        void update(AuthorizationPolicySnapshot policy) {
            List<Object[]> rows = new ArrayList<>();
            if (policy != null) {
                for (TenantMembership membership : policy.memberships()) {
                    rows.add(new Object[]{"MEMBERSHIP", membership.principalId(), membership.tenantId(),
                            membership.type(), "TENANT", membership.active(), membership.evidenceIds().size()});
                }
                for (Delegation delegation : policy.delegations()) {
                    rows.add(new Object[]{"DELEGATION", delegation.delegatePrincipalId(),
                            delegation.targetTenantId(), delegation.roleId(), delegation.scope().type(),
                            delegation.activeAt(policy.capturedAt()), delegation.evidenceIds().size()});
                }
            }
            rows(rows);
        }
    }

    private static final class RoleModel extends RowsModel {
        RoleModel() {
            super("Assignment", "Principal", "Role", "Tenant", "Scope", "Active", "Evidence");
        }

        void update(AuthorizationPolicySnapshot policy) {
            List<Object[]> rows = new ArrayList<>();
            if (policy != null) {
                for (RoleAssignment assignment : policy.roleAssignments()) {
                    rows.add(new Object[]{assignment.assignmentId(), assignment.principalId(), assignment.roleId(),
                            assignment.tenantId(), assignment.scope().type(), assignment.active(),
                            assignment.evidenceIds().size()});
                }
            }
            rows(rows);
        }
    }

    private static final class HierarchyModel extends RowsModel {
        HierarchyModel() {
            super("Inheritance", "Child Role", "Parent Role", "Tenant", "Evidence");
        }

        void update(AuthorizationPolicySnapshot policy) {
            List<Object[]> rows = new ArrayList<>();
            if (policy != null) {
                for (RoleInheritance inheritance : policy.roleInheritances()) {
                    rows.add(new Object[]{inheritance.inheritanceId(), inheritance.childRoleId(),
                            inheritance.parentRoleId(), inheritance.tenantId(), inheritance.evidenceIds().size()});
                }
            }
            rows(rows);
        }
    }

    private static final class PermissionModel extends RowsModel {
        PermissionModel() {
            super("Permission", "Action", "Resource", "Endpoint", "Property", "Scope", "Roles", "Evidence");
        }

        void update(AuthorizationPolicySnapshot policy) {
            List<Object[]> rows = new ArrayList<>();
            if (policy != null) {
                Map<String, List<String>> rolesByPermission = new TreeMap<>();
                for (RolePermissionAssignment assignment : policy.rolePermissionAssignments()) {
                    rolesByPermission.computeIfAbsent(assignment.permissionId(), ignored -> new ArrayList<>())
                            .add(assignment.roleId());
                }
                for (Permission permission : policy.permissions()) {
                    List<String> roles = rolesByPermission.getOrDefault(permission.permissionId(), List.of()).stream()
                            .distinct().sorted().toList();
                    rows.add(new Object[]{permission.permissionId(), permission.action(), permission.resourceType(),
                            permission.endpoint(), permission.property(), permission.scope().type(), roles,
                            permission.evidenceIds().size()});
                }
            }
            rows(rows);
        }
    }

    private static final class MatrixModel extends RowsModel {
        MatrixModel() {
            super("Resolution", "Principal", "Roles", "Subject Tenant", "Resource Tenant", "Relationship",
                    "Resource", "Action", "Expected", "Observed", "State", "Evidence");
        }

        void update(List<S6AuthorizationAnalysisResult> analyses) {
            List<Object[]> rows = new ArrayList<>();
            for (S6AuthorizationAnalysisResult analysis : analyses) {
                EffectiveAuthorizationMatrixEntry entry = analysis.matrixEntry();
                if (entry == null) continue;
                rows.add(new Object[]{entry.resolutionId(), entry.principalId(), entry.effectiveRoleIds(),
                        entry.subjectTenantId(), entry.resourceTenantId(), entry.tenantRelationship(),
                        entry.resourceId(), entry.action(), entry.expectedDecision(), entry.observedDecision(),
                        entry.state(), entry.evidenceIds().size()});
            }
            rows(rows);
        }
    }

    private static final class ConflictModel extends RowsModel {
        ConflictModel() {
            super("Assessment", "Conflicting", "State", "Reasons", "Evidence");
        }

        void update(List<S6AuthorizationAnalysisResult> analyses) {
            List<Object[]> rows = new ArrayList<>();
            for (S6AuthorizationAnalysisResult analysis : analyses) {
                PolicyConflictAssessment conflict = analysis.policyConflict();
                if (conflict == null) continue;
                rows.add(new Object[]{conflict.assessmentId(), conflict.conflicting(),
                        conflict.resolutionState(), conflict.reasons(), conflict.evidenceIds().size()});
            }
            rows(rows);
        }
    }

    private static final class CoverageModel extends RowsModel {
        CoverageModel() {
            super("Resolution", "Known", "Total", "Ratio", "Missing Dimensions");
        }

        void update(List<S6AuthorizationAnalysisResult> analyses) {
            List<Object[]> rows = new ArrayList<>();
            for (S6AuthorizationAnalysisResult analysis : analyses) {
                AuthorizationPolicyCoverage coverage = analysis.coverage();
                if (coverage == null || analysis.effectiveResolution() == null) continue;
                rows.add(new Object[]{analysis.effectiveResolution().resolutionId(), coverage.knownCount(),
                        coverage.total(), String.format(java.util.Locale.ROOT, "%.2f", coverage.ratio()),
                        coverage.missingDimensions()});
            }
            rows(rows);
        }
    }
}
