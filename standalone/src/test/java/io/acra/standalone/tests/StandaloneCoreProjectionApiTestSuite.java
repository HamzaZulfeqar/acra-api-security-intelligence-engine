package io.acra.standalone.tests;

import io.acra.standalone.http.StandaloneServer;
import io.acra.standalone.service.SecurityContextService;
import io.acra.standalone.service.StandaloneImportService;
import io.acra.standalone.store.LocalWorkspaceStore;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public final class StandaloneCoreProjectionApiTestSuite {
    private static int assertions;

    private StandaloneCoreProjectionApiTestSuite() {}

    public static void main(String[] args) throws Exception {
        Path temp = Files.createTempDirectory("acra-s10-projection-api-");
        try {
            LocalWorkspaceStore workspace = new LocalWorkspaceStore(temp);
            var project = workspace.createProject("Projection API", "Phase 4");
            var target = workspace.addTarget(
                    project.id(),
                    "Authorized API",
                    "https://api.example.test/api/v1",
                    "STAGING",
                    "AUTH-P4-API",
                    "IMPORT_ONLY"
            );

            String spec = """
                    {
                      "openapi":"3.0.3",
                      "info":{"title":"Authorized API","version":"1"},
                      "paths":{
                        "/users/{userId}":{
                          "get":{"responses":{"200":{"description":"ok"}}}
                        }
                      }
                    }
                    """;
            new StandaloneImportService(workspace).importText(
                    project.id(), target.id(), "OPENAPI", "api.json", spec);

            SecurityContextService contexts = new SecurityContextService(workspace);
            contexts.addPrincipal(project.id(), "user-a", "User A", "BEARER");
            contexts.addRole(project.id(), "standard-user", "Standard User");
            contexts.addTenant(project.id(), "tenant-a", "Tenant A");
            contexts.addResource(project.id(), "user-42", "user", "user-a", "tenant-a", "ACTIVE");
            contexts.addExpectation(
                    project.id(), target.id(), "/api/v1/users/{user_id}", "READ",
                    "user-a", "standard-user", "tenant-a", "user-42",
                    "ALLOW", "Owner read");

            try (StandaloneServer server = new StandaloneServer(workspace, 0)) {
                server.start();
                HttpClient client = HttpClient.newHttpClient();
                HttpResponse<String> response = client.send(
                        HttpRequest.newBuilder(server.baseUri().resolve(
                                "api/projection?projectId=" + project.id())).GET().build(),
                        HttpResponse.BodyHandlers.ofString());

                check(response.statusCode() == 200, "projection API returns 200");
                check(response.body().contains("\"resolvedDecision\":\"ALLOW\""), "resolved allow exposed");
                check(response.body().contains("\"resolutionState\":\"RESOLVED_ALLOW\""), "resolution state exposed");
                check(response.body().contains("\"bolaStatus\":\"INCONCLUSIVE\""), "BOLA evidence boundary exposed");
                check(response.body().contains("\"bflaStatus\":\"INCONCLUSIVE\""), "BFLA evidence boundary exposed");
                check(response.body().contains("\"workflowWorkspaceProjected\":true"), "workflow workspace projection exposed");
                check(response.body().contains("\"routingWorkspaceProjected\":true"), "routing workspace projection exposed");
                check(response.body().contains("\"propertyWorkspaceProjected\":true"), "property workspace projection exposed");
            }

            System.out.println("SPRINT10_STANDALONE_CORE_PROJECTION_API PASS assertions=" + assertions);
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

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
}
