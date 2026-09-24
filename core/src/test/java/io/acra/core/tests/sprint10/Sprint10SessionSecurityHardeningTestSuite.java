package io.acra.core.tests.sprint10;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.domain.identity.AuthenticationType;
import io.acra.core.product.session.S10SessionWorkspace;
import io.acra.core.recon.IdentityConfidenceState;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.session.AuthenticationSessionObservation;
import io.acra.core.session.S10SessionCoverageTracker;
import io.acra.core.session.SessionCoverageObjective;
import io.acra.core.session.SessionCoverageTarget;
import io.acra.core.tests.TestSupport;
import java.time.Instant;
import java.util.List;

public final class Sprint10SessionSecurityHardeningTestSuite {
    private static final Instant AT = Instant.parse("2026-09-24T18:40:00Z");

    private Sprint10SessionSecurityHardeningTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT10_SESSION_SECURITY_HARDENING PASS assertions=" + assertions);
    }

    public static int run() {
        int assertions = 0;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> observation("obs-raw-token", "session-a", "raw-token-not-a-fingerprint",
                        IdentityConfidenceState.USER_CONFIRMED),
                "raw token material cannot be used as token fingerprint");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> observation("obs-secret-session", "Authorization: Bearer secret-value",
                        TokenFingerprint.sha256("safe"), IdentityConfidenceState.USER_CONFIRMED),
                "secret-bearing session metadata is rejected");
        assertions++;

        AuthenticationSessionObservation inferred = observation(
                "obs-inferred", "session-inferred", TokenFingerprint.sha256("inferred-token"),
                IdentityConfidenceState.INFERRED);
        TestSupport.assertTrue(!inferred.identityVerified(),
                "inferred identity remains unverified");
        assertions++;

        AuthenticationSessionObservation confirmed = observation(
                "obs-confirmed", "session-confirmed", TokenFingerprint.sha256("confirmed-token"),
                IdentityConfidenceState.USER_CONFIRMED);
        TestSupport.assertTrue(confirmed.identityVerified(),
                "user-confirmed identity is verified");
        assertions++;

        S10SessionCoverageTracker coverage = new S10SessionCoverageTracker();
        SessionCoverageTarget target = SessionCoverageTarget.of(
                "hardening-target", "session-confirmed",
                SessionCoverageObjective.BASELINE_CONTEXT, "FR-033");
        coverage.recordTarget(target);
        AuthenticationSessionObservation wrongSession = observation(
                "obs-wrong-session", "session-other", TokenFingerprint.sha256("wrong-session-token"),
                IdentityConfidenceState.USER_CONFIRMED);
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> io.acra.core.session.SessionCoverageEntry.from(target).withObservation(wrongSession),
                "coverage entry rejects observation from a different session");
        assertions++;

        S10SessionWorkspace workspace = new S10SessionWorkspace();
        FindingCandidate nonSession = new FindingCandidate(
                "fc-non-session",
                FindingCandidateState.CANDIDATE,
                "project",
                List.of("test"),
                List.of("execution"),
                List.of("observation"),
                List.of("assessment"),
                List.of("PROPERTY_AUTHORIZATION"),
                "/api/v1/s10/session-context",
                "resource",
                "principal",
                "tenant",
                AuthorizationDecision.UNKNOWN,
                AuthorizationDecision.UNKNOWN,
                List.of("evidence"),
                List.of(),
                List.of("SEC-003"),
                "MEDIUM",
                "not a session finding",
                FindingFingerprint.of(
                        "/api/v1/s10/session-context",
                        "resource",
                        "principal",
                        "tenant",
                        "PROPERTY_AUTHORIZATION",
                        "CANDIDATE"));
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> workspace.recordCandidate(nonSession),
                "session workspace rejects non-session finding projection");
        assertions++;

        workspace.recordObservation(confirmed);
        workspace.recordCoverage(io.acra.core.session.SessionCoverageEntry.from(target));
        var json = workspace.exportJson(AT);
        TestSupport.assertNotContains(json.content(), "session-confirmed",
                "JSON export excludes raw session identifier");
        assertions++;
        TestSupport.assertNotContains(json.content(), confirmed.tokenFingerprint(),
                "JSON export excludes token fingerprint");
        assertions++;
        TestSupport.assertNotContains(json.content(), "\"sessionId\"",
                "JSON schema exposes no raw sessionId field");
        assertions++;
        TestSupport.assertNotContains(json.content(), "\"tokenFingerprint\"",
                "JSON schema exposes no tokenFingerprint field");
        assertions++;
        TestSupport.assertEquals(0, workspace.report(AT).summary().confirmedFindingCount(),
                "session report cannot auto-confirm findings");
        assertions++;

        return assertions;
    }

    private static AuthenticationSessionObservation observation(
            String id,
            String sessionId,
            String fingerprint,
            IdentityConfidenceState state) {
        return new AuthenticationSessionObservation(
                id,
                sessionId,
                fingerprint,
                "user-a",
                "viewer",
                "tenant-a",
                List.of("profile.read"),
                AuthenticationType.OAUTH,
                state,
                AT,
                List.of("evidence-" + id));
    }
}
