package io.acra.core.tests.sprint4;

import io.acra.core.active.analysis.ExpectedDecisionCandidate;
import io.acra.core.active.analysis.ExpectedDecisionSource;
import io.acra.core.active.model.ConfigurationSnapshot;
import io.acra.core.active.model.ExecutionEnvironment;
import io.acra.core.active.model.Mutation;
import io.acra.core.active.model.MutationLocation;
import io.acra.core.active.model.MutationType;
import io.acra.core.active.model.RequestDefinition;
import io.acra.core.active.model.ReproducibilityMetadata;
import io.acra.core.active.model.SafetyClass;
import io.acra.core.active.model.SafetyPolicy;
import io.acra.core.active.model.SecurityTest;
import io.acra.core.active.model.TargetDescriptor;
import io.acra.core.active.model.TestContract;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.common.Confidence;
import io.acra.core.domain.endpoint.Endpoint;
import io.acra.core.domain.http.HttpHeader;
import io.acra.core.domain.http.HttpMethod;
import io.acra.core.domain.http.HttpProtocol;
import io.acra.core.domain.http.HttpRequest;
import io.acra.core.domain.resource.Resource;
import io.acra.core.recon.SecurityContextFingerprint;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class Sprint4LocalhostFixtures {
    static final String PROJECT = "acra-s4";
    static final String SECURE_TARGET = "acra-lab-secure-s4";
    static final String VULNERABLE_TARGET = "acra-lab-vulnerable-s4";
    static final int SECURE_PORT = 18082;
    static final int VULNERABLE_PORT = 18081;
    static final String DOCUMENT_A = "Document-A";
    static final String DOCUMENT_B = "Document-B";
    static final Instant CREATED_AT = Instant.parse("2026-08-31T18:45:00Z");
    static final String USER_A_TOKEN = token("user-a", "tenant-a", "viewer");
    static final String USER_B_TOKEN = token("user-b", "tenant-b", "editor");

    private Sprint4LocalhostFixtures() {}

    static TargetDescriptor target(boolean secure) {
        return new TargetDescriptor(PROJECT, secure ? SECURE_TARGET : VULNERABLE_TARGET,
                "http", "localhost", secure ? SECURE_PORT : VULNERABLE_PORT,
                ExecutionEnvironment.LAB, true, List.of("/api/v1"), Set.of(HttpMethod.GET, HttpMethod.POST));
    }

    static Endpoint endpoint(boolean secure) {
        return new Endpoint(secure ? "EP-S4-EXEC-SECURE" : "EP-S4-EXEC-VULNERABLE", HttpMethod.GET,
                "/api/v1/s4/documents/Document-A", "/api/v1/s4/documents/{id}", "/api/v1/s4/documents/{id}",
                "localhost", "v1", List.of("S4 localhost direct-resource fixture"));
    }

    static HttpRequest request(int port, String documentId, String token, String principalLabel) {
        return HttpRequest.of(HttpMethod.GET, "http", "localhost", port, "/api/v1/s4/documents/" + documentId,
                List.of(new HttpHeader("Authorization", "Bearer " + token),
                        new HttpHeader("X-S4-Principal", principalLabel),
                        new HttpHeader("Accept", "application/json")),
                new byte[0], HttpProtocol.HTTP_1_1);
    }

    static HttpRequest simpleRequest(int port, HttpMethod method, String target, String token,
                                     List<HttpHeader> extraHeaders, byte[] body) {
        java.util.ArrayList<HttpHeader> headers = new java.util.ArrayList<>();
        headers.add(new HttpHeader("Authorization", "Bearer " + token));
        headers.add(new HttpHeader("Accept", "application/json"));
        if (extraHeaders != null) headers.addAll(extraHeaders);
        return HttpRequest.of(method, "http", "localhost", port, target, headers, body, HttpProtocol.HTTP_1_1);
    }

    static SecurityTest resourceExperiment(boolean secure) {
        int port = secure ? SECURE_PORT : VULNERABLE_PORT;
        RequestDefinition baseline = new RequestDefinition("REQ-S4-BASE-A-A",
                request(port, DOCUMENT_A, USER_A_TOKEN, "User-A"), "ctx-user-a-doc-a", "document:Document-A");
        RequestDefinition positive = new RequestDefinition("REQ-S4-POS-A-A",
                request(port, DOCUMENT_A, USER_A_TOKEN, "User-A"), "ctx-user-a-doc-a", "document:Document-A");
        RequestDefinition negative = secure
                ? new RequestDefinition("REQ-S4-NEG-A-B",
                        request(port, DOCUMENT_B, USER_A_TOKEN, "User-A"), "ctx-user-a-doc-b", "document:Document-B")
                : new RequestDefinition("REQ-S4-NEG-B-A",
                        request(port, DOCUMENT_A, USER_B_TOKEN, "User-B"), "ctx-user-b-doc-a", "document:Document-A");
        Mutation mutation = new Mutation("MUT-S4-RESOURCE-A-B", MutationType.RESOURCE_SUBSTITUTION,
                MutationLocation.PATH, DOCUMENT_A, DOCUMENT_B, "ctx-user-a-doc-a", "ctx-user-a-doc-b",
                "replace exactly one document reference while preserving principal and operation",
                "independent ground truth expects DENY", SafetyClass.SAFE_READ_ONLY,
                (secure ? "secure" : "vulnerable") + ":document-a-to-b");
        return new SecurityTest(secure ? "S4-EXEC-SECURE-001" : "S4-EXEC-VULN-001", "1",
                TestContract.CROSS_RESOURCE, HttpProtocol.HTTP_1_1, target(secure), endpoint(secure), HttpMethod.GET,
                baseline, positive, negative, mutation, sourceContext(), targetContext(), sourceResource(), targetResource(),
                AuthorizationDecision.DENY, List.of("GT-EXEC-S4#GT-S4-DENY-A-B"),
                SafetyPolicy.safeLabReadOnly(12, 4), 90,
                "independent ACRA-Lab resource ownership policy is available",
                ConfigurationSnapshot.of(Map.of(
                        "dataset", "S4-EXEC-V1",
                        "environment", "LAB",
                        "labMode", secure ? "secure" : "vulnerable",
                        "target", "localhost:" + port)),
                List.of(), new ReproducibilityMetadata("0.4.0-rc1", 40401L, CREATED_AT,
                        List.of("GT-EXEC-S4", secure ? "secure" : "vulnerable")), 4,
                List.of("principal remains User-A for mutation", "method remains GET",
                        "only resource reference changes Document-A -> Document-B", "ground truth is independent of ACRA output"));
    }

    static SecurityTest timeoutTest() {
        TargetDescriptor target = new TargetDescriptor(PROJECT, SECURE_TARGET, "http", "localhost", SECURE_PORT,
                ExecutionEnvironment.LAB, true, List.of("/api/v1"), Set.of(HttpMethod.GET));
        Endpoint endpoint = new Endpoint("EP-S4-TIMEOUT", HttpMethod.GET, "/api/v1/s4/slow",
                "/api/v1/s4/slow", "/api/v1/s4/slow", "localhost", "v1", List.of("operational timeout fixture"));
        HttpRequest base = simpleRequest(SECURE_PORT, HttpMethod.GET, "/api/v1/s4/slow?ms=250", USER_A_TOKEN, List.of(), new byte[0]);
        RequestDefinition baseline = new RequestDefinition("REQ-TIMEOUT-BASE", base, "ctx-timeout", "operational:slow");
        RequestDefinition positive = new RequestDefinition("REQ-TIMEOUT-POS", base, "ctx-timeout", "operational:slow");
        RequestDefinition negative = new RequestDefinition("REQ-TIMEOUT-NEG", base, "ctx-timeout", "operational:slow");
        Mutation mutation = new Mutation("MUT-TIMEOUT", MutationType.PROPERTY, MutationLocation.QUERY,
                "250", "300", "ctx-timeout", "ctx-timeout", "controlled delay variation for timeout mapping",
                "operational only", SafetyClass.SAFE_READ_ONLY, "timeout-250-300");
        return new SecurityTest("S4-EXEC-TIMEOUT-001", "1", TestContract.PROPERTY, HttpProtocol.HTTP_1_1,
                target, endpoint, HttpMethod.GET, baseline, positive, negative, mutation,
                sourceContext(), sourceContext(), sourceResource(), sourceResource(), AuthorizationDecision.UNKNOWN,
                List.of("GT-EXEC-S4#operational-timeout"), SafetyPolicy.safeLabReadOnly(4, 1), 1,
                "verify operational timeout remains separate from security interpretation",
                ConfigurationSnapshot.of(Map.of("dataset", "S4-EXEC-V1", "environment", "LAB", "target", "localhost:18082")),
                List.of(), new ReproducibilityMetadata("0.4.0-rc1", 40402L, CREATED_AT,
                        List.of("GT-EXEC-S4", "operational-timeout")), 4,
                List.of("timeout is operational", "no security finding"));
    }

    static ExpectedDecisionCandidate groundTruthDeny() {
        return new ExpectedDecisionCandidate(AuthorizationDecision.DENY, ExpectedDecisionSource.ACRA_LAB_GROUND_TRUTH,
                "GT-EXEC-S4#GT-S4-DENY-A-B", List.of("GT-EXEC-S4"), 1.0);
    }

    static SecurityContextFingerprint sourceContext() {
        return new SecurityContextFingerprint("User-A", "viewer", "Tenant-A", "document:Document-A", "User-A",
                "READ", "ACTIVE", "GET /api/v1/s4/documents/{id}", "raw", "s4-user-a",
                AuthorizationDecision.ALLOW, AuthorizationDecision.UNKNOWN, List.of("GT-S4-ALLOW-A-A"));
    }

    static SecurityContextFingerprint targetContext() {
        return new SecurityContextFingerprint("User-A", "viewer", "Tenant-A", "document:Document-B", "User-B",
                "READ", "ACTIVE", "GET /api/v1/s4/documents/{id}", "raw", "s4-user-a",
                AuthorizationDecision.DENY, AuthorizationDecision.UNKNOWN, List.of("GT-S4-DENY-A-B"));
    }

    static Resource sourceResource() {
        return new Resource(DOCUMENT_A, "document", null, "User-A", "Tenant-A", "draft", Confidence.unknown());
    }

    static Resource targetResource() {
        return new Resource(DOCUMENT_B, "document", null, "User-B", "Tenant-B", "draft", Confidence.unknown());
    }

    private static String token(String sub, String tenant, String role) {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String header = encoder.encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = encoder.encodeToString(("{\"sub\":\"" + sub + "\",\"tenant_id\":\"" + tenant
                + "\",\"role\":\"" + role + "\"}").getBytes(StandardCharsets.UTF_8));
        return header + '.' + payload + ".s4synthetic";
    }
}
