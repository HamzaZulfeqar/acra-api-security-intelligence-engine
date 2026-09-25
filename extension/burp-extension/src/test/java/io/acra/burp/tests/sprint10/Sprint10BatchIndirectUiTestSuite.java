package io.acra.burp.tests.sprint10;

import io.acra.burp.scope.ScopeController;
import io.acra.burp.traffic.TrafficIntelligencePipeline;
import io.acra.burp.ui.AcraSuiteTab;
import io.acra.core.active.product.ActiveEngineWorkspace;
import io.acra.core.batch.BatchItemAuthorizationAssessment;
import io.acra.core.batch.BatchItemObservation;
import io.acra.core.batch.BatchItemPolicy;
import io.acra.core.coverage.S10AuthorizationCoverageTracker;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.PolicyValidationState;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.product.authorization.S6AuthorizationWorkspace;
import io.acra.core.product.batchindirect.S10BatchIndirectWorkspace;
import io.acra.core.product.property.S9PropertyWorkspace;
import io.acra.core.product.routing.S8RoutingWorkspace;
import io.acra.core.product.workflow.S7WorkflowWorkspace;
import io.acra.core.reference.IndirectReferenceAuthorizationAssessment;
import io.acra.core.reference.IndirectReferencePolicy;
import io.acra.core.reference.IndirectReferenceResolution;
import io.acra.core.reference.IndirectReferenceSource;
import io.acra.core.security.TokenFingerprint;
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

public final class Sprint10BatchIndirectUiTestSuite {
    private static final String BATCH_ENDPOINT = "/api/v1/s10/documents/batch-read";
    private static final String INDIRECT_ENDPOINT = "/api/v1/s10/share/{alias}";
    private static int assertions;

    private Sprint10BatchIndirectUiTestSuite() { }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        S10BatchIndirectWorkspace workspace = fixtureWorkspace();
        AtomicReference<AcraSuiteTab> reference = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> reference.set(new AcraSuiteTab(
                new TrafficIntelligencePipeline(20),
                new ScopeController(),
                new ActiveEngineWorkspace(Clock.systemUTC(), null),
                new S6AuthorizationWorkspace(),
                new S7WorkflowWorkspace(),
                new S8RoutingWorkspace(),
                new S9PropertyWorkspace(),
                workspace)));
        AcraSuiteTab tab = reference.get();
        try {
            SwingUtilities.invokeAndWait(() -> verify(tab));
        } finally {
            SwingUtilities.invokeAndWait(tab::stop);
        }
        System.out.println("SPRINT10_BATCH_INDIRECT_UI PASS assertions=" + assertions);
    }

    private static void verify(AcraSuiteTab tab) {
        JTabbedPane top = find(tab.component(), JTabbedPane.class, null);
        check(indexOf(top, "Batch & Indirect") >= 0,
                "Sprint 10 Batch & Indirect top-level tab installed");
        assertions++;

        JTabbedPane tabs = find(tab.component(), JTabbedPane.class, "s10-batch-indirect-tabs");
        for (String title : Set.of(
                "Overview", "Policies", "Observations", "Assessments", "Candidates", "Coverage",
                "Report", "JSON Export")) {
            check(indexOf(tabs, title) >= 0, "Sprint 10 sub-tab installed: " + title);
            assertions++;
        }
        JTextArea overview = find(tab.component(), JTextArea.class, "s10-batch-indirect-overview");
        check(overview.getText().contains("Policy contexts: 4"),
                "overview renders explicit combined policy denominator");
        assertions++;
        check(overview.getText().contains("Batch contexts: 2"),
                "overview keeps batch coverage distinct");
        assertions++;
        check(overview.getText().contains("Indirect contexts: 2"),
                "overview keeps indirect coverage distinct");
        assertions++;
        check(overview.getText().contains("Observed contexts: 3"),
                "overview renders evidence-backed observation coverage");
        assertions++;
        check(overview.getText().contains("Assessed contexts: 3"),
                "overview renders reviewed coverage");
        assertions++;
        check(overview.getText().contains("Unobserved contexts: 1"),
                "overview preserves unobserved coverage");
        assertions++;
        check(overview.getText().contains("Finding candidates: 1"),
                "overview renders exactly one review candidate");
        assertions++;
        check(overview.getText().contains("Rejected controls: 2"),
                "overview renders two rejected secure controls");
        assertions++;
        check(overview.getText().contains("Candidate != confirmed vulnerability"),
                "overview preserves review-only boundary");
        assertions++;
        check(overview.getText().contains("Aggregate HTTP success != per-item authorization"),
                "overview rejects aggregate HTTP authorization inference");
        assertions++;
        check(overview.getText().contains("Raw indirect aliases are not rendered"),
                "overview states indirect-reference minimization boundary");
        assertions++;
        check(overview.getText().contains("Real Burp desktop runtime validation remains separate"),
                "headless verification is not mislabeled as real Burp runtime validation");
        assertions++;

        check(rows(tab, "s10-batch-indirect-policy-table") == 4,
                "policy table renders four explicit batch/indirect policies");
        assertions++;
        check(rows(tab, "s10-batch-indirect-observation-table") == 3,
                "observation table renders three explicit item/resolution observations");
        assertions++;
        check(rows(tab, "s10-batch-indirect-assessment-table") == 3,
                "assessment table renders three reviewed contexts");
        assertions++;
        check(rows(tab, "s10-batch-indirect-candidate-table") == 3,
                "candidate table includes candidate and rejected controls");
        assertions++;
        check(rows(tab, "s10-batch-indirect-coverage-table") == 4,
                "coverage table includes the unobserved policy context");
        assertions++;

        var snapshot = tab.batchIndirectWorkspace().snapshot();
        check(snapshot.policyCount() == 4,
                "suite tab exposes the same four-policy product workspace");
        assertions++;
        check(snapshot.observationCount() == 3,
                "workspace snapshot exposes explicit observation count");
        assertions++;
        check(snapshot.assessmentCount() == 3,
                "workspace snapshot exposes explicit assessment count");
        assertions++;
        check(snapshot.candidateCount() == 1,
                "workspace retains one review candidate without confirmation promotion");
        assertions++;
        check(snapshot.rejectedCount() == 2,
                "workspace retains two rejected secure controls");
        assertions++;
        check(snapshot.coverageSummary().unobservedContexts() == 1,
                "workspace retains one unobserved context");
        assertions++;

        JTextArea report = find(tab.component(), JTextArea.class, "s10-batch-indirect-report-view");
        check(report.getText().contains("ACRA Sprint 10 Batch & Indirect Authorization Report"),
                "Sprint 10 report view renders canonical Markdown");
        assertions++;
        check(report.getText().contains("Confirmed findings: 0"),
                "Sprint 10 report preserves zero confirmed findings");
        assertions++;
        check(report.getText().contains("Unobserved contexts: 1"),
                "Sprint 10 report preserves explicit coverage gap");
        assertions++;
        check(report.getText().contains("referenceFingerprint="),
                "Sprint 10 report renders indirect fingerprint instead of raw alias");
        assertions++;
        check(!report.getText().contains("share-a") && !report.getText().contains("share-b"),
                "Sprint 10 report view excludes raw indirect aliases");
        assertions++;

        JTextArea json = find(tab.component(), JTextArea.class, "s10-batch-indirect-json-export-view");
        check(json.getText().contains("\"reportVersion\":\"s10-batch-indirect-report-v1\""),
                "Sprint 10 JSON view renders canonical report version");
        assertions++;
        check(json.getText().contains("\"confirmedFindingCount\":0"),
                "Sprint 10 JSON view preserves review-only boundary");
        assertions++;
        check(json.getText().contains("\"unobservedContextCount\":1"),
                "Sprint 10 JSON view preserves coverage gap");
        assertions++;
        check(!json.getText().contains("\"policySource\""),
                "Sprint 10 JSON projection structurally excludes policySource");
        assertions++;
        check(!json.getText().contains("\"rationale\""),
                "Sprint 10 JSON projection structurally excludes candidate rationale");
        assertions++;
        check(!json.getText().contains("share-a") && !json.getText().contains("share-b"),
                "Sprint 10 JSON view excludes raw indirect aliases");
        assertions++;

        assertNoRawAlias(tab, "s10-batch-indirect-policy-table");
        assertNoRawAlias(tab, "s10-batch-indirect-observation-table");
        assertNoRawAlias(tab, "s10-batch-indirect-assessment-table");
        assertNoRawAlias(tab, "s10-batch-indirect-candidate-table");
        assertNoRawAlias(tab, "s10-batch-indirect-coverage-table");
    }

    private static S10BatchIndirectWorkspace fixtureWorkspace() {
        S10BatchIndirectWorkspace workspace = new S10BatchIndirectWorkspace();
        S10AuthorizationCoverageTracker coverage = new S10AuthorizationCoverageTracker();

        BatchItemPolicy batchA = batchPolicy("s10-ui-batch-a", "resource-a", AuthorizationDecision.ALLOW);
        BatchItemPolicy batchB = batchPolicy("s10-ui-batch-b", "resource-b", AuthorizationDecision.DENY);
        IndirectReferencePolicy indirectA = indirectPolicy(
                "s10-ui-indirect-a", "resource-a", AuthorizationDecision.ALLOW);
        IndirectReferencePolicy indirectB = indirectPolicy(
                "s10-ui-indirect-b", "resource-b", AuthorizationDecision.DENY);

        for (BatchItemPolicy policy : List.of(batchA, batchB)) {
            workspace.recordPolicy(policy);
            coverage.recordPolicy(policy);
        }
        for (IndirectReferencePolicy policy : List.of(indirectA, indirectB)) {
            workspace.recordPolicy(policy);
            coverage.recordPolicy(policy);
        }

        BatchItemObservation batchAObservation = batchObservation(
                "s10-ui-batch-obs-a", "item-a", "resource-a", AuthorizationDecision.ALLOW);
        BatchItemAuthorizationAssessment batchAAssessment = batchAssessment(
                "s10-ui-batch-assessment-a", "item-a", batchA,
                AuthorizationDecision.ALLOW, PolicyValidationState.ALLOWED);
        FindingCandidate batchAFinding = finding(
                "s10-ui-finding-batch-a", batchAAssessment.assessmentId(), "BATCH_AUTHORIZATION",
                batchA.endpoint(), "resource-a", batchAAssessment.expectedDecision(),
                batchAAssessment.observedDecision(), batchA.policyReference(), FindingCandidateState.REJECTED);
        workspace.recordObservation(batchAObservation);
        workspace.recordAssessment(batchAAssessment);
        workspace.recordCandidate(batchAFinding);
        coverage.recordObservation(batchA, batchAObservation);
        coverage.recordAssessment(batchA, batchAObservation, batchAAssessment, batchAFinding);

        BatchItemObservation batchBObservation = batchObservation(
                "s10-ui-batch-obs-b", "item-b", "resource-b", AuthorizationDecision.ALLOW);
        BatchItemAuthorizationAssessment batchBAssessment = batchAssessment(
                "s10-ui-batch-assessment-b", "item-b", batchB,
                AuthorizationDecision.ALLOW, PolicyValidationState.CONFLICTING);
        FindingCandidate batchBFinding = finding(
                "s10-ui-finding-batch-b", batchBAssessment.assessmentId(), "BATCH_AUTHORIZATION",
                batchB.endpoint(), "resource-b", batchBAssessment.expectedDecision(),
                batchBAssessment.observedDecision(), batchB.policyReference(), FindingCandidateState.CANDIDATE);
        workspace.recordObservation(batchBObservation);
        workspace.recordAssessment(batchBAssessment);
        workspace.recordCandidate(batchBFinding);
        coverage.recordObservation(batchB, batchBObservation);
        coverage.recordAssessment(batchB, batchBObservation, batchBAssessment, batchBFinding);

        IndirectReferenceResolution indirectBResolution = indirectResolution(
                "s10-ui-indirect-resolution-b", "share-b", "resource-b", AuthorizationDecision.DENY);
        IndirectReferenceAuthorizationAssessment indirectBAssessment = indirectAssessment(
                "s10-ui-indirect-assessment-b", indirectB,
                AuthorizationDecision.DENY, PolicyValidationState.DENIED);
        FindingCandidate indirectBFinding = finding(
                "s10-ui-finding-indirect-b", indirectBAssessment.assessmentId(),
                "INDIRECT_REFERENCE_AUTHORIZATION", indirectB.endpoint(), "resource-b",
                indirectBAssessment.expectedDecision(), indirectBAssessment.observedDecision(),
                indirectB.policyReference(), FindingCandidateState.REJECTED);
        workspace.recordObservation(indirectBResolution);
        workspace.recordAssessment(indirectBAssessment);
        workspace.recordCandidate(indirectBFinding);
        coverage.recordObservation(indirectB, indirectBResolution);
        coverage.recordAssessment(indirectB, indirectBResolution, indirectBAssessment, indirectBFinding);

        workspace.replaceCoverage(coverage);
        return workspace;
    }

    private static BatchItemPolicy batchPolicy(
            String reference, String resourceId, AuthorizationDecision expected) {
        return new BatchItemPolicy(
                reference, "GT-S10-BATCH-INDIRECT-AUTHORIZATION", BATCH_ENDPOINT,
                resourceId, "READ", "viewer", "tenant-a", expected, List.of("evidence-" + reference));
    }

    private static IndirectReferencePolicy indirectPolicy(
            String reference, String resourceId, AuthorizationDecision expected) {
        return new IndirectReferencePolicy(
                reference, "GT-S10-BATCH-INDIRECT-AUTHORIZATION", INDIRECT_ENDPOINT,
                resourceId, "READ", "viewer", "tenant-a", expected, List.of("evidence-" + reference));
    }

    private static BatchItemObservation batchObservation(
            String id, String itemKey, String resourceId, AuthorizationDecision observed) {
        return new BatchItemObservation(
                id, "source-" + id, "s10-ui-execution", "s10-ui-test", "s10-ui-batch",
                itemKey, BATCH_ENDPOINT, resourceId, "READ", observed, List.of("evidence-" + id));
    }

    private static BatchItemAuthorizationAssessment batchAssessment(
            String id, String itemKey, BatchItemPolicy policy,
            AuthorizationDecision observed, PolicyValidationState state) {
        return new BatchItemAuthorizationAssessment(
                id, "s10-ui-batch", itemKey, policy.endpoint(), policy.resourceId(), policy.action(),
                policy.policyReference(), policy.expectedDecision(), observed, state,
                state == PolicyValidationState.CONFLICTING ? "HIGH" : "MEDIUM",
                List.of("evidence-" + id), "S10 UI fixture", List.of());
    }

    private static IndirectReferenceResolution indirectResolution(
            String id, String rawAlias, String resourceId, AuthorizationDecision observed) {
        return new IndirectReferenceResolution(
                id, "source-" + id, "s10-ui-execution", "s10-ui-test", INDIRECT_ENDPOINT,
                TokenFingerprint.sha256(rawAlias), "ALIAS", resourceId, "READ", observed,
                IndirectReferenceSource.OBSERVED, List.of("evidence-" + id));
    }

    private static IndirectReferenceAuthorizationAssessment indirectAssessment(
            String id, IndirectReferencePolicy policy,
            AuthorizationDecision observed, PolicyValidationState state) {
        return new IndirectReferenceAuthorizationAssessment(
                id, policy.endpoint(), TokenFingerprint.sha256("share-b"), "ALIAS",
                policy.resolvedResourceId(), policy.action(), policy.policyReference(),
                policy.expectedDecision(), observed, state, "MEDIUM",
                List.of("evidence-" + id), "S10 UI fixture", List.of());
    }

    private static FindingCandidate finding(
            String id,
            String assessmentId,
            String dimension,
            String endpoint,
            String resourceId,
            AuthorizationDecision expected,
            AuthorizationDecision observed,
            String policyReference,
            FindingCandidateState state) {
        return new FindingCandidate(
                id, state, "acra-s10-ui", List.of("s10-ui-test"), List.of("s10-ui-execution"),
                List.of("s10-ui-observation"), List.of(assessmentId), List.of(dimension),
                endpoint, resourceId, "user-a", "tenant-a", expected, observed,
                List.of("evidence-" + assessmentId), List.of(), List.of(policyReference),
                state == FindingCandidateState.CANDIDATE ? "HIGH" : "MEDIUM",
                "S10 UI fixture; candidate is not an automatically confirmed vulnerability",
                FindingFingerprint.of(endpoint, resourceId, "user-a", "tenant-a", dimension, state.name()));
    }

    private static void assertNoRawAlias(AcraSuiteTab tab, String name) {
        JTable table = find(tab.component(), JTable.class, name);
        for (int row = 0; row < table.getRowCount(); row++) {
            for (int column = 0; column < table.getColumnCount(); column++) {
                String value = String.valueOf(table.getValueAt(row, column));
                check(!value.contains("share-a") && !value.contains("share-b"),
                        "Sprint 10 UI must not render raw indirect alias material");
                assertions++;
            }
        }
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
