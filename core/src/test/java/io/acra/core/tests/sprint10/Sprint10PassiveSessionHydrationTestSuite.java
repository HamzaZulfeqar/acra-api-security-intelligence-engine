package io.acra.core.tests.sprint10;

import io.acra.core.domain.http.HttpHeader;
import io.acra.core.domain.http.HttpMethod;
import io.acra.core.domain.http.HttpProtocol;
import io.acra.core.domain.http.HttpRequest;
import io.acra.core.domain.http.HttpTransaction;
import io.acra.core.engine.SecurityContextEngine;
import io.acra.core.recon.IdentityConfidenceState;
import io.acra.core.recon.IdentityConfirmationRegistry;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.serialization.DomainSerializer;
import io.acra.core.session.S10PassiveSessionHydrator;
import io.acra.core.tests.TestSupport;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public final class Sprint10PassiveSessionHydrationTestSuite {
    private Sprint10PassiveSessionHydrationTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT10_PASSIVE_SESSION_HYDRATION PASS assertions=" + assertions);
    }

    public static int run() {
        int assertions = 0;
        SecurityContextEngine engine = new SecurityContextEngine();

        String rawJwt = token("user-a", "viewer", "tenant-a");
        HttpTransaction transaction = transaction(
                "req-s10-passive-1",
                rawJwt,
                Map.of("session_id", "browser-session-a", "auth.scopes", "profile.read profile.write"));
        var snapshot = engine.analyze(transaction);
        String fingerprint = TokenFingerprint.sha256(rawJwt);

        IdentityConfirmationRegistry emptyRegistry = new IdentityConfirmationRegistry();
        var inferred = new S10PassiveSessionHydrator(emptyRegistry)
                .hydrate(transaction, snapshot)
                .orElseThrow();
        TestSupport.assertEquals(IdentityConfidenceState.INFERRED,
                inferred.observation().identityState(),
                "unconfirmed JWT claims remain inferred, not verified");
        assertions++;
        TestSupport.assertEquals("user-a", inferred.observation().principalId(),
                "passive JWT subject may be retained as inferred context");
        assertions++;
        TestSupport.assertEquals("viewer", inferred.observation().roleId(),
                "passive JWT role may be retained as inferred context");
        assertions++;
        TestSupport.assertEquals("tenant-a", inferred.observation().tenantId(),
                "passive tenant claim may be retained as inferred context");
        assertions++;
        TestSupport.assertEquals(List.of("profile.read", "profile.write"), inferred.observation().scopes(),
                "collector-provided scope metadata is normalized deterministically");
        assertions++;
        TestSupport.assertTrue(!inferred.observation().identityVerified(),
                "inferred token context must not be treated as identity proof");
        assertions++;
        TestSupport.assertContains(String.join(",", inferred.reasons()), "IDENTITY_CONTEXT_NOT_CONFIRMED",
                "unconfirmed identity reason remains explicit");
        assertions++;

        IdentityConfirmationRegistry confirmedRegistry = new IdentityConfirmationRegistry();
        confirmedRegistry.confirmUser(
                fingerprint, "user-a", "viewer", "tenant-a", "explicit-user-confirmation");
        var confirmed = new S10PassiveSessionHydrator(confirmedRegistry)
                .hydrate(transaction, snapshot)
                .orElseThrow();
        TestSupport.assertEquals(IdentityConfidenceState.USER_CONFIRMED,
                confirmed.observation().identityState(),
                "matching confirmation upgrades passive session context to verified");
        assertions++;
        TestSupport.assertTrue(confirmed.observation().identityVerified(),
                "confirmed passive identity is explicitly verified");
        assertions++;
        TestSupport.assertTrue(!confirmed.confirmationConflict(),
                "matching confirmation has no context conflict");
        assertions++;

        IdentityConfirmationRegistry conflictRegistry = new IdentityConfirmationRegistry();
        conflictRegistry.confirmUser(
                fingerprint, "user-b", "admin", "tenant-b", "conflicting-user-confirmation");
        var conflict = new S10PassiveSessionHydrator(conflictRegistry)
                .hydrate(transaction, snapshot)
                .orElseThrow();
        TestSupport.assertEquals(IdentityConfidenceState.SUSPECTED,
                conflict.observation().identityState(),
                "confirmation/claim mismatch must be downgraded to suspected");
        assertions++;
        TestSupport.assertTrue(conflict.confirmationConflict(),
                "confirmation conflict remains explicit");
        assertions++;
        TestSupport.assertTrue(!conflict.observation().identityVerified(),
                "conflicting context must not be treated as verified identity");
        assertions++;
        TestSupport.assertContains(String.join(",", conflict.reasons()),
                "CONFIRMED_IDENTITY_CONTEXT_CONFLICT",
                "confirmation conflict reason remains explicit");
        assertions++;

        HttpTransaction missingSessionId = transaction("req-s10-passive-2", rawJwt, Map.of());
        var missingSessionSnapshot = engine.analyze(missingSessionId);
        var unresolvedSession = new S10PassiveSessionHydrator(emptyRegistry)
                .hydrate(missingSessionId, missingSessionSnapshot)
                .orElseThrow();
        TestSupport.assertContains(unresolvedSession.observation().sessionId(), "unresolved:req-s10-passive-2",
                "missing session identifier fails closed to request-scoped unresolved identity");
        assertions++;
        TestSupport.assertContains(String.join(",", unresolvedSession.reasons()), "SESSION_ID_UNAVAILABLE",
                "missing session identifier reason remains explicit");
        assertions++;

        HttpTransaction noCredential = noCredentialTransaction();
        var noCredentialSnapshot = engine.analyze(noCredential);
        TestSupport.assertTrue(
                new S10PassiveSessionHydrator(emptyRegistry).hydrate(noCredential, noCredentialSnapshot).isEmpty(),
                "traffic without token/session credential evidence does not create a session observation");
        assertions++;

        String serialized = new DomainSerializer().serialize(confirmed.observation());
        TestSupport.assertNotContains(serialized, rawJwt,
                "hydrated session observation never serializes the raw bearer token");
        assertions++;
        TestSupport.assertContains(serialized, fingerprint,
                "hydrated session observation retains only the SHA-256 correlation handle");
        assertions++;

        return assertions;
    }

    private static HttpTransaction transaction(String requestId, String rawToken, Map<String, String> metadata) {
        HttpRequest request = HttpRequest.of(
                HttpMethod.GET,
                "http",
                "localhost",
                18082,
                "/api/v1/tenants/tenant-a/documents/1001",
                List.of(
                        new HttpHeader("Authorization", "Bearer " + rawToken),
                        new HttpHeader("Accept", "application/json")),
                new byte[0],
                HttpProtocol.HTTP_1_1);
        return new HttpTransaction(
                request,
                null,
                Instant.parse("2026-09-24T17:20:00Z"),
                requestId,
                "burp-passive",
                metadata);
    }

    private static HttpTransaction noCredentialTransaction() {
        HttpRequest request = HttpRequest.of(
                HttpMethod.GET,
                "http",
                "localhost",
                18082,
                "/public",
                List.of(new HttpHeader("Accept", "application/json")),
                new byte[0],
                HttpProtocol.HTTP_1_1);
        return new HttpTransaction(
                request,
                null,
                Instant.parse("2026-09-24T17:20:00Z"),
                "req-s10-passive-none",
                "burp-passive",
                Map.of());
    }

    private static String token(String sub, String role, String tenant) {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String header = encoder.encodeToString(
                "{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = encoder.encodeToString(
                ("{\"sub\":\"" + sub + "\",\"tenant_id\":\"" + tenant
                        + "\",\"role\":\"" + role + "\"}").getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".s10passive";
    }
}
