package io.acra.core.tests.sprint10;

import io.acra.core.active.evidence.EvidenceStage;
import io.acra.core.active.evidence.ExecutionEvidenceStore;
import io.acra.core.domain.identity.AuthenticationType;
import io.acra.core.recon.IdentityConfidenceState;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.session.AuthenticationSessionObservation;
import io.acra.core.session.S10SessionContextAnalyzer;
import io.acra.core.session.S10SessionEvidenceValidator;
import io.acra.core.session.SessionContextDimension;
import io.acra.core.session.SessionCorrelationState;
import io.acra.core.session.SessionEvidenceBinding;
import io.acra.core.tests.TestSupport;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

public final class Sprint10ControlledSessionContextTestSuite {
    private static final String PROJECT = "acra-s10";
    private static final int PORT = 18082;
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    private Sprint10ControlledSessionContextTestSuite() { }

    public static void main(String[] args) throws Exception {
        int assertions = run();
        System.out.println("SPRINT10_CONTROLLED_SESSION_CONTEXT PASS assertions=" + assertions);
    }

    public static int run() throws Exception {
        int assertions = 0;
        ExecutionEvidenceStore store = new ExecutionEvidenceStore(PROJECT);
        S10SessionEvidenceValidator provenance = new S10SessionEvidenceValidator(store);
        S10SessionContextAnalyzer analyzer = new S10SessionContextAnalyzer();

        Captured baseline = capture(
                store, provenance,
                "obs-s10-1", "exec-s10-1", "test-s10-1", "e-s10-1",
                "session-a", "user-a", "viewer", "tenant-a", List.of("profile.read"), "token-a");
        TestSupport.assertEquals(200, baseline.status(),
                "baseline session context must be observable from controlled localhost lab");
        assertions++;
        TestSupport.assertTrue(baseline.provenanceValid(),
                "baseline session observation provenance must validate");
        assertions++;
        TestSupport.assertNotContains(baseline.body(), baseline.rawToken(),
                "lab response must not return raw bearer material");
        assertions++;
        TestSupport.assertContains(baseline.body(), "\"principal_id\":\"user-a\"",
                "baseline response exposes the synthetic lab principal context");
        assertions++;

        var baselineResult = analyzer.analyze(null, baseline.observation());
        TestSupport.assertEquals(SessionCorrelationState.BASELINE, baselineResult.state(),
                "first verified lab observation establishes baseline");
        assertions++;

        Captured stableRotation = capture(
                store, provenance,
                "obs-s10-2", "exec-s10-2", "test-s10-2", "e-s10-2",
                "session-a", "user-a", "viewer", "tenant-a", List.of("profile.read"), "token-b");
        TestSupport.assertTrue(stableRotation.provenanceValid(),
                "stable rotation observation provenance must validate");
        assertions++;
        TestSupport.assertNotContains(stableRotation.body(), stableRotation.rawToken(),
                "stable rotation response must not return raw bearer material");
        assertions++;
        var rotatedResult = analyzer.analyze(baseline.observation(), stableRotation.observation());
        TestSupport.assertEquals(SessionCorrelationState.TOKEN_ROTATED, rotatedResult.state(),
                "same verified context with a new token fingerprint is a safe rotation");
        assertions++;
        TestSupport.assertEquals(List.of(), rotatedResult.driftDimensions(),
                "safe rotation has no principal/role/tenant/scope drift");
        assertions++;

        Captured driftRotation = capture(
                store, provenance,
                "obs-s10-3", "exec-s10-3", "test-s10-3", "e-s10-3",
                "session-a", "user-a", "admin", "tenant-b",
                List.of("admin.write", "profile.read"), "token-c");
        TestSupport.assertTrue(driftRotation.provenanceValid(),
                "drift observation provenance must validate");
        assertions++;
        TestSupport.assertContains(driftRotation.body(), "\"role\":\"admin\"",
                "controlled drift response carries the declared changed role");
        assertions++;
        TestSupport.assertContains(driftRotation.body(), "\"tenant_id\":\"tenant-b\"",
                "controlled drift response carries the declared changed tenant");
        assertions++;

        var driftResult = analyzer.analyze(stableRotation.observation(), driftRotation.observation());
        TestSupport.assertEquals(SessionCorrelationState.CONTEXT_DRIFT, driftResult.state(),
                "verified token rotation with changed context becomes explicit drift");
        assertions++;
        TestSupport.assertTrue(driftResult.driftDimensions().contains(SessionContextDimension.ROLE),
                "controlled drift retains role dimension");
        assertions++;
        TestSupport.assertTrue(driftResult.driftDimensions().contains(SessionContextDimension.TENANT),
                "controlled drift retains tenant dimension");
        assertions++;
        TestSupport.assertTrue(driftResult.driftDimensions().contains(SessionContextDimension.SCOPE),
                "controlled drift retains scope dimension");
        assertions++;

        Captured otherSession = capture(
                store, provenance,
                "obs-s10-4", "exec-s10-4", "test-s10-4", "e-s10-4",
                "session-b", "user-a", "viewer", "tenant-a", List.of("profile.read"), "token-d");
        TestSupport.assertTrue(otherSession.provenanceValid(),
                "separate-session observation provenance must validate");
        assertions++;
        var separate = analyzer.analyze(stableRotation.observation(), otherSession.observation());
        TestSupport.assertEquals(SessionCorrelationState.INCONCLUSIVE, separate.state(),
                "different session IDs remain outside the same-session correlation boundary");
        assertions++;

        return assertions;
    }

    private static Captured capture(
            ExecutionEvidenceStore store,
            S10SessionEvidenceValidator validator,
            String observationId,
            String executionId,
            String testId,
            String evidenceId,
            String sessionId,
            String principal,
            String role,
            String tenant,
            List<String> scopes,
            String tokenSeed) throws Exception {

        String rawToken = token(principal, role, tenant, scopes, tokenSeed);
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + PORT + "/api/v1/s10/session-context"))
                .timeout(Duration.ofSeconds(2))
                .header("Authorization", "Bearer " + rawToken)
                .header("X-Session-ID", sessionId)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        TestSupport.assertContains(response.body(), "\"session_id\":\"" + sessionId + "\"",
                "lab response session ID must match declared fixture");
        TestSupport.assertContains(response.body(), "\"principal_id\":\"" + principal + "\"",
                "lab response principal must match declared fixture");
        TestSupport.assertContains(response.body(), "\"role\":\"" + role + "\"",
                "lab response role must match declared fixture");
        TestSupport.assertContains(response.body(), "\"tenant_id\":\"" + tenant + "\"",
                "lab response tenant must match declared fixture");

        AuthenticationSessionObservation observation = new AuthenticationSessionObservation(
                observationId,
                sessionId,
                TokenFingerprint.sha256(rawToken),
                principal,
                role,
                tenant,
                scopes,
                AuthenticationType.OAUTH,
                IdentityConfidenceState.LAB_CONFIRMED,
                Instant.parse("2026-09-24T17:10:00Z"),
                List.of(evidenceId));

        store.append(executionId, testId, EvidenceStage.TEST, evidenceId, "s10-controlled-session-context");
        store.append(executionId, testId, EvidenceStage.OBSERVATION, observationId, observation);

        boolean valid = validator.validate(new SessionEvidenceBinding(
                PROJECT, executionId, testId, observationId, List.of(evidenceId))).valid();

        return new Captured(observation, response.statusCode(), response.body(), rawToken, valid);
    }

    private static String token(
            String sub,
            String role,
            String tenant,
            List<String> scopes,
            String seed) {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String header = encoder.encodeToString(
                "{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String scope = String.join(" ", scopes);
        String payload = encoder.encodeToString(
                ("{\"sub\":\"" + sub + "\",\"tenant_id\":\"" + tenant
                        + "\",\"role\":\"" + role + "\",\"scope\":\"" + scope
                        + "\",\"seed\":\"" + seed + "\"}").getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".s10synthetic";
    }

    private record Captured(
            AuthenticationSessionObservation observation,
            int status,
            String body,
            String rawToken,
            boolean provenanceValid) { }
}
