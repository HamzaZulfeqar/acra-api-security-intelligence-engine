package io.acra.burp.tests.sprint9;

import io.acra.burp.scope.ScopeController;
import io.acra.burp.traffic.TrafficIntelligencePipeline;
import io.acra.burp.ui.AcraSuiteTab;
import io.acra.core.active.product.ActiveEngineWorkspace;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.PolicyValidationState;
import io.acra.core.domain.authorization.PropertyAuthorizationAssessment;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.engine.PolicyValidationEvaluator;
import io.acra.core.product.authorization.S6AuthorizationWorkspace;
import io.acra.core.product.property.S9PropertyWorkspace;
import io.acra.core.product.routing.S8RoutingWorkspace;
import io.acra.core.product.workflow.S7WorkflowWorkspace;
import io.acra.core.property.PropertyAccessObservation;
import io.acra.core.property.S9PropertyCoverageTracker;
import java.awt.Component;
import java.awt.Container;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public final class Sprint9PropertyUiTestSuite {
    private static final String ENDPOINT = "/api/v1/s9/users/user-a/profile";
    private static int assertions;

    private Sprint9PropertyUiTestSuite() { }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        S9PropertyWorkspace property = fixtureWorkspace();
        AtomicReference<AcraSuiteTab> reference = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> reference.set(new AcraSuiteTab(
                new TrafficIntelligencePipeline(20),
                new ScopeController(),
                new ActiveEngineWorkspace(Clock.systemUTC(), null),
                new S6AuthorizationWorkspace(),
                new S7WorkflowWorkspace(),
                new S8RoutingWorkspace(),
                property)));
        AcraSuiteTab tab = reference.get();
        try {
            SwingUtilities.invokeAndWait(() -> verify(tab));
        } finally {
            SwingUtilities.invokeAndWait(tab::stop);
        }
        System.out.println("SPRINT9_PROPERTY_UI PASS assertions=" + assertions);
    }

    private static void verify(AcraSuiteTab tab) {
        JTabbedPane top = find(tab.component(), JTabbedPane.class, null);
        check(indexOf(top, "Properties") >= 0, "Properties top-level tab installed");
        assertions++;

        JTabbedPane propertyTabs = find(tab.component(), JTabbedPane.class, "s9-property-tabs");
        for (String title : Set.of(
                "Overview", "Policies", "Observations", "Assessments", "Candidates", "Coverage")) {
            check(indexOf(propertyTabs, title) >= 0, "property sub-tab installed: " + title);
            assertions++;
        }

        JTextArea overview = find(tab.component(), JTextArea.class, "s9-property-overview");
        check(overview.getText().contains("Policy contexts: 3"),
                "property overview renders explicit policy denominator");
        assertions++;
        check(overview.getText().contains("READ contexts: 1"),
                "property overview keeps READ coverage distinct");
        assertions++;
        check(overview.getText().contains("UPDATE contexts: 2"),
                "property overview keeps UPDATE coverage distinct");
        assertions++;
        check(overview.getText().contains("Observed contexts: 2"),
                "property overview renders observed coverage");
        assertions++;
        check(overview.getText().contains("Assessed contexts: 2"),
                "property overview renders assessed coverage");
        assertions++;
        check(overview.getText().contains("Unobserved contexts: 1"),
                "property overview preserves unobserved coverage");
        assertions++;
        check(overview.getText().contains("Finding candidates: 1"),
                "property overview renders one review candidate");
        assertions++;
        check(overview.getText().contains("Rejected controls: 1"),
                "property overview renders one rejected secure control");
        assertions++;
        check(overview.getText().contains("Candidate != confirmed vulnerability"),
                "property overview preserves review-only boundary");
        assertions++;
        check(overview.getText().contains("Unobserved != secure"),
                "property overview does not label missing coverage as secure");
        assertions++;
        check(overview.getText().contains("Property values are not rendered"),
                "property overview states value-minimization boundary");
        assertions++;
        check(overview.getText().contains("Real Burp desktop runtime validation remains separate"),
                "headless UI test does not imply real Burp validation");
        assertions++;

        check(rows(tab, "s9-property-policy-table") == 3,
                "property policy table renders three explicit policies");
        assertions++;
        check(rows(tab, "s9-property-observation-table") == 2,
                "property observation table renders two evidence-backed observations");
        assertions++;
        check(rows(tab, "s9-property-assessment-table") == 2,
                "property assessment table renders two reviewed contexts");
        assertions++;
        check(rows(tab, "s9-property-candidate-table") == 2,
                "property candidate table includes candidate and rejected control");
        assertions++;
        check(rows(tab, "s9-property-coverage-table") == 3,
                "property coverage table includes unobserved policy context");
        assertions++;

        var snapshot = tab.propertyWorkspace().snapshot();
        check(snapshot.candidateCount() == 1,
                "property workspace exposes one candidate without confirmation promotion");
        assertions++;
        check(snapshot.rejectedCount() == 1,
                "property workspace exposes one rejected secure control");
        assertions++;
        check(snapshot.coverageSummary().unobservedContexts() == 1,
                "property workspace retains one unobserved context");
        assertions++;

        JTable policies = find(tab.component(), JTable.class, "s9-property-policy-table");
        for (int row = 0; row < policies.getRowCount(); row++) {
            for (int column = 0; column < policies.getColumnCount(); column++) {
                String value = String.valueOf(policies.getValueAt(row, column));
                check(!value.contains("L2") && !value.contains("User A Updated"),
                        "property UI must not render synthetic property values");
                assertions++;
            }
        }
    }

    private static S9PropertyWorkspace fixtureWorkspace() {
        S9PropertyWorkspace workspace = new S9PropertyWorkspace();
        S9PropertyCoverageTracker coverage = new S9PropertyCoverageTracker();

        var salaryRead = policy(
                "s9-ui-salary-read", "salary_band",
                PolicyValidationEvaluator.PropertyOperation.READ, AuthorizationDecision.DENY);
        var adminUpdate = policy(
                "s9-ui-admin-update", "is_admin",
                PolicyValidationEvaluator.PropertyOperation.UPDATE, AuthorizationDecision.DENY);
        var displayUpdate = policy(
                "s9-ui-display-update", "display_name",
                PolicyValidationEvaluator.PropertyOperation.UPDATE, AuthorizationDecision.ALLOW);

        for (var policy : List.of(salaryRead, adminUpdate, displayUpdate)) {
            workspace.recordPolicy(policy);
            coverage.recordPolicy(policy);
        }

        PropertyAccessObservation salaryObservation = observation(
                "s9-ui-obs-salary", "salary_band",
                PolicyValidationEvaluator.PropertyOperation.READ, AuthorizationDecision.DENY);
        PropertyAuthorizationAssessment salaryAssessment = assessment(
                "s9-ui-assessment-salary", salaryRead,
                AuthorizationDecision.DENY, PolicyValidationState.DENIED);
        FindingCandidate salaryFinding = finding(
                "s9-ui-finding-salary", salaryAssessment, FindingCandidateState.REJECTED);
        workspace.recordObservation(salaryObservation);
        workspace.recordAssessment(salaryAssessment);
        workspace.recordCandidate(salaryFinding);
        coverage.recordObservation(salaryRead, salaryObservation);
        coverage.recordAssessment(salaryRead, salaryObservation, salaryAssessment, salaryFinding);

        PropertyAccessObservation adminObservation = observation(
                "s9-ui-obs-admin", "is_admin",
                PolicyValidationEvaluator.PropertyOperation.UPDATE, AuthorizationDecision.ALLOW);
        PropertyAuthorizationAssessment adminAssessment = assessment(
                "s9-ui-assessment-admin", adminUpdate,
                AuthorizationDecision.ALLOW, PolicyValidationState.CONFLICTING);
        FindingCandidate adminFinding = finding(
                "s9-ui-finding-admin", adminAssessment, FindingCandidateState.CANDIDATE);
        workspace.recordObservation(adminObservation);
        workspace.recordAssessment(adminAssessment);
        workspace.recordCandidate(adminFinding);
        coverage.recordObservation(adminUpdate, adminObservation);
        coverage.recordAssessment(adminUpdate, adminObservation, adminAssessment, adminFinding);

        workspace.replaceCoverage(coverage);
        return workspace;
    }

    private static PolicyValidationEvaluator.PropertyPolicy policy(
            String reference,
            String property,
            PolicyValidationEvaluator.PropertyOperation operation,
            AuthorizationDecision expected) {
        return new PolicyValidationEvaluator.PropertyPolicy(
                reference,
                "GT-S9-PROPERTY-AUTHORIZATION",
                ENDPOINT,
                property,
                operation,
                "viewer",
                "tenant-a",
                expected,
                List.of("evidence-" + reference));
    }

    private static PropertyAccessObservation observation(
            String id,
            String property,
            PolicyValidationEvaluator.PropertyOperation operation,
            AuthorizationDecision observed) {
        return new PropertyAccessObservation(
                id,
                "s9-ui-execution",
                "s9-ui-test",
                ENDPOINT,
                property,
                operation,
                observed,
                List.of("evidence-" + id));
    }

    private static PropertyAuthorizationAssessment assessment(
            String id,
            PolicyValidationEvaluator.PropertyPolicy policy,
            AuthorizationDecision observed,
            PolicyValidationState state) {
        return new PropertyAuthorizationAssessment(
                id,
                policy.endpoint(),
                policy.property(),
                policy.operation().name(),
                policy.roleId(),
                policy.tenantId(),
                policy.policyReference(),
                policy.expectedDecision(),
                observed,
                state,
                state == PolicyValidationState.INCONCLUSIVE ? "INSUFFICIENT" : "HIGH",
                List.of("evidence-" + id),
                "S9 UI fixture",
                List.of());
    }

    private static FindingCandidate finding(
            String id,
            PropertyAuthorizationAssessment assessment,
            FindingCandidateState state) {
        return new FindingCandidate(
                id,
                state,
                "acra-s9-ui",
                List.of("s9-ui-test"),
                List.of("s9-ui-execution"),
                List.of("s9-ui-observation"),
                List.of(assessment.assessmentId()),
                List.of("PROPERTY", "PROPERTY_" + assessment.operation()),
                assessment.endpoint(),
                "user-a",
                "user-a",
                "tenant-a",
                assessment.expectedDecision(),
                assessment.observedDecision(),
                assessment.evidenceIds(),
                List.of(),
                List.of(assessment.policyReference()),
                state == FindingCandidateState.CANDIDATE ? "HIGH" : "MEDIUM",
                "S9 UI fixture; candidate is not an automatically confirmed vulnerability",
                FindingFingerprint.of(
                        assessment.endpoint(),
                        "user-a#" + assessment.property(),
                        "user-a",
                        "tenant-a",
                        "PROPERTY_" + assessment.operation(),
                        state.name()));
    }

    private static int rows(AcraSuiteTab tab, String name) {
        JTable table = find(tab.component(), JTable.class, name);
        if (table == null) throw new AssertionError("table missing: " + name);
        return table.getRowCount();
    }

    private static int indexOf(JTabbedPane tabs, String title) {
        if (tabs == null) return -1;
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
