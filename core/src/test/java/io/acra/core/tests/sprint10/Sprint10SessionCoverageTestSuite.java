package io.acra.core.tests.sprint10;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.domain.identity.AuthenticationType;
import io.acra.core.recon.IdentityConfidenceState;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.session.AuthenticationSessionObservation;
import io.acra.core.session.S10SessionCoverageTracker;
import io.acra.core.session.SessionContextDimension;
import io.acra.core.session.SessionCorrelationResult;
import io.acra.core.session.SessionCorrelationState;
import io.acra.core.session.SessionCoverageDisposition;
import io.acra.core.session.SessionCoverageObjective;
import io.acra.core.session.SessionCoverageTarget;
import io.acra.core.session.SessionSecurityAssessment;
import io.acra.core.session.SessionSecurityAssessmentState;
import io.acra.core.tests.TestSupport;
import java.time.Instant;
import java.util.List;

public final class Sprint10SessionCoverageTestSuite {
    private Sprint10SessionCoverageTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT10_SESSION_COVERAGE PASS assertions=" + assertions);
    }

    public static int run() {
        int assertions = 0;
        S10SessionCoverageTracker tracker = new S10SessionCoverageTracker();

        SessionCoverageTarget unobserved = SessionCoverageTarget.of(
                "target-unobserved", "session-u", SessionCoverageObjective.BASELINE_CONTEXT, "FR-033");
        SessionCoverageTarget observed = SessionCoverageTarget.of(
                "target-observed", "session-o", SessionCoverageObjective.SESSION_CONTINUITY, "SEC-010");
        SessionCoverageTarget correlated = SessionCoverageTarget.of(
                "target-correlated", "session-c", SessionCoverageObjective.SESSION_CONTINUITY, "SEC-010");
        SessionCoverageTarget rejected = SessionCoverageTarget.of(
                "target-rejected", "session-r", SessionCoverageObjective.ROTATION_CONTEXT_STABILITY, "SEC-003");
        SessionCoverageTarget candidate = SessionCoverageTarget.of(
                "target-candidate", "session-k", SessionCoverageObjective.ROTATION_CONTEXT_STABILITY, "SEC-003");
        SessionCoverageTarget inconclusive = SessionCoverageTarget.of(
                "target-inconclusive", "session-i", SessionCoverageObjective.SESSION_CONTINUITY, "SEC-010");

        tracker.recordTarget(unobserved);
        tracker.recordTarget(observed);
        tracker.recordTarget(correlated);
        tracker.recordTarget(rejected);
        tracker.recordTarget(candidate);
        tracker.recordTarget(inconclusive);

        tracker.recordTarget(unobserved);
        TestSupport.assertEquals(6, tracker.size(),
                "duplicate registration of the same explicit target must not inflate denominator");
        assertions++;

        AuthenticationSessionObservation observedValue = observation("obs-o", "session-o", "user-o", "viewer", "tenant-a");
        tracker.recordObservation(observed, observedValue);
        TestSupport.assertEquals(SessionCoverageDisposition.OBSERVED_UNCORRELATED,
                entry(tracker, observed).disposition(),
                "observation without correlation remains visibly uncorrelated");
        assertions++;

        AuthenticationSessionObservation correlatedValue = observation("obs-c", "session-c", "user-c", "viewer", "tenant-a");
        SessionCorrelationResult correlatedResult = correlation(
                "corr-c", "session-c", "obs-c-prev", "obs-c",
                SessionCorrelationState.STABLE, false, List.of());
        tracker.recordCorrelation(correlated, correlatedValue, correlatedResult);
        TestSupport.assertEquals(SessionCoverageDisposition.CORRELATED_UNASSESSED,
                entry(tracker, correlated).disposition(),
                "correlated context without assessment remains visibly unassessed");
        assertions++;

        AuthenticationSessionObservation rejectedValue = observation("obs-r", "session-r", "user-r", "viewer", "tenant-a");
        SessionCorrelationResult safeRotation = correlation(
                "corr-r", "session-r", "obs-r-prev", "obs-r",
                SessionCorrelationState.TOKEN_ROTATED, true, List.of());
        tracker.recordCorrelation(rejected, rejectedValue, safeRotation);
        SessionSecurityAssessment rejectedAssessment = assessment(
                "assess-r", "session-r", "obs-r-prev", "obs-r",
                SessionSecurityAssessmentState.SAFE_ROTATION, true, List.of());
        tracker.recordAssessment(rejected, safeRotation, rejectedAssessment,
                finding("fc-r", FindingCandidateState.REJECTED, rejectedAssessment, rejected, "user-r", "tenant-a"));
        TestSupport.assertEquals(SessionCoverageDisposition.REJECTED,
                entry(tracker, rejected).disposition(),
                "safe rotation assessed and rejected is counted as rejected coverage");
        assertions++;

        AuthenticationSessionObservation candidateValue = observation("obs-k", "session-k", "user-k", "viewer", "tenant-a");
        List<SessionContextDimension> drift = List.of(
                SessionContextDimension.ROLE, SessionContextDimension.TENANT, SessionContextDimension.SCOPE);
        SessionCorrelationResult driftCorrelation = correlation(
                "corr-k", "session-k", "obs-k-prev", "obs-k",
                SessionCorrelationState.CONTEXT_DRIFT, true, drift);
        tracker.recordCorrelation(candidate, candidateValue, driftCorrelation);
        SessionSecurityAssessment candidateAssessment = assessment(
                "assess-k", "session-k", "obs-k-prev", "obs-k",
                SessionSecurityAssessmentState.CANDIDATE, true, drift);
        tracker.recordAssessment(candidate, driftCorrelation, candidateAssessment,
                finding("fc-k", FindingCandidateState.CANDIDATE, candidateAssessment, candidate, "user-k", "tenant-a"));
        TestSupport.assertEquals(SessionCoverageDisposition.CANDIDATE,
                entry(tracker, candidate).disposition(),
                "review-only finding candidate is counted as candidate coverage");
        assertions++;

        AuthenticationSessionObservation inconclusiveValue = observation(
                "obs-i", "session-i", "user-i", "viewer", "tenant-a");
        SessionCorrelationResult inconclusiveCorrelation = correlation(
                "corr-i", "session-i", "obs-i-prev", "obs-i",
                SessionCorrelationState.UNVERIFIED_IDENTITY, false, List.of(SessionContextDimension.PRINCIPAL));
        tracker.recordCorrelation(inconclusive, inconclusiveValue, inconclusiveCorrelation);
        SessionSecurityAssessment inconclusiveAssessment = assessment(
                "assess-i", "session-i", "obs-i-prev", "obs-i",
                SessionSecurityAssessmentState.INCONCLUSIVE, false, List.of(SessionContextDimension.PRINCIPAL));
        tracker.recordAssessment(inconclusive, inconclusiveCorrelation, inconclusiveAssessment,
                finding("fc-i", FindingCandidateState.INCONCLUSIVE, inconclusiveAssessment,
                        inconclusive, "user-i", "tenant-a"));
        TestSupport.assertEquals(SessionCoverageDisposition.INCONCLUSIVE,
                entry(tracker, inconclusive).disposition(),
                "unverified assessed context remains inconclusive coverage");
        assertions++;

        var summary = tracker.summary();
        TestSupport.assertEquals(6, summary.totalTargets(), "explicit target universe defines denominator");
        assertions++;
        TestSupport.assertEquals(1, summary.baselineTargets(), "baseline objective count remains explicit");
        assertions++;
        TestSupport.assertEquals(3, summary.continuityTargets(), "continuity objective count remains explicit");
        assertions++;
        TestSupport.assertEquals(2, summary.rotationTargets(), "rotation objective count remains explicit");
        assertions++;
        TestSupport.assertEquals(5, summary.observedTargets(), "observed target count is correct");
        assertions++;
        TestSupport.assertEquals(4, summary.correlatedTargets(), "correlated target count is correct");
        assertions++;
        TestSupport.assertEquals(3, summary.assessedTargets(), "assessed target count is correct");
        assertions++;
        TestSupport.assertEquals(1, summary.candidateTargets(), "candidate count is correct");
        assertions++;
        TestSupport.assertEquals(1, summary.rejectedTargets(), "rejected count is correct");
        assertions++;
        TestSupport.assertEquals(1, summary.inconclusiveTargets(), "inconclusive count is correct");
        assertions++;
        TestSupport.assertEquals(1, summary.unobservedTargets(), "unobserved context stays visible");
        assertions++;
        TestSupport.assertEquals(1, summary.observedUncorrelatedTargets(),
                "observed-but-uncorrelated context stays visible");
        assertions++;
        TestSupport.assertEquals(1, summary.correlatedUnassessedTargets(),
                "correlated-but-unassessed context stays visible");
        assertions++;
        TestSupport.assertEquals(List.of(unobserved.coverageId()), tracker.unobservedCoverageIds(),
                "unobserved coverage IDs are deterministic and discoverable");
        assertions++;
        TestSupport.assertEquals(List.of(observed.coverageId()), tracker.observedUncorrelatedCoverageIds(),
                "observed-uncorrelated coverage IDs are deterministic and discoverable");
        assertions++;
        TestSupport.assertEquals(List.of(correlated.coverageId()), tracker.correlatedUnassessedCoverageIds(),
                "correlated-unassessed coverage IDs are deterministic and discoverable");
        assertions++;
        TestSupport.assertEquals(List.of(inconclusive.coverageId()), tracker.inconclusiveCoverageIds(),
                "inconclusive coverage IDs are deterministic and discoverable");
        assertions++;

        expectFailure(() -> tracker.recordObservation(
                observed, observation("obs-wrong", "other-session", "user-o", "viewer", "tenant-a")),
                "session mismatch must not be accepted into coverage");
        assertions++;

        SessionCoverageTarget rotationFailure = SessionCoverageTarget.of(
                "target-rotation-failure", "session-rf",
                SessionCoverageObjective.ROTATION_CONTEXT_STABILITY, "SEC-003");
        AuthenticationSessionObservation rotationFailureObservation =
                observation("obs-rf", "session-rf", "user-rf", "viewer", "tenant-a");
        tracker.recordObservation(rotationFailure, rotationFailureObservation);
        expectFailure(() -> tracker.recordCorrelation(
                rotationFailure,
                rotationFailureObservation,
                correlation("corr-rf", "session-rf", "prev-rf", "obs-rf",
                        SessionCorrelationState.STABLE, false, List.of())),
                "rotation coverage target must reject non-rotation correlation");
        assertions++;

        List<String> coverageIds = tracker.entries().stream()
                .map(value -> value.target().coverageId())
                .toList();
        List<String> sortedIds = coverageIds.stream().sorted().toList();
        TestSupport.assertEquals(sortedIds, coverageIds,
                "coverage entries have deterministic coverage-id ordering");
        assertions++;

        return assertions;
    }

    private static io.acra.core.session.SessionCoverageEntry entry(
            S10SessionCoverageTracker tracker, SessionCoverageTarget target) {
        return tracker.entries().stream()
                .filter(value -> value.target().coverageId().equals(target.coverageId()))
                .findFirst()
                .orElseThrow();
    }

    private static AuthenticationSessionObservation observation(
            String id, String sessionId, String principal, String role, String tenant) {
        return new AuthenticationSessionObservation(
                id,
                sessionId,
                TokenFingerprint.sha256("token-" + id),
                principal,
                role,
                tenant,
                List.of("profile.read"),
                AuthenticationType.OAUTH,
                IdentityConfidenceState.USER_CONFIRMED,
                Instant.parse("2026-09-24T18:00:00Z"),
                List.of("evidence-" + id));
    }

    private static SessionCorrelationResult correlation(
            String id,
            String sessionId,
            String previousObservationId,
            String currentObservationId,
            SessionCorrelationState state,
            boolean rotated,
            List<SessionContextDimension> drift) {
        return new SessionCorrelationResult(
                id,
                sessionId,
                previousObservationId,
                currentObservationId,
                state,
                true,
                true,
                rotated,
                drift,
                List.of("evidence-" + currentObservationId),
                List.of(state.name()));
    }

    private static SessionSecurityAssessment assessment(
            String id,
            String sessionId,
            String previousObservationId,
            String currentObservationId,
            SessionSecurityAssessmentState state,
            boolean rotated,
            List<SessionContextDimension> drift) {
        return new SessionSecurityAssessment(
                id,
                sessionId,
                previousObservationId,
                currentObservationId,
                state,
                rotated,
                state != SessionSecurityAssessmentState.INCONCLUSIVE,
                drift,
                List.of("evidence-" + currentObservationId),
                "coverage test assessment",
                List.of());
    }

    private static FindingCandidate finding(
            String id,
            FindingCandidateState state,
            SessionSecurityAssessment assessment,
            SessionCoverageTarget target,
            String principal,
            String tenant) {
        return new FindingCandidate(
                id,
                state,
                "project-coverage",
                List.of("test-" + id),
                List.of("execution-" + id),
                List.of(assessment.currentObservationId()),
                List.of(assessment.assessmentId()),
                List.of("AUTHENTICATION_SESSION"),
                "/api/v1/s10/session-context",
                "auth-context:" + target.sessionId(),
                principal,
                tenant,
                AuthorizationDecision.UNKNOWN,
                AuthorizationDecision.UNKNOWN,
                List.of("evidence-" + id),
                List.of(),
                List.of(target.ruleReference()),
                state == FindingCandidateState.CANDIDATE ? "HIGH" : "MEDIUM",
                "coverage test projection; candidate is not an automatically confirmed vulnerability",
                FindingFingerprint.of(
                        "/api/v1/s10/session-context",
                        "auth-context:" + target.sessionId(),
                        principal,
                        tenant,
                        "AUTHENTICATION_SESSION",
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
