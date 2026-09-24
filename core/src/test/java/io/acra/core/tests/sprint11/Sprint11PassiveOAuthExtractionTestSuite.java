package io.acra.core.tests.sprint11;

import io.acra.core.domain.http.HttpHeader;
import io.acra.core.domain.http.HttpMethod;
import io.acra.core.domain.http.HttpProtocol;
import io.acra.core.domain.http.HttpRequest;
import io.acra.core.domain.http.HttpResponse;
import io.acra.core.domain.http.HttpTransaction;
import io.acra.core.oauth.OAuthContextCorrelationState;
import io.acra.core.oauth.OAuthContextCorrelator;
import io.acra.core.oauth.OAuthProtocol;
import io.acra.core.oauth.PkceMethod;
import io.acra.core.oauth.S11PassiveOAuthExtractor;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.serialization.DomainSerializer;
import io.acra.core.tests.TestSupport;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class Sprint11PassiveOAuthExtractionTestSuite {
    private static final Instant AT = Instant.parse("2026-09-24T18:50:00Z");

    private Sprint11PassiveOAuthExtractionTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT11_PASSIVE_OAUTH_EXTRACTION PASS assertions=" + assertions);
    }

    public static int run() {
        int assertions = 0;
        S11PassiveOAuthExtractor extractor = new S11PassiveOAuthExtractor();

        String rawRedirect = "https://app.example/callback";
        String rawState = "state-value-must-not-persist";
        String rawNonce = "nonce-value-must-not-persist";
        String rawChallenge = "pkce-challenge-must-not-persist";
        String authorizationTarget =
                "/authorize?client_id=client-a"
                + "&response_type=code"
                + "&scope=openid%20profile%20orders.read"
                + "&redirect_uri=https%3A%2F%2Fapp.example%2Fcallback"
                + "&state=" + rawState
                + "&nonce=" + rawNonce
                + "&code_challenge=" + rawChallenge
                + "&code_challenge_method=S256"
                + "&resource=https%3A%2F%2Fapi.example"
                + "&audience=api%3A%2F%2Forders";

        var authResult = extractor.extract(transaction(
                HttpMethod.GET,
                authorizationTarget,
                List.of(new HttpHeader("Accept", "text/html")),
                "",
                Map.of(
                        "oauth.issuer", "https://issuer.example",
                        "oauth.token_endpoint", "https://issuer.example/token",
                        "oauth.protocol", "oidc"),
                "request-auth")).orElseThrow();

        var auth = authResult.observation();
        TestSupport.assertEquals(OAuthProtocol.OIDC, auth.protocol(),
                "openid authorization request is represented as OIDC");
        assertions++;
        TestSupport.assertEquals("https://issuer.example", auth.issuer(),
                "explicit issuer metadata is preserved");
        assertions++;
        TestSupport.assertEquals("client-a", auth.clientId(),
                "client_id is extracted from observed authorization request");
        assertions++;
        TestSupport.assertEquals(TokenFingerprint.sha256(rawRedirect), auth.redirectUriFingerprint(),
                "redirect URI is retained only as SHA-256 handle");
        assertions++;
        TestSupport.assertEquals(PkceMethod.S256, auth.pkceMethod(),
                "observed S256 PKCE method is preserved");
        assertions++;
        TestSupport.assertTrue(auth.statePresent(), "state presence is retained");
        assertions++;
        TestSupport.assertTrue(auth.noncePresent(), "nonce presence is retained");
        assertions++;
        TestSupport.assertEquals(List.of("openid", "orders.read", "profile"), auth.scopes(),
                "scope values are normalized deterministically");
        assertions++;
        TestSupport.assertEquals(List.of("https://api.example"), auth.resourceIndicators(),
                "resource indicator is preserved");
        assertions++;
        TestSupport.assertEquals(List.of("api://orders"), auth.audiences(),
                "audience is preserved");
        assertions++;

        String serializedAuth = new DomainSerializer().serialize(authResult);
        for (String secret : List.of(rawRedirect, rawState, rawNonce, rawChallenge)) {
            TestSupport.assertNotContains(serializedAuth, secret,
                    "passive authorization extraction excludes raw sensitive value");
            assertions++;
        }
        TestSupport.assertNotContains(auth.authorizationEndpoint(), "?",
                "authorization endpoint projection excludes query material");
        assertions++;

        String rawCode = "authorization-code-must-not-persist";
        String rawVerifier = "pkce-verifier-must-not-persist";
        String tokenBody = "grant_type=authorization_code"
                + "&client_id=client-a"
                + "&code=" + rawCode
                + "&code_verifier=" + rawVerifier
                + "&redirect_uri=https%3A%2F%2Fapp.example%2Fcallback"
                + "&scope=openid%20profile";

        var tokenResult = extractor.extract(transaction(
                HttpMethod.POST,
                "/token",
                List.of(new HttpHeader("Content-Type", "application/x-www-form-urlencoded")),
                tokenBody,
                Map.of(
                        "oauth.issuer", "https://issuer.example",
                        "oauth.authorization_endpoint", "https://issuer.example/authorize"),
                "request-token")).orElseThrow();

        var token = tokenResult.observation();
        TestSupport.assertEquals("authorization_code", token.grantType(),
                "token endpoint grant_type is preserved");
        assertions++;
        TestSupport.assertEquals("https://issuer.example/token", token.tokenEndpoint(),
                "observed token endpoint is derived without form/query material");
        assertions++;
        TestSupport.assertEquals(PkceMethod.UNKNOWN, token.pkceMethod(),
                "code_verifier presence does not invent the original PKCE method");
        assertions++;

        String serializedToken = new DomainSerializer().serialize(tokenResult);
        for (String secret : List.of(rawRedirect, rawCode, rawVerifier)) {
            TestSupport.assertNotContains(serializedToken, secret,
                    "passive token extraction excludes raw sensitive value");
            assertions++;
        }
        TestSupport.assertContains(serializedToken, "presence-only",
                "token verifier is represented only by a presence evidence signal");
        assertions++;

        var nonOauth = extractor.extract(transaction(
                HttpMethod.GET,
                "/api/v1/orders",
                List.of(new HttpHeader("Accept", "application/json")),
                "",
                Map.of(),
                "request-normal"));
        TestSupport.assertTrue(nonOauth.isEmpty(),
                "ordinary application request is not misclassified as OAuth");
        assertions++;

        var unknownIssuer = extractor.extract(transaction(
                HttpMethod.GET,
                "/authorize?client_id=client-a&response_type=code&scope=openid",
                List.of(),
                "",
                Map.of(),
                "request-unknown-issuer")).orElseThrow().observation();
        TestSupport.assertEquals("UNKNOWN", unknownIssuer.issuer(),
                "missing issuer remains explicitly unknown");
        assertions++;
        var unknownCorrelation = new OAuthContextCorrelator().correlate(auth, unknownIssuer);
        TestSupport.assertEquals(OAuthContextCorrelationState.INCONCLUSIVE, unknownCorrelation.state(),
                "known and unknown issuer contexts fail closed as inconclusive");
        assertions++;
        TestSupport.assertContains(String.join(",", unknownCorrelation.reasons()), "ISSUER_UNKNOWN",
                "unknown issuer reason remains explicit");
        assertions++;

        var malformed = extractor.extract(transaction(
                HttpMethod.GET,
                "/authorize?client_id=%ZZ&response_type=code",
                List.of(),
                "",
                Map.of(),
                "request-malformed"));
        TestSupport.assertTrue(malformed.isEmpty(),
                "malformed query encoding does not produce partial OAuth context");
        assertions++;

        return assertions;
    }

    private static HttpTransaction transaction(
            HttpMethod method,
            String target,
            List<HttpHeader> headers,
            String body,
            Map<String, String> metadata,
            String requestId) {
        HttpRequest request = HttpRequest.of(
                method,
                "https",
                "issuer.example",
                443,
                target,
                headers,
                body.getBytes(StandardCharsets.UTF_8),
                HttpProtocol.HTTP_1_1);
        HttpResponse response = new HttpResponse(
                200,
                List.of(),
                "{}".getBytes(StandardCharsets.UTF_8),
                "application/json",
                HttpProtocol.HTTP_1_1,
                new byte[0]);
        return new HttpTransaction(request, response, AT, requestId, "SPRINT11_TEST", metadata);
    }
}
