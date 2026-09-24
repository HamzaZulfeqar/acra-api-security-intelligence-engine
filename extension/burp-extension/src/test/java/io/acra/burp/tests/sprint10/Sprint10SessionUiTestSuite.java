package io.acra.burp.tests.sprint10;

import io.acra.burp.scope.ScopeController;
import io.acra.burp.traffic.TrafficIntelligencePipeline;
import io.acra.burp.ui.AcraSuiteTab;
import io.acra.core.active.product.ActiveEngineWorkspace;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.domain.identity.AuthenticationType;
import io.acra.core.product.authorization.S6AuthorizationWorkspace;
import io.acra.core.product.property.S9PropertyWorkspace;
import io.acra.core.product.routing.S8RoutingWorkspace;
import io.acra.core.product.session.S10SessionWorkspace;
import io.acra.core.product.workflow.S7WorkflowWorkspace;
import io.acra.core.recon.IdentityConfidenceState;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.session.AuthenticationSessionObservation;
import io.acra.core.session.S10SessionCoverageTracker;
import io.acra.core.session.SessionContextDimension;
import io.acra.core.session.SessionCorrelationResult;
import io.acra.core.session.SessionCorrelationState;
import io.acra.core.session.SessionCoverageObjective;
import io.acra.core.session.SessionCoverageTarget;
import io.acra.core.session.SessionSecurityAssessment;
import io.acra.core.session.SessionSecurityAssessmentState;
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

public final class Sprint10SessionUiTestSuite {
    private static final String RAW_TOKEN_SENTINEL = "s10-ui-raw-token-material";
    private static final String TOKEN_FINGERPRINT = TokenFingerprint.sha256(RAW_TOKEN_SENTINEL);
    private static int assertions;

    private Sprint10SessionUiTestSuite() { }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        S10SessionWorkspace session = fixtureWorkspace();
        AtomicReference<AcraSuiteTab> reference = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> reference.set(new AcraSuiteTab(
                new TrafficIntelligencePipeline(20),
                new ScopeController(),
                new ActiveEngineWorkspace(Clock.systemUTC(), null),
                new S6AuthorizationWorkspace(),
                new S7WorkflowWorkspace(),
                new S8RoutingWorkspace(),
                new S9PropertyWorkspace(),
                session)));
        AcraSuiteTab tab = reference.get();
        try {
            SwingUtilities.invokeAndWait(() -> verify(tab));
        } finally {
            SwingUtilities.invokeAndWait(tab::stop);
        }
        System.out.println("SPRINT10_SESSION_UI PASS assertions=" + assertions);
    }

    private static void verify(AcraSuiteTab tab) {
        JTabbedPane top = find(tab.component(), JTabbedPane.class, null);
        check(indexOf(top, "Authentication") >= 0, "Authentication top-level tab installed");
        assertions++;

        JTabbedPane sessionTabs = find(tab.component(), JTabbedPane.class, "s10-session-tabs");
        for (String title : Set.of(
                "Overview", "Sessions", "Correlations", "Assessments", "Candidates", "Coverage",
                "Report", "JSON Export")) {
            check(indexOf(sessionTabs, title) >= 0, "session sub-tab installed: " + title);
            assertions++;
        }

        JTextArea overview = find(tab.component(), JTextArea.class, "s10-session-overview");
        check(overview.getText().contains("Coverage targets: 3"),
                "session overview renders explicit coverage denominator");
        assertions++;
        check(overview.getText().contains("Observed targets: 2"),
                "session overview renders observed coverage");
        assertions++;
        check(overview.getText().contains("Correlated targets: 2"),
                "session overview renders correlated coverage");
        assertions++;
        check(overview.getText().contains("Assessed targets: 2"),
                "session overview renders assessed coverage");
        assertions++;
        check(overview.getText().contains("Unobserved targets: 1"),
                "session overview preserves unobserved coverage");
        assertions++;
        check(overview.getText().contains("Finding candidates: 1"),
                "session overview renders one review candidate");
        assertions++;
        check(overview.getText().contains("Rejected controls: 1"),
                "session overview renders one rejected safe rotation");
        assertions++;
        check(overview.getText().contains("Token fingerprint != verified principal"),
                "session overview preserves identity-proof boundary");
        assertions++;
        check(overview.getText().contains("Candidate != confirmed vulnerability"),
                "session overview preserves review-only boundary");
        assertions++;
        check(overview.getText().contains("Unobserved != secure"),
                "session overview does not label missing coverage as secure");
        assertions++;
        check(overview.getText().contains("Raw authentication material is not rendered"),
                "session overview states secret-minimization boundary");
        assertions++;
        check(overview.getText().contains("Real Burp desktop runtime validation remains separate"),
                "headless UI verification does not imply real Burp runtime validation");
        assertions++;

        check(rows(tab, "s10-session-observation-table") == 2,
                "session table renders two observations");
        assertions++;
        check(rows(tab, "s10-session-correlation-table") == 2,
                "correlation table renders two correlations");
        assertions++;
        check(rows(tab, "s10-session-assessment-table") == 2,
                "assessment table renders two assessments");
        assertions++;
        check(rows(tab, "s10-session-candidate-table") == 2,
                "candidate table renders candidate and rejected control");
        assertions++;
        check(rows(tab, "s10-session-coverage-table") == 3,
                "coverage table includes the unobserved target");
        assertions++;

        JTextArea report = find(tab.component(), JTextArea.class, "s10-session-report-view");
        check(report.getText().contains("Confirmed findings: 0"),
                "session report view preserves zero confirmed findings");
        assertions++;
        check(report.getText().contains("Unobserved targets: 1"),
                "session report view preserves coverage gaps");
        assertions++;
        check(!report.getText().contains("session-r") && !report.getText().contains("session-k"),
                "session report view excludes raw session identifiers");
        assertions++;

        JTextArea jsonExport = find(tab.component(), JTextArea.class, "s10-session-json-export-view");
        check(jsonExport.getText().contains("\"confirmedFindingCount\":0"),
                "session JSON export view preserves zero confirmed findings");
        assertions++;
        check(jsonExport.getText().contains("\"unobservedTargetCount\":1"),
                "session JSON export view preserves unobserved coverage");
        assertions++;
        check(!jsonExport.getText().contains("\"tokenFingerprint\"")
                        && !jsonExport.getText().contains("\"sessionId\""),
                "session JSON export schema excludes token fingerprint and raw session ID fields");
        assertions++;

        JTable candidates = find(tab.component(), JTable.class, "s10-session-candidate-table");
        boolean sawAuthContext = false;
        for (int row = 0; row < candidates.getRowCount(); row++) {
            String context = String.valueOf(candidates.getValueAt(row, 3));
            if (context.startsWith("auth-context:")) sawAuthContext = true;
            check(!context.contains("<redacted>"),
                    "redaction-safe auth-context resource IDs remain readable");
            assertions++;
        }
        check(sawAuthContext, "candidate table renders auth-context resource identity");
        assertions++;

        assertNoSecretProjection(tab.component(), RAW_TOKEN_SENTINEL);
        assertNoSecretProjection(tab.component(), TOKEN_FINGERPRINT);

        var snapshot = tab.sessionWorkspace().snapshot();
        check(snapshot.observations().size() == 2,
                "session workspace exposes two observations");
        assertions++;
        check(snapshot.correlations().size() == 2,
                "session workspace exposes two correlations");
        assertions++;
        check(snapshot.assessments().size() == 2,
                "session workspace exposes two assessments");
        assertions++;
        check(snapshot.candidateCount() == 1,
                "session workspace exposes one review candidate");
        assertions++;
        check(snapshot.rejectedCount() == 1,
                "session workspace exposes one rejected safe control");
        assertions++;
        check(snapshot.coverageSummary().unobservedTargets() == 1,
                "session workspace preserves one unobserved target");
        assertions++;

        tab.sessionWorkspace().clearRuntimeState();
        var cleared = tab.sessionWorkspace().snapshot();
        check(cleared.observations().isEmpty() && cleared.correlations().isEmpty()
                        && cleared.assessments().isEmpty() && cleared.candidates().isEmpty(),
                "clearRuntimeState removes session runtime results");
        assertions++;
        check(cleared.coverageSummary().totalTargets() == 3
                        && cleared.coverageSummary().unobservedTargets() == 3,
                "clearRuntimeState preserves explicit coverage targets as unobserved");
        assertions++;
    }

    private static S10SessionWorkspace fixtureWorkspace() {
        S10SessionWorkspace workspace = new S10SessionWorkspace();
        S10SessionCoverageTracker coverage = new S10SessionCoverageTracker();

        SessionCoverageTarget unobserved = SessionCoverageTarget.of(
                "s10-ui-unobserved", "session-u", SessionCoverageObjective.BASELINE_CONTEXT, "FR-033");
        SessionCoverageTarget safe = SessionCoverageTarget.of(
                "s10-ui-safe", "session-r", SessionCoverageObjective.ROTATION_CONTEXT_STABILITY, "SEC-003");
        SessionCoverageTarget candidate = SessionCoverageTarget.of(
                "s10-ui-candidate", "session-k", SessionCoverageObjective.ROTATION_CONTEXT_STABILITY, "SEC-003");

        coverage.recordTarget(unobserved);
        coverage.recordTarget(safe);
        coverage.recordTarget(candidate);

        AuthenticationSessionObservation safeObservation = observation(
                "s10-ui-obs-safe", "session-r", "user-r", "viewer", "tenant-a");
        SessionCorrelationResult safeCorrelation = correlation(
                "s10-ui-corr-safe", "session-r", "s10-ui-prev-safe", safeObservation.observationId(),
                SessionCorrelationState.TOKEN_ROTATED, List.of());
        SessionSecurityAssessment safeAssessment = assessment(
                "s10-ui-assess-safe", safeCorrelation, SessionSecurityAssessmentState.SAFE_ROTATION, true);
        FindingCandidate safeFinding = finding(
                "s10-ui-finding-safe", safeAssessment, safe, "user-r", "tenant-a", FindingCandidateState.REJECTED);
        workspace.recordObservation(safeObservation);
        workspace.recordCorrelation(safeCorrelation);
        workspace.recordAssessment(safeAssessment);
        workspace.recordCandidate(safeFinding);
        coverage.recordCorrelation(safe, safeObservation, safeCorrelation);
        coverage.recordAssessment(safe, safeCorrelation, safeAssessment, safeFinding);

        AuthenticationSessionObservation driftObservation = observation(
                "s10-ui-obs-drift", "session-k", "user-k", "admin", "tenant-b");
        SessionCorrelationResult driftCorrelation = correlation(
                "s10-ui-corr-drift", "session-k", "s10-ui-prev-drift", driftObservation.observationId(),
                SessionCorrelationState.CONTEXT_DRIFT,
                List.of(SessionContextDimension.ROLE, SessionContextDimension.TENANT, SessionContextDimension.SCOPE));
        SessionSecurityAssessment driftAssessment = assessment(
                "s10-ui-assess-drift", driftCorrelation, SessionSecurityAssessmentState.CANDIDATE, true);
        FindingCandidate driftFinding = finding(
                "s10-ui-finding-drift", driftAssessment, candidate, "user-k", "tenant-a",
                FindingCandidateState.CANDIDATE);
        workspace.recordObservation(driftObservation);
        workspace.recordCorrelation(driftCorrelation);
        workspace.recordAssessment(driftAssessment);
        workspace.recordCandidate(driftFinding);
        coverage.recordCorrelation(candidate, driftObservation, driftCorrelation);
        coverage.recordAssessment(candidate, driftCorrelation, driftAssessment, driftFinding);

        workspace.replaceCoverage(coverage);
        return workspace;
    }

    private static AuthenticationSessionObservation observation(
            String id, String sessionId, String principal, String role, String tenant) {
        return new AuthenticationSessionObservation(
                id,
                sessionId,
                TOKEN_FINGERPRINT,
                principal,
                role,
                tenant,
                List.of("profile.read"),
                AuthenticationType.OAUTH,
                IdentityConfidenceState.USER_CONFIRMED,
                Instant.parse("2026-09-24T18:20:00Z"),
                List.of("evidence-" + id));
    }

    private static SessionCorrelationResult correlation(
            String id,
            String sessionId,
            String previousObservation,
            String currentObservation,
            SessionCorrelationState state,
            List<SessionContextDimension> drift) {
        return new SessionCorrelationResult(
                id,
                sessionId,
                previousObservation,
                currentObservation,
                state,
                true,
                true,
                true,
                drift,
                List.of("evidence-" + currentObservation),
                List.of(state.name()));
    }

    private static SessionSecurityAssessment assessment(
            String id,
            SessionCorrelationResult correlation,
            SessionSecurityAssessmentState state,
            boolean verified) {
        return new SessionSecurityAssessment(
                id,
                correlation.sessionId(),
                correlation.previousObservationId(),
                correlation.currentObservationId(),
                state,
                correlation.tokenRotated(),
                verified,
                correlation.driftDimensions(),
                correlation.evidenceIds(),
                "S10 headless UI fixture",
                List.of());
    }

    private static FindingCandidate finding(
            String id,
            SessionSecurityAssessment assessment,
            SessionCoverageTarget target,
            String principal,
            String tenant,
            FindingCandidateState state) {
        List<String> dimensions = new java.util.ArrayList<>();
        dimensions.add("AUTHENTICATION_SESSION");
        dimensions.add("TOKEN_ROTATION");
        for (SessionContextDimension dimension : assessment.driftDimensions()) {
            dimensions.add("SESSION_CONTEXT_" + dimension.name());
        }
        return new FindingCandidate(
                id,
                state,
                "acra-s10-ui",
                List.of("s10-ui-test"),
                List.of("s10-ui-execution"),
                List.of(assessment.currentObservationId()),
                List.of(assessment.assessmentId()),
                dimensions,
                "/api/v1/s10/session-context",
                "auth-context:" + target.sessionId(),
                principal,
                tenant,
                AuthorizationDecision.UNKNOWN,
                AuthorizationDecision.UNKNOWN,
                assessment.evidenceIds(),
                List.of(),
                List.of(target.ruleReference()),
                state == FindingCandidateState.CANDIDATE ? "HIGH" : "MEDIUM",
                "S10 UI fixture; candidate is not an automatically confirmed vulnerability",
                FindingFingerprint.of(
                        "/api/v1/s10/session-context",
                        "auth-context:" + target.sessionId(),
                        principal,
                        tenant,
                        String.join("+", dimensions),
                        state.name()));
    }

    private static int rows(AcraSuiteTab tab, String name) {
        JTable table = find(tab.component(), JTable.class, name);
        if (table == null) throw new AssertionError("table missing: " + name);
        return table.getRowCount();
    }

    private static void assertNoSecretProjection(Component root, String forbidden) {
        if (root instanceof JTextArea area) {
            check(!area.getText().contains(forbidden), "text view must not expose secret/fingerprint projection");
            assertions++;
        }
        if (root instanceof JTable table) {
            for (int row = 0; row < table.getRowCount(); row++) {
                for (int column = 0; column < table.getColumnCount(); column++) {
                    check(!String.valueOf(table.getValueAt(row, column)).contains(forbidden),
                            "table must not expose secret/fingerprint projection");
                    assertions++;
                }
            }
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                assertNoSecretProjection(child, forbidden);
            }
        }
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
