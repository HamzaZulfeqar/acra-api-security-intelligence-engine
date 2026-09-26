package io.acra.standalone.tests;

import io.acra.core.active.analysis.AuthorizationOutcome;
import io.acra.core.active.analysis.DifferentialClassification;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.testing.TestState;
import io.acra.standalone.http.StandaloneServer;
import io.acra.standalone.model.ControlledExecutionRecord;
import io.acra.standalone.service.SecurityContextService;
import io.acra.standalone.service.StandaloneEvidenceService;
import io.acra.standalone.service.StandaloneFindingLifecycleService;
import io.acra.standalone.service.StandaloneImportService;
import io.acra.standalone.store.ControlledExecutionStore;
import io.acra.standalone.store.LocalWorkspaceStore;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class StandaloneFindingProductSurfaceTestSuite {
    private static int assertions;

    private StandaloneFindingProductSurfaceTestSuite() {}

    public static void main(String[] args) throws Exception {
        Path temp = Files.createTempDirectory("acra-s11-finding-product-");
        try {
            LocalWorkspaceStore workspace = new LocalWorkspaceStore(temp);
            var project = workspace.createProject("Sprint 11 Product", "Finding reproduction UI");
            var target = workspace.addTarget(
                    project.id(),
                    "Controlled Lab",
                    "http://127.0.0.1:18921/api/v1",
                    "LAB",
                    "AUTH-S11-PRODUCT",
                    "CONTROLLED_LAB");

            String spec = """
                    {
                      "openapi":"3.0.3",
                      "info":{"title":"Controlled Lab","version":"1"},
                      "paths":{"/demo":{"get":{"responses":{"200":{"description":"ok"},"403":{"description":"denied"}}}}}
                    }
                    """;
            new StandaloneImportService(workspace).importText(
                    project.id(), target.id(), "OPENAPI", "lab.json", spec);

            SecurityContextService contexts = new SecurityContextService(workspace);
            contexts.addPrincipal(project.id(), "viewer-a", "Viewer A", "BEARER");
            contexts.addRole(project.id(), "viewer", "Viewer");
            contexts.addTenant(project.id(), "tenant-a", "Tenant A");
            contexts.addResource(project.id(), "demo-resource", "demo", "viewer-a", "tenant-a", "ACTIVE");
            var expectation = contexts.addExpectation(
                    project.id(), target.id(), "/api/v1/demo", "READ",
                    "viewer-a", "viewer", "tenant-a", "demo-resource",
                    "DENY", "Equivalent route must remain denied");

            StandaloneEvidenceService evidence = new StandaloneEvidenceService(workspace);
            var activeEvidence = evidence.captureExecutionSummary(
                    project.id(),
                    target.id(),
                    "controlled-route-equivalence:product",
                    "testId=S11-PRODUCT\nexpected=DENY\nobserved=ALLOW\ndifferential=UNEXPECTED_CHANGE");

            UUID runId = UUID.randomUUID();
            new ControlledExecutionStore(workspace).save(new ControlledExecutionRecord(
                    runId,
                    project.id(),
                    target.id(),
                    expectation.id(),
                    "S11-PRODUCT-TEST",
                    "S11-PRODUCT-EXEC",
                    "S11-PRODUCT-OBS",
                    "/api/v1/demo",
                    "/api/v1/demo",
                    "/api/v1/demo/",
                    AuthorizationDecision.DENY,
                    AuthorizationOutcome.ALLOW,
                    DifferentialClassification.UNEXPECTED_CHANGE,
                    TestState.COMPLETED,
                    activeEvidence.evidenceId(),
                    12,
                    Instant.parse("2026-09-26T14:00:00Z")));

            StandaloneFindingLifecycleService lifecycle = new StandaloneFindingLifecycleService(workspace);
            var opened = lifecycle.openFromExecution(project.id(), runId);
            var reviewEvidence = evidence.captureExecutionSummary(
                    project.id(), target.id(), "review-product", "Independent validation artifact");
            var validated = lifecycle.transition(
                    project.id(),
                    opened.finding().findingId(),
                    "VALIDATED",
                    "reviewer-product-hidden",
                    "Product review reason must not be exported",
                    List.of(reviewEvidence.evidenceId()));

            try (StandaloneServer server = new StandaloneServer(workspace, 0)) {
                server.start();
                HttpClient client = HttpClient.newHttpClient();
                String base = server.baseUri().toString();
                String common = "projectId=" + enc(project.id().toString())
                        + "&findingId=" + enc(validated.finding().findingId())
                        + "&format=";

                HttpResponse<String> json = get(client, URI.create(base + "api/finding-reproduction?" + common + "JSON"));
                check(json.statusCode() == 200, "JSON reproduction API returns 200");
                check(json.body().contains("\"format\":\"JSON\""), "JSON export envelope identifies format");
                check(json.body().contains("\"sha256\":"), "JSON export exposes SHA-256");
                check(json.body().contains("\"confirmedFinding\":false"), "validated finding is not confirmed");
                check(!json.body().contains("reviewer-product-hidden")
                                && !json.body().contains("Product review reason must not be exported"),
                        "JSON export excludes reviewer identity and reason");

                HttpResponse<String> sarif = get(client, URI.create(base + "api/finding-reproduction?" + common + "SARIF"));
                check(sarif.statusCode() == 200, "SARIF reproduction API returns 200");
                check(sarif.body().contains("\"format\":\"SARIF\""), "SARIF export envelope identifies format");
                check(sarif.body().contains("\\\"version\\\":\\\"2.1.0\\\""),
                        "SARIF content retains version 2.1.0");
                check(!sarif.body().contains("reviewer-product-hidden"), "SARIF excludes reviewer identity");

                HttpResponse<String> burp = get(client, URI.create(base + "api/finding-reproduction?" + common + "BURP_DRAFT"));
                check(burp.statusCode() == 200, "Burp draft API returns 200");
                check(burp.body().contains("\"format\":\"BURP_DRAFT\""), "Burp draft identifies format");
                check(burp.body().contains("\"publicationEligible\":false"),
                        "validated finding Burp draft is not publication eligible");
                check(burp.body().contains("does not publish"), "Burp draft states non-publication boundary");

                HttpResponse<String> page = get(client, server.baseUri());
                HttpResponse<String> script = get(client, server.baseUri().resolve("app.js"));
                check(page.statusCode() == 200 && page.body().contains("REPRODUCTION & EXPORTS"),
                        "packaged Findings page exposes reproduction surface");
                check(page.body().contains("No automatic publication"),
                        "packaged UI states publication boundary");
                check(script.body().contains("/api/finding-reproduction"),
                        "browser JavaScript is wired to reproduction API");
                check(script.body().contains("View JSON")
                                && script.body().contains("View SARIF")
                                && script.body().contains("View Burp Draft"),
                        "browser offers all three read-only artifact views");
                check(!script.body().contains("PUBLISH_BURP"),
                        "browser has no automatic Burp publication action");
            }

            System.out.println("SPRINT11_STANDALONE_FINDING_PRODUCT_SURFACE PASS assertions=" + assertions);
        } finally {
            if (Files.exists(temp)) {
                try (var walk = Files.walk(temp)) {
                    for (Path pathToDelete : walk.sorted(Comparator.reverseOrder()).toList()) {
                        Files.deleteIfExists(pathToDelete);
                    }
                }
            }
        }
    }

    private static HttpResponse<String> get(HttpClient client, URI uri) throws Exception {
        return client.send(
                HttpRequest.newBuilder(uri).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
}
