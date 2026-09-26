package io.acra.standalone.tests;

import com.sun.net.httpserver.HttpServer;
import io.acra.standalone.http.StandaloneServer;
import io.acra.standalone.service.SecurityContextService;
import io.acra.standalone.service.StandaloneControlledExecutionService;
import io.acra.standalone.service.StandaloneEvidenceService;
import io.acra.standalone.service.StandaloneImportService;
import io.acra.standalone.store.LocalWorkspaceStore;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StandaloneFindingLifecycleApiTestSuite {
    private static final Pattern FINDING_ID = Pattern.compile("\"findingId\":\"([^\"]+)\"");
    private static int assertions;

    private StandaloneFindingLifecycleApiTestSuite() {}

    public static void main(String[] args) throws Exception {
        Path temp = Files.createTempDirectory("acra-s11-findings-api-");
        HttpServer fixture = HttpServer.create(
                new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);

        fixture.createContext("/api/v1/demo", exchange -> {
            String authorization = exchange.getRequestHeaders().getFirst("Authorization");
            boolean positive = "Bearer admin-s11-api-secret".equals(authorization);
            boolean viewer = "Bearer viewer-s11-api-secret".equals(authorization);
            boolean variant = exchange.getRequestURI().getPath().equals("/api/v1/demo/");
            int status = positive || (viewer && variant) ? 200 : 403;
            byte[] body = (status == 200 ? "{\"ok\":true}" : "{\"error\":\"denied\"}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, body.length);
            try (var out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        fixture.start();

        try {
            LocalWorkspaceStore workspace = new LocalWorkspaceStore(temp);
            var project = workspace.createProject("Sprint 11 API Review", "Finding lifecycle API");
            int targetPort = fixture.getAddress().getPort();
            var target = workspace.addTarget(
                    project.id(),
                    "Controlled Lab",
                    "http://127.0.0.1:" + targetPort + "/api/v1",
                    "LAB",
                    "AUTH-S11-API",
                    "CONTROLLED_LAB");

            String openApi = """
                    {
                      "openapi":"3.0.3",
                      "info":{"title":"Controlled Lab","version":"1"},
                      "paths":{
                        "/demo":{
                          "get":{"responses":{"200":{"description":"ok"},"403":{"description":"denied"}}}
                        }
                      }
                    }
                    """;
            new StandaloneImportService(workspace).importText(
                    project.id(), target.id(), "OPENAPI", "lab.json", openApi);

            SecurityContextService contexts = new SecurityContextService(workspace);
            contexts.addPrincipal(project.id(), "viewer-a", "Viewer A", "BEARER");
            contexts.addRole(project.id(), "viewer", "Viewer");
            contexts.addTenant(project.id(), "tenant-a", "Tenant A");
            contexts.addResource(project.id(), "demo-resource", "demo", "viewer-a", "tenant-a", "ACTIVE");
            var expectation = contexts.addExpectation(
                    project.id(), target.id(), "/api/v1/demo", "READ",
                    "viewer-a", "viewer", "tenant-a", "demo-resource",
                    "DENY", "Equivalent route must remain denied");

            var run = new StandaloneControlledExecutionService(workspace).executeRouteEquivalence(
                    project.id(),
                    target.id(),
                    expectation.id(),
                    "/api/v1/demo",
                    "Bearer viewer-s11-api-secret",
                    "Bearer admin-s11-api-secret",
                    true);

            StandaloneEvidenceService evidence = new StandaloneEvidenceService(workspace);
            var validationEvidence = evidence.captureExecutionSummary(
                    project.id(), target.id(), "review-validation-api", "Review validation evidence");
            var confirmationEvidence = evidence.captureExecutionSummary(
                    project.id(), target.id(), "review-confirmation-api", "Review confirmation evidence");

            try (StandaloneServer server = new StandaloneServer(workspace, 0)) {
                server.start();
                HttpClient client = HttpClient.newHttpClient();
                URI findingsUri = server.baseUri().resolve("api/findings");

                HttpResponse<String> initial = client.send(
                        HttpRequest.newBuilder(server.baseUri().resolve(
                                "api/findings?projectId=" + enc(project.id().toString())))
                                .GET().build(),
                        HttpResponse.BodyHandlers.ofString());
                check(initial.statusCode() == 200, "findings GET returns 200");
                check(initial.body().contains(run.runId().toString()),
                        "eligible controlled execution is returned");
                check(initial.body().contains("\"findings\":[]"),
                        "no finding is fabricated before explicit intake");
                check(!initial.body().contains("viewer-s11-api-secret")
                                && !initial.body().contains("admin-s11-api-secret"),
                        "findings GET exposes no active credentials");

                HttpResponse<String> noCsrf = client.send(
                        HttpRequest.newBuilder(findingsUri)
                                .header("Content-Type", "application/x-www-form-urlencoded")
                                .POST(HttpRequest.BodyPublishers.ofString(
                                        "action=OPEN&projectId=" + enc(project.id().toString())
                                                + "&runId=" + enc(run.runId().toString())))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
                check(noCsrf.statusCode() == 403, "finding mutation API requires CSRF");

                HttpResponse<String> opened = post(
                        client,
                        findingsUri,
                        server.csrfToken(),
                        "action=OPEN"
                                + "&projectId=" + enc(project.id().toString())
                                + "&runId=" + enc(run.runId().toString()));
                check(opened.statusCode() == 201, "explicit intake returns 201");
                check(opened.body().contains("\"state\":\"NEEDS_REVIEW\""),
                        "opened finding starts NEEDS_REVIEW");
                check(opened.body().contains("\"severity\":\"LOW\""),
                        "independent severity exposed");
                check(opened.body().contains("\"confidence\":\"HIGH\""),
                        "active-evidence confidence exposed");
                String findingId = extractFindingId(opened.body());

                HttpResponse<String> directConfirm = post(
                        client,
                        findingsUri,
                        server.csrfToken(),
                        "action=TRANSITION"
                                + "&projectId=" + enc(project.id().toString())
                                + "&findingId=" + enc(findingId)
                                + "&targetState=CONFIRMED"
                                + "&reviewerReference=reviewer-a"
                                + "&reason=" + enc("Attempt direct confirmation")
                                + "&evidenceId=" + enc(validationEvidence.evidenceId().toString()));
                check(directConfirm.statusCode() == 400,
                        "direct NEEDS_REVIEW to CONFIRMED fails closed");

                HttpResponse<String> validated = post(
                        client,
                        findingsUri,
                        server.csrfToken(),
                        "action=TRANSITION"
                                + "&projectId=" + enc(project.id().toString())
                                + "&findingId=" + enc(findingId)
                                + "&targetState=VALIDATED"
                                + "&reviewerReference=reviewer-a"
                                + "&reason=" + enc("Execution and context reviewed")
                                + "&evidenceId=" + enc(validationEvidence.evidenceId().toString()));
                check(validated.statusCode() == 200
                                && validated.body().contains("\"state\":\"VALIDATED\""),
                        "validated transition succeeds");

                HttpResponse<String> secretReason = post(
                        client,
                        findingsUri,
                        server.csrfToken(),
                        "action=TRANSITION"
                                + "&projectId=" + enc(project.id().toString())
                                + "&findingId=" + enc(findingId)
                                + "&targetState=CONFIRMED"
                                + "&reviewerReference=reviewer-b"
                                + "&reason=" + enc("Authorization: Bearer api-review-secret")
                                + "&evidenceId=" + enc(confirmationEvidence.evidenceId().toString()));
                check(secretReason.statusCode() == 400,
                        "secret-bearing review reason is rejected");

                HttpResponse<String> confirmed = post(
                        client,
                        findingsUri,
                        server.csrfToken(),
                        "action=TRANSITION"
                                + "&projectId=" + enc(project.id().toString())
                                + "&findingId=" + enc(findingId)
                                + "&targetState=CONFIRMED"
                                + "&reviewerReference=reviewer-b"
                                + "&reason=" + enc("Independent review confirms authorization violation")
                                + "&evidenceId=" + enc(confirmationEvidence.evidenceId().toString()));
                check(confirmed.statusCode() == 200
                                && confirmed.body().contains("\"state\":\"CONFIRMED\""),
                        "explicit confirmed transition succeeds");
                check(confirmed.body().contains("\"history\":["),
                        "review history is returned");
                check(!confirmed.body().contains("api-review-secret"),
                        "rejected secret does not leak into response");

                HttpResponse<String> listing = client.send(
                        HttpRequest.newBuilder(server.baseUri().resolve(
                                "api/findings?projectId=" + enc(project.id().toString())))
                                .GET().build(),
                        HttpResponse.BodyHandlers.ofString());
                check(listing.statusCode() == 200, "findings listing remains available");
                check(listing.body().contains("\"state\":\"CONFIRMED\""),
                        "confirmed finding persists through API");
                check(listing.body().contains(findingId),
                        "finding identity remains stable");

                HttpResponse<String> index = client.send(
                        HttpRequest.newBuilder(server.baseUri()).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
                check(index.statusCode() == 200, "packaged workbench index is served");
                check(index.body().contains("Reviewed Findings"),
                        "Reviewed Findings navigation/workspace is packaged");

                HttpResponse<String> appJs = client.send(
                        HttpRequest.newBuilder(server.baseUri().resolve("app.js")).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
                check(appJs.statusCode() == 200, "packaged browser JavaScript is served");
                check(appJs.body().contains("/api/findings")
                                && appJs.body().contains("openFindingFromExecution")
                                && appJs.body().contains("transitionReviewedFinding"),
                        "browser workflow is wired to reviewed-finding API");
                check(!appJs.body().contains("viewer-s11-api-secret")
                                && !appJs.body().contains("admin-s11-api-secret"),
                        "packaged browser resources contain no fixture credentials");

                System.out.println("SPRINT11_STANDALONE_FINDING_LIFECYCLE_API PASS assertions=" + assertions);
            }
        } finally {
            fixture.stop(0);
            if (Files.exists(temp)) {
                try (var walk = Files.walk(temp)) {
                    for (Path pathToDelete : walk.sorted(Comparator.reverseOrder()).toList()) {
                        Files.deleteIfExists(pathToDelete);
                    }
                }
            }
        }
    }

    private static HttpResponse<String> post(
            HttpClient client,
            URI uri,
            String csrf,
            String body
    ) throws Exception {
        return client.send(
                HttpRequest.newBuilder(uri)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .header("X-ACRA-CSRF", csrf)
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static String extractFindingId(String json) {
        Matcher matcher = FINDING_ID.matcher(json);
        if (!matcher.find()) throw new AssertionError("findingId missing from response: " + json);
        return matcher.group(1);
    }

    private static String enc(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
}
