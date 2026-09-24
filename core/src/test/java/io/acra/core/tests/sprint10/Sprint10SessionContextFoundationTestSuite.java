package io.acra.core.tests.sprint10;

import io.acra.core.domain.identity.AuthenticationType;
import io.acra.core.recon.IdentityConfidenceState;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.session.AuthenticationSessionObservation;
import io.acra.core.session.S10SessionContextAnalyzer;
import io.acra.core.session.SessionContextDimension;
import io.acra.core.session.SessionCorrelationState;
import io.acra.core.tests.TestSupport;
import java.time.Instant;
import java.util.List;

public final class Sprint10SessionContextFoundationTestSuite {
    private Sprint10SessionContextFoundationTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT10_SESSION_CONTEXT_FOUNDATION PASS assertions=" + assertions);
    }

    public static int run() {
        S10SessionContextAnalyzer analyzer = new S10SessionContextAnalyzer();
        int assertions = 0;

        var first = verified(
                "obs-1", "session-a", fp("token-1"), "user-a", "viewer", "tenant-a",
                List.of("profile.read"), "e-1");
        var baseline = analyzer.analyze(null, first);
        TestSupport.assertEquals(SessionCorrelationState.BASELINE, baseline.state(),
                "first session observation creates a baseline only");
        assertions++;
        TestSupport.assertTrue(baseline.currentIdentityVerified(),
                "explicitly confirmed identity remains verified");
        assertions++;

        var same = verified(
                "obs-2", "session-a", fp("token-1"), "user-a", "viewer", "tenant-a",
                List.of("profile.read"), "e-2");
        var stable = analyzer.analyze(first, same);
        TestSupport.assertEquals(SessionCorrelationState.STABLE, stable.state(),
                "same verified token and context remains stable");
        assertions++;
        TestSupport.assertTrue(!stable.tokenRotated(),
                "stable observation does not invent token rotation");
        assertions++;
        TestSupport.assertEquals(List.of(), stable.driftDimensions(),
                "stable observation has no context drift");
        assertions++;

        var rotatedObservation = verified(
                "obs-3", "session-a", fp("token-2"), "user-a", "viewer", "tenant-a",
                List.of("profile.read"), "e-3");
        var rotated = analyzer.analyze(same, rotatedObservation);
        TestSupport.assertEquals(SessionCorrelationState.TOKEN_ROTATED, rotated.state(),
                "verified token renewal with stable context is represented as rotation");
        assertions++;
        TestSupport.assertTrue(rotated.tokenRotated(),
                "token rotation remains explicit");
        assertions++;
        TestSupport.assertEquals(List.of(), rotated.driftDimensions(),
                "safe token renewal does not invent context drift");
        assertions++;

        var driftObservation = verified(
                "obs-4", "session-a", fp("token-3"), "user-a", "admin", "tenant-b",
                List.of("profile.read", "admin.write"), "e-4");
        var drift = analyzer.analyze(rotatedObservation, driftObservation);
        TestSupport.assertEquals(SessionCorrelationState.CONTEXT_DRIFT, drift.state(),
                "token renewal that changes verified context becomes drift");
        assertions++;
        TestSupport.assertTrue(drift.verifiedContextDrift(),
                "verified context drift remains explicitly detectable");
        assertions++;
        TestSupport.assertTrue(drift.driftDimensions().contains(SessionContextDimension.ROLE),
                "role drift is preserved");
        assertions++;
        TestSupport.assertTrue(drift.driftDimensions().contains(SessionContextDimension.TENANT),
                "tenant drift is preserved");
        assertions++;
        TestSupport.assertTrue(drift.driftDimensions().contains(SessionContextDimension.SCOPE),
                "scope drift is preserved");
        assertions++;
        TestSupport.assertContains(String.join(",", drift.reasons()), "TOKEN_ROTATION_CONTEXT_DRIFT",
                "token-renewal context drift reason remains explicit");
        assertions++;

        var unverified = observation(
                "obs-5", "session-a", fp("token-4"), "user-z", "admin", "tenant-z",
                List.of("admin.write"), IdentityConfidenceState.INFERRED, "e-5");
        var unresolved = analyzer.analyze(driftObservation, unverified);
        TestSupport.assertEquals(SessionCorrelationState.UNVERIFIED_IDENTITY, unresolved.state(),
                "unverified identity must fail closed even when a token fingerprint exists");
        assertions++;
        TestSupport.assertTrue(!unresolved.currentIdentityVerified(),
                "token fingerprint is not equivalent to verified principal identity");
        assertions++;
        TestSupport.assertContains(String.join(",", unresolved.reasons()),
                "TOKEN_FINGERPRINT_DOES_NOT_VERIFY_PRINCIPAL",
                "identity-proof boundary is explicit");
        assertions++;

        var otherSession = verified(
                "obs-6", "session-b", fp("token-5"), "user-a", "viewer", "tenant-a",
                List.of("profile.read"), "e-6");
        var different = analyzer.analyze(rotatedObservation, otherSession);
        TestSupport.assertEquals(SessionCorrelationState.INCONCLUSIVE, different.state(),
                "different session identifiers are not correlated as one session");
        assertions++;
        TestSupport.assertContains(String.join(",", different.reasons()), "SESSION_ID_CHANGED",
                "session-boundary reason remains explicit");
        assertions++;

        TestSupport.assertEquals(
                analyzer.analyze(same, rotatedObservation).correlationId(),
                analyzer.analyze(same, rotatedObservation).correlationId(),
                "correlation identity is deterministic");
        assertions++;

        expectFailure(() -> observation(
                "obs-secret", "session-a", "Bearer raw-secret", "user-a", "viewer", "tenant-a",
                List.of("profile.read"), IdentityConfidenceState.USER_CONFIRMED, "e-secret"),
                "raw authentication material must not be accepted as token fingerprint");
        assertions++;

        return assertions;
    }

    private static AuthenticationSessionObservation verified(
            String observationId,
            String sessionId,
            String fingerprint,
            String principal,
            String role,
            String tenant,
            List<String> scopes,
            String evidence) {
        return observation(
                observationId,
                sessionId,
                fingerprint,
                principal,
                role,
                tenant,
                scopes,
                IdentityConfidenceState.USER_CONFIRMED,
                evidence);
    }

    private static AuthenticationSessionObservation observation(
            String observationId,
            String sessionId,
            String fingerprint,
            String principal,
            String role,
            String tenant,
            List<String> scopes,
            IdentityConfidenceState state,
            String evidence) {
        return new AuthenticationSessionObservation(
                observationId,
                sessionId,
                fingerprint,
                principal,
                role,
                tenant,
                scopes,
                AuthenticationType.OAUTH,
                state,
                Instant.parse("2026-09-24T16:00:00Z"),
                List.of(evidence));
    }

    private static String fp(String value) {
        return TokenFingerprint.sha256(value);
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
