package io.acra.burp.tests.sprint6;

import io.acra.burp.scope.ScopeController;
import io.acra.burp.traffic.TrafficIntelligencePipeline;
import io.acra.burp.ui.AcraSuiteTab;
import io.acra.core.active.product.ActiveEngineWorkspace;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.AuthorizationPolicyCoverage;
import io.acra.core.domain.authorization.AuthorizationPolicySnapshot;
import io.acra.core.domain.authorization.AuthorizationRule;
import io.acra.core.domain.authorization.AuthorizationRuleEffect;
import io.acra.core.domain.authorization.AuthorizationScope;
import io.acra.core.domain.authorization.Delegation;
import io.acra.core.domain.authorization.EffectiveAuthorizationMatrixEntry;
import io.acra.core.domain.authorization.EffectiveAuthorizationResolution;
import io.acra.core.domain.authorization.Permission;
import io.acra.core.domain.authorization.PolicyConflictAssessment;
import io.acra.core.domain.authorization.PolicyResolutionState;
import io.acra.core.domain.authorization.RbacAssessment;
import io.acra.core.domain.authorization.RbacAssessmentState;
import io.acra.core.domain.authorization.RoleAssignment;
import io.acra.core.domain.authorization.RoleEscalationAssessment;
import io.acra.core.domain.authorization.RoleEscalationAssessmentState;
import io.acra.core.domain.authorization.RoleInheritance;
import io.acra.core.domain.authorization.RolePermissionAssignment;
import io.acra.core.domain.authorization.S6AuthorizationAnalysisResult;
import io.acra.core.domain.authorization.TenantIsolationAssessment;
import io.acra.core.domain.authorization.TenantIsolationAssessmentState;
import io.acra.core.domain.authorization.TenantMembership;
import io.acra.core.domain.authorization.TenantMembershipType;
import io.acra.core.domain.authorization.TenantRelationship;
import io.acra.core.product.authorization.S6AuthorizationWorkspace;
import java.awt.Component;
import java.awt.Container;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public final class Sprint6AuthorizationUiTestSuite {
    private static final Instant NOW = Instant.parse("2026-09-23T16:00:00Z");
    private static int assertions;

    private Sprint6AuthorizationUiTestSuite() { }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        S6AuthorizationWorkspace authorization = fixtureWorkspace();
        AtomicReference<AcraSuiteTab> reference = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> reference.set(new AcraSuiteTab(
                new TrafficIntelligencePipeline(20),
                new ScopeController(),
                new ActiveEngineWorkspace(Clock.systemUTC(), null),
                authorization)));
        AcraSuiteTab tab = reference.get();
        try {
            SwingUtilities.invokeAndWait(() -> verify(tab));
        } finally {
            SwingUtilities.invokeAndWait(tab::stop);
        }
        System.out.println("SPRINT6_AUTHORIZATION_UI PASS assertions=" + assertions);
    }

    private static void verify(AcraSuiteTab tab) {
        JTabbedPane top = find(tab.component(), JTabbedPane.class, null);
        check(indexOf(top, "Authorization") >= 0, "Authorization top-level tab installed");
        assertions++;

        JTabbedPane authTabs = find(tab.component(), JTabbedPane.class, "s6-authorization-tabs");
        Set<String> required = Set.of("Overview", "Policy", "Tenant Map", "Roles", "Role Hierarchy",
                "Permissions", "Effective Permissions", "Policy Conflicts", "Coverage");
        for (String title : required) {
            check(indexOf(authTabs, title) >= 0, "authorization sub-tab installed: " + title);
            assertions++;
        }

        JTextArea overview = find(tab.component(), JTextArea.class, "s6-auth-overview");
        check(overview.getText().contains("s6-ui-policy"), "overview renders loaded policy");
        assertions++;
        check(overview.getText().contains("Candidate != confirmed vulnerability"),
                "overview preserves finding-candidate evidence boundary");
        assertions++;

        check(rows(tab, "s6-tenant-map-table") == 2, "tenant map renders membership and delegation");
        assertions++;
        check(rows(tab, "s6-roles-table") == 2, "roles table renders assignments");
        assertions++;
        check(rows(tab, "s6-role-hierarchy-table") == 1, "role hierarchy renders edge");
        assertions++;
        check(rows(tab, "s6-permissions-table") == 2, "permissions table renders catalog");
        assertions++;
        check(rows(tab, "s6-effective-permissions-table") == 1, "effective matrix renders resolution");
        assertions++;
        check(rows(tab, "s6-policy-conflicts-table") == 1, "conflict table renders assessment");
        assertions++;
        check(rows(tab, "s6-coverage-table") == 1, "coverage table renders analysis coverage");
        assertions++;

        JTextArea policy = find(tab.component(), JTextArea.class, "s6-policy-view");
        check(policy.getText().contains("rule-deny"), "policy view renders explicit rule");
        assertions++;
        check(policy.getText().contains("delegation-a-b"), "policy view renders delegation");
        assertions++;
        check(tab.authorizationWorkspace().snapshot().analyses().size() == 1,
                "suite tab exposes the same S6 product workspace");
        assertions++;
    }

    private static S6AuthorizationWorkspace fixtureWorkspace() {
        S6AuthorizationWorkspace workspace = new S6AuthorizationWorkspace();
        AuthorizationPolicySnapshot policy = AuthorizationPolicySnapshot.create(
                "s6-ui-policy", "1", "controlled-ui-fixture",
                List.of(new TenantMembership("tm-a", "user-a", "tenant-a",
                        TenantMembershipType.DIRECT, true, List.of("e-tm"))),
                List.of(
                        new RoleAssignment("ra-viewer", "user-a", "viewer", "tenant-a",
                                AuthorizationScope.tenant("tenant-a"), true, List.of("e-role-viewer")),
                        new RoleAssignment("ra-admin", "admin-a", "admin", "tenant-a",
                                AuthorizationScope.tenant("tenant-a"), true, List.of("e-role-admin"))),
                List.of(new RoleInheritance("rh-admin-viewer", "admin", "viewer", "tenant-a",
                        List.of("e-hierarchy"))),
                List.of(
                        new Permission("p-read", "READ_REPORT", "report", "", "",
                                AuthorizationScope.tenant("tenant-a"), List.of("e-p-read")),
                        new Permission("p-admin", "READ_ADMIN_SUMMARY", "admin-summary", "", "",
                                AuthorizationScope.tenant("tenant-a"), List.of("e-p-admin"))),
                List.of(
                        new RolePermissionAssignment("rp-viewer", "viewer", "p-read", "tenant-a",
                                List.of("e-rp-viewer")),
                        new RolePermissionAssignment("rp-admin", "admin", "p-admin", "tenant-a",
                                List.of("e-rp-admin"))),
                List.of(new AuthorizationRule("rule-deny", AuthorizationRuleEffect.DENY, "user-a", "",
                        "p-admin", "tenant-a", AuthorizationScope.tenant("tenant-a"), 10,
                        "explicit-ui-fixture", List.of("e-rule"))),
                List.of(new Delegation("delegation-a-b", "admin-b", "delegate-a", "tenant-a", "tenant-b",
                        "delegated-admin", List.of("READ_REPORT"), AuthorizationScope.tenant("tenant-b"),
                        NOW.minusSeconds(60), NOW.plusSeconds(3600), List.of("e-delegation"))),
                List.of("e-policy"), AuthorizationDecision.DENY, NOW);
        workspace.loadPolicy(policy);

        EffectiveAuthorizationResolution resolution = new EffectiveAuthorizationResolution(
                "resolution-ui-1", policy.fingerprint(), "user-a", "tenant-a", TenantRelationship.SAME_TENANT,
                List.of("viewer"), List.of("p-read"), List.of("rule-deny"), List.of(),
                AuthorizationDecision.DENY, AuthorizationDecision.ALLOW, PolicyResolutionState.CONFLICTING,
                List.of("e-policy", "e-rule"), List.of("explicit allow/deny conflict"));
        EffectiveAuthorizationMatrixEntry matrix = new EffectiveAuthorizationMatrixEntry(
                resolution.resolutionId(), "user-a", List.of("viewer"), "tenant-a", "tenant-a",
                TenantRelationship.SAME_TENANT, "admin-summary", "READ_ADMIN_SUMMARY",
                "/api/v1/s6/tenants/tenant-a/admin/summary", policy.fingerprint(),
                AuthorizationDecision.DENY, AuthorizationDecision.ALLOW, PolicyResolutionState.CONFLICTING,
                List.of("e-policy", "e-rule"));
        PolicyConflictAssessment conflict = new PolicyConflictAssessment(
                "conflict-ui-1", true, PolicyResolutionState.CONFLICTING,
                List.of("explicit allow/deny conflict"), List.of("e-rule"));
        AuthorizationPolicyCoverage coverage = new AuthorizationPolicyCoverage(
                true, true, true, true, true, false);
        workspace.record(new S6AuthorizationAnalysisResult(
                null,
                resolution,
                new TenantIsolationAssessment("tenant-ui", TenantRelationship.SAME_TENANT,
                        AuthorizationDecision.DENY, AuthorizationDecision.ALLOW,
                        TenantIsolationAssessmentState.CONFLICTING, List.of("e-rule"), "fixture"),
                new RbacAssessment("rbac-ui", List.of("viewer"), List.of("p-read"),
                        AuthorizationDecision.DENY, AuthorizationDecision.ALLOW,
                        RbacAssessmentState.CONFLICTING, List.of("e-rule"), "fixture"),
                new RoleEscalationAssessment("role-ui", true, "admin", List.of("viewer"),
                        AuthorizationDecision.DENY, AuthorizationDecision.ALLOW,
                        RoleEscalationAssessmentState.CONFLICTING, List.of("e-rule"), "fixture"),
                conflict,
                coverage,
                matrix,
                null,
                null));
        return workspace;
    }

    private static int rows(AcraSuiteTab tab, String name) {
        JTable table = find(tab.component(), JTable.class, name);
        if (table == null) throw new AssertionError("table missing: " + name);
        return table.getRowCount();
    }

    private static int indexOf(JTabbedPane tabs, String title) {
        for (int index = 0; index < tabs.getTabCount(); index++) {
            if (title.equals(tabs.getTitleAt(index))) return index;
        }
        return -1;
    }

    private static <T extends Component> T find(Component root, Class<T> type, String name) {
        if (root == null) return null;
        if (type.isInstance(root) && (name == null
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
