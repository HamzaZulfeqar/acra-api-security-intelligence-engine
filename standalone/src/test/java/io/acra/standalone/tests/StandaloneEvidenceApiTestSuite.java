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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StandaloneEvidenceApiTestSuite {
    private static final Pattern EVIDENCE_ID = Pattern.compile("\"evidenceId\":\"([^\"]+)\"");
    private static final Pattern SAMPLE_ID = Pattern.compile("\"sampleId\":\"([^\"]+)\"");
    private static int assertions;

    private StandaloneEvidenceApiTestSuite() {}

    public static void main(String[] args) throws Exception {
        Path temp = Files.createTempDirectory("acra-s10-evidence-api-");
        try {
            LocalWorkspaceStore workspace = new LocalWorkspaceStore(temp);
            var project = workspace.createProject("Evidence API", "Phase 5");
            var target = workspace.addTarget(
                    project.id(),
                    "Authorized API",
                    "https://api.example.test/api/v1",
                    "STAGING",
                    "AUTH-P5-API",
                    "IMPORT_ONLY"
            );

            String allowHar = """
                    {
                      "log":{"entries":[{
                        "request":{
                          "method":"GET",
                          "url":"https://api.example.test/api/v1/users/42",
                          "headers":[{"name":"Authorization","value":"Bearer ultra-secret-token"}]
                        },
                        "response":{
                          "status":200,
                          "content":{"mimeType":"application/json","text":"{\\\"id\\\":42}"}
                        }
                      }]}
                    }
                    """;
            String denyHar = """
                    {
                      "log":{"entries":[{
                        "request":{"method":"GET","url":"https://api.example.test/api/v1/users/42"},
                        "response":{
                          "status":403,
                          "content":{"mimeType":"application/json","text":"{\\\"error\\\":\\\"forbidden\\\"}"}
                        }
                      }]}
                    }
                    """;
            StandaloneImportService imports = new StandaloneImportService(workspace);
            imports.importText(project.id(), target.id(), "HAR", "allow.har", allowHar);
            imports.importText(project.id(), target.id(), "HAR", "deny.har", denyHar);

            SecurityContextService contexts = new SecurityContextService(workspace);
            contexts.addPrincipal(project.id(), "user-a", "User A", "BEARER");
            contexts.addPrincipal(project.id(), "user-b", "User B", "BEARER");
            contexts.addRole(project.id(), "standard-user", "Standard User");
            contexts.addTenant(project.id(), "tenant-a", "Tenant A");
            contexts.addResource(project.id(), "user-42", "user", "user-a", "tenant-a", "ACTIVE");
            var leftContext = contexts.addExpectation(
                    project.id(), target.id(), "/api/v1/users/{user_id}", "READ",
                    "user-a", "standard-user", "tenant-a", "user-42", "ALLOW", "owner");
            var rightContext = contexts.addExpectation(
                    project.id(), target.id(), "/api/v1/users/{user_id}", "READ",
                    "user-b", "standard-user", "tenant-a", "user-42", "DENY", "non-owner");

            try (StandaloneServer server = new StandaloneServer(workspace, 0)) {
                server.start();
                HttpClient client = HttpClient.newHttpClient();

                HttpResponse<String> list = client.send(
                        HttpRequest.newBuilder(server.baseUri().resolve(
                                "api/evidence?projectId=" + project.id())).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
                check(list.statusCode() == 200, "evidence list returns 200");
                check(!list.body().contains("ultra-secret-token"), "list never exposes bearer secret");

                Matcher evidenceMatcher = EVIDENCE_ID.matcher(list.body());
                check(evidenceMatcher.find(), "evidence id returned");
                String firstEvidenceId = evidenceMatcher.group(1);

                Matcher sampleMatcher = SAMPLE_ID.matcher(list.body());
                check(sampleMatcher.find(), "first sample id returned");
                String firstSampleId = sampleMatcher.group(1);
                check(sampleMatcher.find(), "second sample id returned");
                String secondSampleId = sampleMatcher.group(1);

                HttpResponse<String> detail = client.send(
                        HttpRequest.newBuilder(server.baseUri().resolve(
                                "api/evidence?projectId=" + project.id()
                                        + "&evidenceId=" + firstEvidenceId)).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
                check(detail.statusCode() == 200, "evidence detail returns 200");
                check(!detail.body().contains("ultra-secret-token"), "detail never exposes bearer secret");
                check(detail.body().contains("<redacted>"), "detail exposes redacted representation");

                HttpResponse<String> httpDiff = client.send(
                        HttpRequest.newBuilder(server.baseUri().resolve(
                                "api/differential?projectId=" + project.id()
                                        + "&type=HTTP&leftId=" + firstSampleId
                                        + "&rightId=" + secondSampleId
                                        + "&mode=NORMALIZED")).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
                check(httpDiff.statusCode() == 200, "HTTP differential returns 200");
                check(httpDiff.body().contains("\"equivalent\":false"), "HTTP differential detects change");
                check(httpDiff.body().contains("\"status\""), "HTTP status change returned");

                HttpResponse<String> authDiff = client.send(
                        HttpRequest.newBuilder(server.baseUri().resolve(
                                "api/differential?projectId=" + project.id()
                                        + "&type=AUTHORIZATION&leftId=" + leftContext.id()
                                        + "&rightId=" + rightContext.id())).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
                check(authDiff.statusCode() == 200, "authorization differential returns 200");
                check(authDiff.body().contains("\"principal\""), "principal change returned");
                check(authDiff.body().contains("\"expectedDecision\""), "decision change returned");
            }

            System.out.println("SPRINT10_STANDALONE_EVIDENCE_API PASS assertions=" + assertions);
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
