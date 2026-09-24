package io.acra.core.tests.sprint11;

import io.acra.core.active.analysis.AuthorizationOutcome;
import io.acra.core.active.analysis.AuthorizationOutcomeNormalizer;
import io.acra.core.analysis.PassiveDifferentialComparator;
import io.acra.core.analysis.ResponseComparisonMode;
import io.acra.core.analysis.semantic.ResponseSemanticAnalyzer;
import io.acra.core.domain.http.HttpProtocol;
import io.acra.core.domain.http.HttpResponse;
import io.acra.core.tests.TestSupport;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

public final class Sprint11ControlledResearchLabFixtureTestSuite {
    private static final int SECURE = 18082;
    private static final int VULNERABLE = 18081;
    private static final String TOKEN = token("user-a", "tenant-a", "viewer");
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();
    private static final ResponseSemanticAnalyzer SEMANTIC = new ResponseSemanticAnalyzer();
    private static final AuthorizationOutcomeNormalizer OUTCOME = new AuthorizationOutcomeNormalizer();
    private static final PassiveDifferentialComparator COMPARATOR = new PassiveDifferentialComparator();

    private Sprint11ControlledResearchLabFixtureTestSuite() { }

    public static void main(String[] args) throws Exception {
        int assertions = run();
        System.out.println("SPRINT11_CONTROLLED_RESEARCH_LAB_FIXTURES PASS assertions=" + assertions);
    }

    public static int run() throws Exception {
        int assertions = 0;

        LabResponse timestampA = get(SECURE, "/api/v1/s11/research/case-003?variant=a", false, false);
        LabResponse timestampB = get(SECURE, "/api/v1/s11/research/case-003?variant=b", false, false);
        TestSupport.assertTrue(!timestampA.body().equals(timestampB.body()),
                "timestamp-only fixture has distinct raw bodies");
        assertions++;
        TestSupport.assertEquals(
                timestampA.body().replaceAll("\"timestamp\":\"[^\"]+\"", "\"timestamp\":\"<volatile>\""),
                timestampB.body().replaceAll("\"timestamp\":\"[^\"]+\"", "\"timestamp\":\"<volatile>\""),
                "timestamp-only fixture differs only in body timestamp");
        assertions++;
        TestSupport.assertContains(timestampA.body(), "\"request_id\":\"fixed-003\"",
                "timestamp-only fixture keeps request id fixed");
        assertions++;
        TestSupport.assertTrue(semanticEquivalent(timestampA, timestampB),
                "timestamp-only fixture remains semantically equivalent");
        assertions++;

        LabResponse requestA = get(SECURE, "/api/v1/s11/research/case-004?variant=a", false, false);
        LabResponse requestB = get(SECURE, "/api/v1/s11/research/case-004?variant=b", false, false);
        TestSupport.assertTrue(!requestA.body().equals(requestB.body()),
                "request-id-only fixture has distinct raw bodies");
        assertions++;
        TestSupport.assertEquals(
                requestA.body().replaceAll("\"request_id\":\"[^\"]+\"", "\"request_id\":\"<volatile>\""),
                requestB.body().replaceAll("\"request_id\":\"[^\"]+\"", "\"request_id\":\"<volatile>\""),
                "request-id-only fixture differs only in body request_id");
        assertions++;
        TestSupport.assertContains(requestA.body(), "\"timestamp\":\"2026-09-25T00:00:00Z\"",
                "request-id-only fixture keeps timestamp fixed");
        assertions++;
        TestSupport.assertTrue(semanticEquivalent(requestA, requestB),
                "request-id-only fixture remains semantically equivalent");
        assertions++;

        LabResponse orderingA = get(SECURE, "/api/v1/s11/research/case-005?variant=a", false, false);
        LabResponse orderingB = get(SECURE, "/api/v1/s11/research/case-005?variant=b", false, false);
        TestSupport.assertTrue(!orderingA.body().equals(orderingB.body()),
                "ordering-only fixture has distinct raw bodies");
        assertions++;
        TestSupport.assertEquals(orderingA.body().length(), orderingB.body().length(),
                "ordering-only fixture preserves body length");
        assertions++;
        TestSupport.assertTrue(!orderingA.body().contains("\n") && !orderingB.body().contains("\n"),
                "ordering-only fixture keeps compact formatting");
        assertions++;
        TestSupport.assertTrue(semanticEquivalent(orderingA, orderingB),
                "ordering-only fixture remains semantically equivalent");
        assertions++;

        LabResponse formattingA = get(SECURE, "/api/v1/s11/research/case-006?variant=a", false, false);
        LabResponse formattingB = get(SECURE, "/api/v1/s11/research/case-006?variant=b", false, false);
        TestSupport.assertTrue(!formattingA.body().equals(formattingB.body()),
                "formatting-only fixture has distinct raw bodies");
        assertions++;
        TestSupport.assertTrue(!formattingA.body().contains("\n") && formattingB.body().contains("\n"),
                "formatting-only fixture isolates pretty-print whitespace");
        assertions++;
        TestSupport.assertEquals(
                formattingA.body().replaceAll("\\s+", ""),
                formattingB.body().replaceAll("\\s+", ""),
                "formatting-only fixture preserves compact JSON content");
        assertions++;
        TestSupport.assertTrue(semanticEquivalent(formattingA, formattingB),
                "formatting-only fixture remains semantically equivalent");
        assertions++;

        LabResponse sameStatusSecure = get(SECURE, "/api/v1/s11/research/case-008?variant=a", true, false);
        LabResponse sameStatusVulnerable = get(VULNERABLE, "/api/v1/s11/research/case-008?variant=a", true, false);
        TestSupport.assertEquals(200, sameStatusSecure.status(), "same-status secure response is HTTP 200");
        assertions++;
        TestSupport.assertEquals(200, sameStatusVulnerable.status(), "same-status vulnerable response is HTTP 200");
        assertions++;
        TestSupport.assertEquals(AuthorizationOutcome.DENY, outcome(sameStatusSecure),
                "same-status secure response is semantically DENY");
        assertions++;
        TestSupport.assertEquals(AuthorizationOutcome.ALLOW, outcome(sameStatusVulnerable),
                "same-status vulnerable response is semantically ALLOW");
        assertions++;

        LabResponse softSecure = get(SECURE, "/api/v1/s11/research/case-009?variant=a", true, false);
        LabResponse softVulnerable = get(VULNERABLE, "/api/v1/s11/research/case-009?variant=a", true, false);
        TestSupport.assertEquals(200, softSecure.status(), "soft-denial secure response stays HTTP 200");
        assertions++;
        TestSupport.assertEquals(AuthorizationOutcome.DENY, outcome(softSecure),
                "soft-denial secure response normalizes to DENY");
        assertions++;
        TestSupport.assertEquals(AuthorizationOutcome.ALLOW, outcome(softVulnerable),
                "soft-denial vulnerable fixture exposes foreign resource");
        assertions++;

        LabResponse dynamicSecureShort = get(SECURE, "/api/v1/s11/research/case-010?variant=a&pad=2", true, false);
        LabResponse dynamicSecureLong = get(SECURE, "/api/v1/s11/research/case-010?variant=a&pad=17", true, false);
        LabResponse dynamicVulnerableShort = get(VULNERABLE, "/api/v1/s11/research/case-010?variant=a&pad=2", true, false);
        LabResponse dynamicVulnerableLong = get(VULNERABLE, "/api/v1/s11/research/case-010?variant=a&pad=17", true, false);
        TestSupport.assertTrue(dynamicSecureShort.body().length() != dynamicSecureLong.body().length(),
                "dynamic-length secure bodies vary in length");
        assertions++;
        TestSupport.assertTrue(dynamicVulnerableShort.body().length() != dynamicVulnerableLong.body().length(),
                "dynamic-length vulnerable bodies vary in length");
        assertions++;
        TestSupport.assertEquals(AuthorizationOutcome.DENY, outcome(dynamicSecureShort),
                "dynamic-length secure meaning remains DENY");
        assertions++;
        TestSupport.assertEquals(AuthorizationOutcome.DENY, outcome(dynamicSecureLong),
                "dynamic-length secure meaning is stable across length");
        assertions++;
        TestSupport.assertEquals(AuthorizationOutcome.ALLOW, outcome(dynamicVulnerableShort),
                "dynamic-length vulnerable meaning remains ALLOW");
        assertions++;
        TestSupport.assertEquals(AuthorizationOutcome.ALLOW, outcome(dynamicVulnerableLong),
                "dynamic-length vulnerable meaning is stable across length");
        assertions++;

        LabResponse reorderedSecure = get(SECURE, "/api/v1/s11/research/case-011?variant=a", true, false);
        LabResponse reorderedVulnerableA = get(VULNERABLE, "/api/v1/s11/research/case-011?variant=a", true, false);
        LabResponse reorderedVulnerableB = get(VULNERABLE, "/api/v1/s11/research/case-011?variant=b", true, false);
        TestSupport.assertEquals(AuthorizationOutcome.DENY, outcome(reorderedSecure),
                "reordered-json secure response denies");
        assertions++;
        TestSupport.assertEquals(AuthorizationOutcome.ALLOW, outcome(reorderedVulnerableA),
                "reordered-json vulnerable response allows");
        assertions++;
        TestSupport.assertTrue(!reorderedVulnerableA.body().equals(reorderedVulnerableB.body()),
                "reordered-json vulnerable variants differ in raw order");
        assertions++;
        TestSupport.assertTrue(semanticEquivalent(reorderedVulnerableA, reorderedVulnerableB),
                "reordered-json vulnerable variants preserve semantic meaning");
        assertions++;

        LabResponse opaqueSecure = get(SECURE, "/api/v1/s11/research/case-012?variant=a", true, false);
        LabResponse opaqueVulnerable = get(VULNERABLE, "/api/v1/s11/research/case-012?variant=a", true, false);
        TestSupport.assertEquals(AuthorizationOutcome.DENY, outcome(opaqueSecure),
                "opaque-id secure response denies");
        assertions++;
        TestSupport.assertEquals(AuthorizationOutcome.ALLOW, outcome(opaqueVulnerable),
                "opaque-id vulnerable response allows");
        assertions++;
        TestSupport.assertTrue(toDomain(opaqueVulnerable).bodyUtf8().contains("Q7M2-X9P4-ZETA"),
                "opaque identifier is preserved in vulnerable response");
        assertions++;

        LabResponse nestedSecure = get(SECURE, "/api/v1/s11/research/case-013?variant=a", true, false);
        LabResponse nestedVulnerable = get(VULNERABLE, "/api/v1/s11/research/case-013?variant=a", true, false);
        var nestedFingerprint = SEMANTIC.fingerprint(toDomain(nestedVulnerable));
        TestSupport.assertEquals(AuthorizationOutcome.DENY, outcome(nestedSecure),
                "nested secure response denies");
        assertions++;
        TestSupport.assertEquals(AuthorizationOutcome.ALLOW, outcome(nestedVulnerable),
                "nested vulnerable response allows");
        assertions++;
        TestSupport.assertTrue(nestedFingerprint.resourceIds().contains("nested-foreign-013"),
                "nested foreign resource ID is visible to semantic evidence");
        assertions++;
        TestSupport.assertTrue(nestedFingerprint.ownerIds().contains("user-b"),
                "nested foreign owner is visible to semantic evidence");
        assertions++;

        LabResponse collectionSecure = get(SECURE, "/api/v1/s11/research/case-014?variant=a", true, false);
        LabResponse collectionVulnerable = get(VULNERABLE, "/api/v1/s11/research/case-014?variant=a", true, false);
        TestSupport.assertEquals(AuthorizationOutcome.ALLOW, outcome(collectionSecure),
                "collection secure response is an allowed owned collection");
        assertions++;
        TestSupport.assertEquals(AuthorizationOutcome.ALLOW, outcome(collectionVulnerable),
                "collection vulnerable response remains HTTP/semantic ALLOW");
        assertions++;
        TestSupport.assertNotContains(collectionSecure.body(), "foreign-014",
                "secure collection excludes foreign member");
        assertions++;
        TestSupport.assertContains(collectionVulnerable.body(), "foreign-014",
                "vulnerable collection contains foreign member");
        assertions++;
        var secureCollectionFingerprint = SEMANTIC.fingerprint(toDomain(collectionSecure));
        var vulnerableCollectionFingerprint = SEMANTIC.fingerprint(toDomain(collectionVulnerable));
        TestSupport.assertTrue(!secureCollectionFingerprint.resourceIds()
                        .equals(vulnerableCollectionFingerprint.resourceIds()),
                "collection membership change is visible in extracted resource evidence");
        assertions++;
        TestSupport.assertTrue(vulnerableCollectionFingerprint.resourceIds().contains("foreign-014"),
                "collection semantic evidence captures the foreign member");
        assertions++;

        LabResponse nonstandardSecure = get(SECURE, "/api/v1/s11/research/case-015?variant=a", false, true);
        LabResponse nonstandardVulnerable = get(VULNERABLE, "/api/v1/s11/research/case-015?variant=a", false, true);
        TestSupport.assertEquals(AuthorizationOutcome.DENY, outcome(nonstandardSecure),
                "nonstandard-auth secure response denies");
        assertions++;
        TestSupport.assertEquals(AuthorizationOutcome.ALLOW, outcome(nonstandardVulnerable),
                "nonstandard-auth vulnerable response allows");
        assertions++;
        LabResponse nonstandardMissing = get(SECURE, "/api/v1/s11/research/case-015?variant=a", false, false);
        TestSupport.assertEquals(401, nonstandardMissing.status(),
                "nonstandard-auth fixture actually requires its synthetic custom header");
        assertions++;

        for (int index : List.of(8, 9, 10, 11, 12, 13, 14, 15)) {
            String caseId = String.format("case-%03d", index);
            LabResponse vulnerable = get(VULNERABLE, "/api/v1/s11/research/" + caseId + "?variant=a",
                    index != 15, index == 15);
            TestSupport.assertNotContains(vulnerable.body(), "POSITIVE",
                    "live positive fixture does not leak research label");
            assertions++;
            TestSupport.assertNotContains(vulnerable.body(), "expected_candidate",
                    "live positive fixture does not leak ground-truth field");
            assertions++;
        }

        return assertions;
    }

    private static boolean semanticEquivalent(LabResponse left, LabResponse right) {
        return COMPARATOR.compare(toDomain(left), toDomain(right), ResponseComparisonMode.SEMANTIC).equivalent();
    }

    private static AuthorizationOutcome outcome(LabResponse response) {
        HttpResponse domain = toDomain(response);
        return OUTCOME.normalize(domain, SEMANTIC.fingerprint(domain));
    }

    private static HttpResponse toDomain(LabResponse response) {
        return new HttpResponse(
                response.status(),
                List.of(),
                response.body().getBytes(StandardCharsets.UTF_8),
                "application/json",
                HttpProtocol.HTTP_1_1,
                new byte[0]);
    }

    private static LabResponse get(
            int port,
            String target,
            boolean bearer,
            boolean customPrincipal) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + target))
                .timeout(Duration.ofSeconds(2))
                .GET()
                .header("Accept", "application/json");
        if (bearer) request.header("Authorization", "Bearer " + TOKEN);
        if (customPrincipal) request.header("X-S11-Principal", "user-a");
        var response = CLIENT.send(request.build(), java.net.http.HttpResponse.BodyHandlers.ofString());
        return new LabResponse(response.statusCode(), response.body());
    }

    private static String token(String sub, String tenant, String role) {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String header = encoder.encodeToString(
                "{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = encoder.encodeToString(
                ("{\"sub\":\"" + sub + "\",\"tenant_id\":\"" + tenant
                        + "\",\"role\":\"" + role + "\"}").getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".s11synthetic";
    }

    private record LabResponse(int status, String body) { }
}
