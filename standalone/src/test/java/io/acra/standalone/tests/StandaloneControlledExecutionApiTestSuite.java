package io.acra.standalone.tests;

import com.sun.net.httpserver.HttpServer;
import io.acra.standalone.http.StandaloneServer;
import io.acra.standalone.service.SecurityContextService;
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
import java.util.concurrent.atomic.AtomicInteger;

public final class StandaloneControlledExecutionApiTestSuite {
    private static int assertions;

    private StandaloneControlledExecutionApiTestSuite() {}

    public static void main(String[] args) throws Exception {
        Path temp = Files.createTempDirectory("acra-s10-active-api-");
        AtomicInteger requests = new AtomicInteger();

        HttpServer fixture = HttpServer.create(
                new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        fixture.createContext("/api/v1/demo", exchange -> {
            requests.incrementAndGet();
            String authorization = exchange.getRequestHeaders().getFirst("Authorization");
            boolean testedContext = "Bearer viewer-api-secret".equals(authorization);
            boolean positiveContext = "Bearer admin-api-secret".equals(authorization);
            boolean variant = exchange.getRequestURI().getPath().equals("/api/v1/demo/");
            int status = positiveContext || (variant && testedContext) ? 200 : 403;
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
            int targetPort = fixture.getAddress().getPort();
            LocalWorkspaceStore workspace = new LocalWorkspaceStore(temp);
            var project = workspace.createProject("Active API", "Phase 7 API");
            var target = workspace.addTarget(
                    project.id(),
                    "Controlled Lab",
                    "http://127.0.0.1:" + targetPort + "/api/v1",
                    "LAB",
                    "AUTH-P7-API",
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

            try (StandaloneServer server = new StandaloneServer(workspace, 0)) {
                server.start();
                HttpClient client = HttpClient.newHttpClient();
                URI activeUri = server.baseUri().resolve("api/active");
                String testedSecret = "Bearer viewer-api-secret";
                String positiveSecret = "Bearer admin-api-secret";

                HttpResponse<String> execution = post(
                        client,
                        activeUri,
                        server.csrfToken(),
                        "action=EXECUTE_ROUTE_EQUIVALENCE"
                                + "&projectId=" + enc(project.id().toString())
                                + "&targetId=" + enc(target.id().toString())
                                + "&expectationId=" + enc(expectation.id().toString())
                                + "&concretePath=" + enc("/api/v1/demo")
                                + "&testedAuthorizationValue=" + enc(testedSecret)
                                + "&positiveControlAuthorizationValue=" + enc(positiveSecret)
                                + "&confirmed=true");

                check(execution.statusCode() == 201, "active execution API returns 201");
                check(execution.body().contains("\"observedDecision\":\"ALLOW\""),
                        "active API exposes observed ALLOW");
                check(execution.body().contains("\"differentialClassification\":\"UNEXPECTED_CHANGE\""),
                        "active API exposes unexpected differential");
                check(!execution.body().contains(testedSecret) && !execution.body().contains(positiveSecret),
                        "active API never returns credentials");
                check(requests.get() == 4, "active API uses four controlled variants");

                HttpResponse<String> listing = client.send(
                        HttpRequest.newBuilder(server.baseUri().resolve(
                                "api/active?projectId=" + enc(project.id().toString()))).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
                check(listing.statusCode() == 200, "active list API returns 200");
                check(listing.body().contains("\"killSwitchEngaged\":false"), "kill switch initially clear");
                check(listing.body().contains("\"executions\":["), "execution history returned");
                check(!listing.body().contains(testedSecret) && !listing.body().contains(positiveSecret),
                        "execution history contains no credentials");

                HttpResponse<String> kill = post(
                        client,
                        activeUri,
                        server.csrfToken(),
                        "action=KILL&reason=" + enc("operator stop"));
                check(kill.statusCode() == 200 && kill.body().contains("\"killSwitchEngaged\":true"),
                        "kill action engages switch");

                int beforeBlocked = requests.get();
                HttpResponse<String> blocked = post(
                        client,
                        activeUri,
                        server.csrfToken(),
                        "action=EXECUTE_ROUTE_EQUIVALENCE"
                                + "&projectId=" + enc(project.id().toString())
                                + "&targetId=" + enc(target.id().toString())
                                + "&expectationId=" + enc(expectation.id().toString())
                                + "&concretePath=" + enc("/api/v1/demo")
                                + "&testedAuthorizationValue=" + enc(testedSecret)
                                + "&positiveControlAuthorizationValue=" + enc(positiveSecret)
                                + "&confirmed=true");
                check(blocked.statusCode() == 409, "kill-switched execution returns conflict");
                check(requests.get() == beforeBlocked, "kill-switched API execution sends no request");

                HttpResponse<String> reset = post(
                        client,
                        activeUri,
                        server.csrfToken(),
                        "action=RESET_KILL&confirmed=true&reason=" + enc("operator reset"));
                check(reset.statusCode() == 200 && reset.body().contains("\"killSwitchEngaged\":false"),
                        "explicit reset clears kill switch");

                HttpResponse<String> noCsrf = client.send(
                        HttpRequest.newBuilder(activeUri)
                                .header("Content-Type", "application/x-www-form-urlencoded")
                                .POST(HttpRequest.BodyPublishers.ofString("action=KILL"))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
                check(noCsrf.statusCode() == 403, "active mutation API requires CSRF");
            }

            System.out.println("SPRINT10_STANDALONE_CONTROLLED_EXECUTION_API PASS assertions=" + assertions);
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

    private static String enc(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
}
