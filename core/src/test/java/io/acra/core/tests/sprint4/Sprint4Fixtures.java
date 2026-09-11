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
import io.acra.core.active.model.SelectionMode;
import io.acra.core.active.model.TargetDescriptor;
import io.acra.core.active.model.TestContract;
import io.acra.core.active.model.TestProfile;
import io.acra.core.active.model.TestProfileDefinition;
import io.acra.core.active.planning.PlanningInput;
import io.acra.core.active.planning.TestSeed;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.AuthorizationMatrix;
import io.acra.core.domain.common.Confidence;
import io.acra.core.domain.endpoint.ApiEndpointRecord;
import io.acra.core.domain.endpoint.DocumentationStatus;
import io.acra.core.domain.endpoint.Endpoint;
import io.acra.core.domain.endpoint.RiskTier;
import io.acra.core.domain.http.HttpHeader;
import io.acra.core.domain.http.HttpMethod;
import io.acra.core.domain.http.HttpProtocol;
import io.acra.core.domain.http.HttpRequest;
import io.acra.core.domain.http.HttpResponse;
import io.acra.core.domain.identity.AuthenticationType;
import io.acra.core.domain.resource.Resource;
import io.acra.core.graph.SecurityContextGraph;
import io.acra.core.recon.SecurityContextFingerprint;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class Sprint4Fixtures {
    static final Instant NOW = Instant.parse("2026-08-31T18:30:00Z");
    static final String PROJECT = "acra-s4";
    static final String TARGET = "acra-lab-local";
    static final String ENDPOINT_ID = "EP-S4-DOC-READ";

    private Sprint4Fixtures() {}

    static TargetDescriptor target() {
        return new TargetDescriptor(PROJECT, TARGET, "http", "localhost", 8080,
                ExecutionEnvironment.LAB, true, List.of("/api/v1"), Set.of(HttpMethod.GET));
    }

    static Endpoint endpoint() {
        return new Endpoint(ENDPOINT_ID, HttpMethod.GET,
                "/api/v1/documents/1001", "/api/v1/documents/{id}",
                "/api/v1/documents/{id}", "localhost", "v1", List.of("S4 synthetic fixture"));
    }

    static SecurityContextFingerprint sourceContext() {
        return new SecurityContextFingerprint("User-A", "viewer", "Tenant-A", "document:1001", "User-A",
                "READ", "ACTIVE", "GET /api/v1/documents/{id}", "raw", "ctx-a",
                AuthorizationDecision.ALLOW, AuthorizationDecision.UNKNOWN, List.of("GT-S4-ALLOW"));
    }

    static SecurityContextFingerprint targetContext() {
        return new SecurityContextFingerprint("User-B", "viewer", "Tenant-A", "document:1001", "User-A",
                "READ", "ACTIVE", "GET /api/v1/documents/{id}", "raw", "ctx-b",
                AuthorizationDecision.DENY, AuthorizationDecision.UNKNOWN, List.of("GT-S4-DENY"));
    }

    static Resource resource() {
        return new Resource("1001", "document", null, "User-A", "Tenant-A", "ACTIVE", Confidence.unknown());
    }

    static HttpRequest requestFor(String principal, String bearer) {
        return HttpRequest.of(HttpMethod.GET, "http", "localhost", 8080, "/api/v1/documents/1001",
                List.of(new HttpHeader("Authorization", "Bearer " + bearer),
                        new HttpHeader("X-Principal", principal),
                        new HttpHeader("X-Tenant", "Tenant-A"),
                        new HttpHeader("Accept", "application/json")),
                new byte[0], HttpProtocol.HTTP_1_1);
    }

    static RequestDefinition baseline() {
        return new RequestDefinition("REQ-BASE", requestFor("User-A", "synthetic-user-a-token"), "ctx-a", "document:1001");
    }

    static RequestDefinition positive() {
        return new RequestDefinition("REQ-POS", requestFor("User-A", "synthetic-user-a-token"), "ctx-a", "document:1001");
    }

    static RequestDefinition negative() {
        return new RequestDefinition("REQ-NEG", requestFor("User-B", "synthetic-user-b-token"), "ctx-b", "document:1001");
    }

    static Mutation mutation(String deduplicationKey) {
        return new Mutation("MUT-PRINCIPAL", MutationType.IDENTITY_SUBSTITUTION, MutationLocation.IDENTITY,
                "User-A", "User-B", "ctx-a", "ctx-b", "single synthetic principal substitution",
                "expected denial from independent lab ground truth", SafetyClass.SAFE_READ_ONLY, deduplicationKey);
    }

    static SafetyPolicy safetyPolicy() {
        return SafetyPolicy.safeLabReadOnly(20, 10);
    }

    static ConfigurationSnapshot configuration() {
        return ConfigurationSnapshot.of(Map.of(
                "environment", "LAB",
                "target", "localhost:8080",
                "profile", "SAFE_LAB",
                "dataset", "S4-SYNTHETIC-V1"));
    }

    static ReproducibilityMetadata reproducibility() {
        return new ReproducibilityMetadata("0.4.0-rc1", 404L, NOW, List.of("S4-SYNTHETIC-V1", "GT-S4"));
    }

    static SecurityTest test(String testId, String deduplicationKey) {
        return test(testId, deduplicationKey, mutation(deduplicationKey));
    }

    static SecurityTest test(String testId, String deduplicationKey, Mutation testMutation) {
        return new SecurityTest(testId, "1", TestContract.CROSS_USER, HttpProtocol.HTTP_1_1,
                target(), endpoint(), HttpMethod.GET, baseline(), positive(), negative(), testMutation,
                sourceContext(), targetContext(), resource(), resource(), AuthorizationDecision.DENY,
                List.of("GT-S4-DENY"), safetyPolicy(), 80,
                "synthetic resource and independent deny ground truth are available", configuration(), List.of(),
                reproducibility(), 4,
                List.of("single intended mutation", "resource remains constant", "tenant remains constant", "operation remains READ"));
    }

    static TestSeed seed(String testId, String deduplicationKey) {
        return new TestSeed(testId, "1", TestContract.CROSS_USER, endpoint(), baseline(), positive(), negative(),
                mutation(deduplicationKey), sourceContext(), targetContext(), resource(), resource(),
                AuthorizationDecision.DENY, List.of("GT-S4-DENY"), List.of(),
                List.of("single intended mutation", "resource remains constant"), 4,
                true, false, false, "synthetic cross-context differential");
    }

    static PlanningInput planningInput(List<TestSeed> seeds) {
        ApiEndpointRecord record = new ApiEndpointRecord(endpoint(), "v1", "LAB", AuthenticationType.BEARER,
                "ACRA-Lab", NOW, NOW, DocumentationStatus.DOCUMENTED, RiskTier.MEDIUM);
        TestProfileDefinition profile = TestProfileDefinition.defaults(TestProfile.SAFE_LAB);
        return new PlanningInput("PLAN-S4-VERIFY", List.of(record), new SecurityContextGraph(), new AuthorizationMatrix(),
                List.of(sourceContext(), targetContext()), List.of(), null, target(), Set.of(TestContract.CROSS_USER),
                Map.of(), Map.of(), 20, 10, safetyPolicy(), SelectionMode.ALL, profile, configuration(), seeds, NOW);
    }

    static HttpResponse response(int status, String body) {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        return new HttpResponse(status, List.of(new HttpHeader("Content-Type", "application/json")),
                bytes, "application/json", HttpProtocol.HTTP_1_1, new byte[0]);
    }

    static ExpectedDecisionCandidate groundTruthDeny() {
        return new ExpectedDecisionCandidate(AuthorizationDecision.DENY, ExpectedDecisionSource.ACRA_LAB_GROUND_TRUTH,
                "GT-S4-DENY", List.of("GT-S4-DENY"), 1.0);
    }
}
