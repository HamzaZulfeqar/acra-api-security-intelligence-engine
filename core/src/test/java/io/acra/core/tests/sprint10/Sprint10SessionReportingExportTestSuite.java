package io.acra.core.tests.sprint10;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.domain.identity.AuthenticationType;
import io.acra.core.product.session.S10SessionWorkspace;
import io.acra.core.recon.IdentityConfidenceState;
import io.acra.core.reporting.s10.S10SessionJsonReporter;
import io.acra.core.reporting.s10.S10SessionReportStatus;
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
import io.acra.core.tests.TestSupport;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class Sprint10SessionReportingExportTestSuite {
    private static final String RAW_TOKEN = "s10-report-raw-token-secret";
    private static final String TOKEN_FINGERPRINT = TokenFingerprint.sha256(RAW_TOKEN);
    private static final Instant AT = Instant.parse("2026-09-24T18:30:00Z");

    private Sprint10SessionReportingExportTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT10_SESSION_REPORTING_EXPORT PASS assertions=" + assertions);
    }

    public static int run() {
        int assertions = 0;
        S10SessionWorkspace workspace = fixtureWorkspace();

        var report = workspace.report(AT);
        TestSupport.assertEquals(S10SessionReportStatus.READY_FOR_REVIEW, report.status(),
                "non-empty session workspace produces review-ready report");
        assertions++;
        TestSupport.assertEquals(3, report.summary().coverageTargetCount(),
                "report preserves explicit coverage denominator");
        assertions++;
        TestSupport.assertEquals(2, report.summary().observedTargetCount(),
                "report preserves observed target count");
        assertions++;
        TestSupport.assertEquals(2, report.summary().correlatedTargetCount(),
                "report preserves correlated target count");
        assertions++;
        TestSupport.assertEquals(2, report.summary().assessedTargetCount(),
                "report preserves assessed target count");
        assertions++;
        TestSupport.assertEquals(1, report.summary().unobservedTargetCount(),
                "report preserves unobserved coverage gap");
        assertions++;
        TestSupport.assertEquals(1, report.summary().findingCandidateCount(),
                "report preserves one review candidate");
        assertions++;
        TestSupport.assertEquals(1, report.summary().rejectedControlCount(),
                "report preserves one rejected safe control");
        assertions++;
        TestSupport.assertEquals(0, report.summary().confirmedFindingCount(),
                "Sprint 10 report cannot auto-confirm findings");
        assertions++;
        TestSupport.assertEquals(2, report.observations().size(),
                "report contains two secret-minimized observation projections");
        assertions++;
        TestSupport.assertTrue(report.observations().stream()
                        .allMatch(value -> value.authContextRef().startsWith("authctx-")),
                "report replaces raw session identifiers with auth-context references");
        assertions++;

        var jsonA = workspace.exportJson(AT);
        var jsonB = workspace.exportJson(AT);
        TestSupport.assertEquals(jsonA.content(), jsonB.content(),
                "canonical JSON export is deterministic at the same report timestamp");
        assertions++;
        TestSupport.assertEquals(jsonA.sha256(), jsonB.sha256(),
                "canonical JSON digest is deterministic");
        assertions++;
        TestSupport.assertEquals(TokenFingerprint.sha256(jsonA.content()), jsonA.sha256(),
                "JSON artifact digest matches exported content");
        assertions++;
        TestSupport.assertContains(jsonA.content(), "\"confirmedFindingCount\":0",
                "JSON export preserves zero confirmed findings");
        assertions++;
        TestSupport.assertContains(jsonA.content(), "\"unobservedTargetCount\":1",
                "JSON export preserves coverage gaps");
        assertions++;
        TestSupport.assertContains(jsonA.content(), "\"findingCandidateCount\":1",
                "JSON export preserves review candidate count");
        assertions++;
        TestSupport.assertNotContains(jsonA.content(), RAW_TOKEN,
                "JSON export excludes raw bearer material");
        assertions++;
        TestSupport.assertNotContains(jsonA.content(), TOKEN_FINGERPRINT,
                "JSON export excludes token fingerprints");
        assertions++;
        TestSupport.assertNotContains(jsonA.content(), "\"tokenFingerprint\"",
                "JSON report schema has no tokenFingerprint field");
        assertions++;
        TestSupport.assertNotContains(jsonA.content(), "\"sessionId\"",
                "JSON report schema has no raw sessionId field");
        assertions++;
        for (String rawSession : List.of("session-u", "session-r", "session-k")) {
            TestSupport.assertNotContains(jsonA.content(), rawSession,
                    "JSON export excludes raw session identifier " + rawSession);
            assertions++;
        }

        var markdown = workspace.exportMarkdown(AT);
        TestSupport.assertContains(markdown.content(), "Confirmed findings: 0",
                "Markdown preserves zero confirmed findings");
        assertions++;
        TestSupport.assertContains(markdown.content(), "Unobserved targets: 1",
                "Markdown preserves coverage gaps");
        assertions++;
        TestSupport.assertContains(markdown.content(), "Finding Candidates - Review Only",
                "Markdown labels findings as review-only");
        assertions++;
        TestSupport.assertNotContains(markdown.content(), RAW_TOKEN,
                "Markdown excludes raw bearer material");
        assertions++;
        TestSupport.assertNotContains(markdown.content(), TOKEN_FINGERPRINT,
                "Markdown excludes token fingerprints");
        assertions++;
        for (String rawSession : List.of("session-u", "session-r", "session-k")) {
            TestSupport.assertNotContains(markdown.content(), rawSession,
                    "Markdown excludes raw session identifier " + rawSession);
            assertions++;
        }

        var adapter = new S10SessionJsonReporter();
        TestSupport.assertEquals("s10-auth-session-json-v1", adapter.id(),
                "Reporter adapter exposes stable Sprint 10 identifier");
        assertions++;
        TestSupport.assertEquals(jsonA.content(), adapter.render(Map.of("report", report)),
                "Reporter adapter renders the canonical JSON export");
        assertions++;

        var reportAgain = fixtureWorkspace().report(AT);
        TestSupport.assertEquals(report.reportId(), reportAgain.reportId(),
                "report identifier is deterministic across equivalent workspace construction");
        assertions++;
        TestSupport.assertEquals(
                report.reportId(),
                workspace.report(AT.plusSeconds(60)).reportId(),
                "report identity is independent of presentation timestamp");
        assertions++;

        S10SessionWorkspace empty = new S10SessionWorkspace();
        var emptyReport = empty.report(AT);
        TestSupport.assertEquals(S10SessionReportStatus.NO_SESSION_EVIDENCE, emptyReport.status(),
                "empty workspace report is explicitly no-evidence");
        assertions++;
        TestSupport.assertEquals(0, emptyReport.summary().confirmedFindingCount(),
                "empty report cannot confirm findings");
        assertions++;

        expectFailure(() -> adapter.render(Map.of("report", "not-a-session-report")),
                "Reporter adapter must reject wrong report model type");
        assertions++;

        return assertions;
    }

    private static S10SessionWorkspace fixtureWorkspace() {
        S10SessionWorkspace workspace = new S10SessionWorkspace();
        S10SessionCoverageTracker coverage = new S10SessionCoverageTracker();

        SessionCoverageTarget unobserved = SessionCoverageTarget.of(
                "report-unobserved", "session-u", SessionCoverageObjective.BASELINE_CONTEXT, "FR-033");
        SessionCoverageTarget safe = SessionCoverageTarget.of(
                "report-safe", "session-r", SessionCoverageObjective.ROTATION_CONTEXT_STABILITY, "SEC-003");
        SessionCoverageTarget candidate = SessionCoverageTarget.of(
                "report-candidate", "session-k", SessionCoverageObjective.ROTATION_CONTEXT_STABILITY, "SEC-003");

        coverage.recordTarget(unobserved);
        coverage.recordTarget(safe);
        coverage.recordTarget(candidate);

        AuthenticationSessionObservation safeObservation = observation(
                "report-obs-safe", "session-r", "user-r", "viewer", "tenant-a");
        SessionCorrelationResult safeCorrelation = correlation(
                "report-corr-safe", "session-r", "report-prev-safe", safeObservation.observationId(),
                SessionCorrelationState.TOKEN_ROTATED, List.of());
        SessionSecurityAssessment safeAssessment = assessment(
                "report-assess-safe", safeCorrelation, SessionSecurityAssessmentState.SAFE_ROTATION, true);
        FindingCandidate safeFinding = finding(
                "report-find-safe", safeAssessment, safe, "user-r", "tenant-a", FindingCandidateState.REJECTED);
        add(workspace, safeObservation, safeCorrelation, safeAssessment, safeFinding);
        coverage.recordCorrelation(safe, safeObservation, safeCorrelation);
        coverage.recordAssessment(safe, safeCorrelation, safeAssessment, safeFinding);

        AuthenticationSessionObservation driftObservation = observation(
                "report-obs-drift", "session-k", "user-k", "admin", "tenant-b");
        List<SessionContextDimension> drift = List.of(
                SessionContextDimension.ROLE, SessionContextDimension.TENANT, SessionContextDimension.SCOPE);
        SessionCorrelationResult driftCorrelation = correlation(
                "report-corr-drift", "session-k", "report-prev-drift", driftObservation.observationId(),
                SessionCorrelationState.CONTEXT_DRIFT, drift);
        SessionSecurityAssessment driftAssessment = assessment(
                "report-assess-drift", driftCorrelation, SessionSecurityAssessmentState.CANDIDATE, true);
        FindingCandidate driftFinding = finding(
                "report-find-drift", driftAssessment, candidate, "user-k", "tenant-a", FindingCandidateState.CANDIDATE);
        add(workspace, driftObservation, driftCorrelation, driftAssessment, driftFinding);
        coverage.recordCorrelation(candidate, driftObservation, driftCorrelation);
        coverage.recordAssessment(candidate, driftCorrelation, driftAssessment, driftFinding);

        workspace.replaceCoverage(coverage);
        return workspace;
    }

    private static void add(
            S10SessionWorkspace workspace,
            AuthenticationSessionObservation observation,
            SessionCorrelationResult correlation,
            SessionSecurityAssessment assessment,
            FindingCandidate finding) {
        workspace.recordObservation(observation);
        workspace.recordCorrelation(correlation);
        workspace.recordAssessment(assessment);
        workspace.recordCandidate(finding);
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
                AT,
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
                "Sprint 10 reporting fixture",
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
                "acra-s10-report",
                List.of("s10-report-test"),
                List.of("s10-report-execution"),
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
                "Sprint 10 reporting fixture; candidate is not an automatically confirmed vulnerability",
                FindingFingerprint.of(
                        "/api/v1/s10/session-context",
                        "auth-context:" + target.sessionId(),
                        principal,
                        tenant,
                        String.join("+", dimensions),
                        state.name()));
    }

    private static void expectFailure(Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }
}
