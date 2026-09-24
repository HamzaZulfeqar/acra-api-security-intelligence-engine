package io.acra.core.tests.sprint11;

import io.acra.core.oauth.OAuthContextCorrelationState;
import io.acra.core.oauth.OAuthContextCorrelator;
import io.acra.core.oauth.OAuthContextDimension;
import io.acra.core.oauth.OAuthContextObservation;
import io.acra.core.oauth.OAuthProtocol;
import io.acra.core.oauth.PkceMethod;
import io.acra.core.recon.IdentityConfidenceState;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.tests.TestSupport;
import java.time.Instant;
import java.util.List;

public final class Sprint11OAuthOidcFoundationTestSuite {
    private static final Instant AT = Instant.parse("2026-09-24T18:40:00Z");

    private Sprint11OAuthOidcFoundationTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT11_OAUTH_OIDC_FOUNDATION PASS assertions=" + assertions);
    }

    public static int run() {
        int assertions = 0;
        OAuthContextCorrelator correlator = new OAuthContextCorrelator();

        OAuthContextObservation baseline = observation(
                "obs-a",
                "https://issuer.example",
                "client-a",
                TokenFingerprint.sha256("https://app.example/callback"),
                List.of("https://api.example"),
                List.of("api://orders"),
                List.of("openid", "profile", "orders.read"),
                List.of("code"),
                "authorization_code",
                PkceMethod.S256,
                true,
                true,
                IdentityConfidenceState.INFERRED);

        TestSupport.assertTrue(!baseline.identityVerified(),
                "passively inferred OAuth/OIDC context does not verify an identity");
        assertions++;
        TestSupport.assertTrue(baseline.redirectUriObserved(),
                "redirect observation is represented only by its fingerprint");
        assertions++;
        TestSupport.assertTrue(baseline.contextId().startsWith("s11-oauth-context-"),
                "OAuth context uses deterministic correlation identity");
        assertions++;

        OAuthContextObservation stable = observation(
                "obs-b",
                "https://issuer.example",
                "client-a",
                TokenFingerprint.sha256("https://app.example/callback"),
                List.of("https://api.example"),
                List.of("api://orders"),
                List.of("orders.read", "profile", "openid"),
                List.of("code"),
                "authorization_code",
                PkceMethod.S256,
                true,
                true,
                IdentityConfidenceState.USER_CONFIRMED);

        var stableResult = correlator.correlate(baseline, stable);
        TestSupport.assertEquals(OAuthContextCorrelationState.STABLE, stableResult.state(),
                "equivalent observed OAuth context remains stable");
        assertions++;
        TestSupport.assertEquals(List.of(), stableResult.driftDimensions(),
                "stable context has no invented drift dimensions");
        assertions++;
        TestSupport.assertEquals(baseline.contextId(), stable.contextId(),
                "context identity is independent of evidence ordering and identity confidence");
        assertions++;

        OAuthContextObservation drifted = observation(
                "obs-c",
                "https://issuer-two.example",
                "client-a",
                TokenFingerprint.sha256("https://app.example/alternate-callback"),
                List.of("https://api-two.example"),
                List.of("api://admin"),
                List.of("openid", "admin.write"),
                List.of("code", "id_token"),
                "authorization_code",
                PkceMethod.PLAIN,
                false,
                false,
                IdentityConfidenceState.USER_CONFIRMED);

        var drift = correlator.correlate(stable, drifted);
        TestSupport.assertEquals(OAuthContextCorrelationState.CONTEXT_DRIFT, drift.state(),
                "changed OAuth security context is retained as descriptive drift");
        assertions++;
        for (OAuthContextDimension dimension : List.of(
                OAuthContextDimension.ISSUER,
                OAuthContextDimension.REDIRECT_URI,
                OAuthContextDimension.RESOURCE,
                OAuthContextDimension.AUDIENCE,
                OAuthContextDimension.SCOPE,
                OAuthContextDimension.RESPONSE_TYPE,
                OAuthContextDimension.PKCE,
                OAuthContextDimension.STATE_SIGNAL,
                OAuthContextDimension.NONCE_SIGNAL)) {
            TestSupport.assertTrue(drift.driftDimensions().contains(dimension),
                    "drift preserves dimension " + dimension);
            assertions++;
        }
        TestSupport.assertTrue(drift.driftObserved(),
                "drift result exposes descriptive drift without vulnerability promotion");
        assertions++;

        OAuthContextObservation otherClient = observation(
                "obs-d",
                "https://issuer.example",
                "client-b",
                TokenFingerprint.sha256("https://app.example/callback"),
                List.of("https://api.example"),
                List.of("api://orders"),
                List.of("openid"),
                List.of("code"),
                "authorization_code",
                PkceMethod.S256,
                true,
                true,
                IdentityConfidenceState.USER_CONFIRMED);
        var unrelated = correlator.correlate(stable, otherClient);
        TestSupport.assertEquals(OAuthContextCorrelationState.INCONCLUSIVE, unrelated.state(),
                "different client context is not silently merged");
        assertions++;
        TestSupport.assertContains(String.join(",", unrelated.reasons()), "DIFFERENT_CLIENT_CONTEXT",
                "different-client reason remains explicit");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> observation(
                        "obs-query-issuer",
                        "https://issuer.example?token=secret",
                        "client-a",
                        "",
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        "UNKNOWN",
                        PkceMethod.UNKNOWN,
                        false,
                        false,
                        IdentityConfidenceState.UNKNOWN),
                "issuer query/secret material is rejected");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> observation(
                        "obs-raw-redirect",
                        "https://issuer.example",
                        "client-a",
                        "https://app.example/callback",
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        "UNKNOWN",
                        PkceMethod.UNKNOWN,
                        false,
                        false,
                        IdentityConfidenceState.UNKNOWN),
                "raw redirect URI cannot be stored where a SHA-256 handle is required");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> new OAuthContextObservation(
                        "obs-no-evidence",
                        "request-no-evidence",
                        OAuthProtocol.OIDC,
                        "https://issuer.example",
                        "https://issuer.example/authorize",
                        "https://issuer.example/token",
                        "client-a",
                        List.of(),
                        List.of(),
                        List.of("openid"),
                        List.of("code"),
                        "authorization_code",
                        "",
                        PkceMethod.S256,
                        true,
                        true,
                        IdentityConfidenceState.INFERRED,
                        AT,
                        List.of()),
                "OAuth context without provenance evidence is rejected");
        assertions++;

        TestSupport.assertEquals(
                correlator.correlate(baseline, stable).correlationId(),
                correlator.correlate(baseline, stable).correlationId(),
                "OAuth correlation identifier is deterministic");
        assertions++;

        return assertions;
    }

    private static OAuthContextObservation observation(
            String observationId,
            String issuer,
            String clientId,
            String redirectFingerprint,
            List<String> resources,
            List<String> audiences,
            List<String> scopes,
            List<String> responseTypes,
            String grantType,
            PkceMethod pkce,
            boolean statePresent,
            boolean noncePresent,
            IdentityConfidenceState identityState) {
        return new OAuthContextObservation(
                observationId,
                "request-" + observationId,
                OAuthProtocol.OIDC,
                issuer,
                issuer.equals("https://issuer-two.example")
                        ? "https://issuer-two.example/authorize"
                        : "https://issuer.example/authorize",
                issuer.equals("https://issuer-two.example")
                        ? "https://issuer-two.example/token"
                        : "https://issuer.example/token",
                clientId,
                resources,
                audiences,
                scopes,
                responseTypes,
                grantType,
                redirectFingerprint,
                pkce,
                statePresent,
                noncePresent,
                identityState,
                AT,
                List.of("evidence-" + observationId));
    }
}
