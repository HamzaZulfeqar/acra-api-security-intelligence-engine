package io.acra.standalone.tests;

import io.acra.standalone.http.StandaloneServer;
import io.acra.standalone.service.SecurityContextService;
import io.acra.standalone.service.StandaloneImportService;
import io.acra.standalone.store.LocalWorkspaceStore;

import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StandaloneReviewReportingApiTestSuite {
    private static final Pattern CANDIDATE_ID = Pattern.compile("\"candidateId\":\"([^\"]+)\"");
    private static final Pattern SHA = Pattern.compile("\"sha256\":\"([^\"]+)\"");
    private static int assertions;

    private StandaloneReviewReportingApiTestSuite() {}

    public static void main(String[] args) throws Exception {
        Path temp = Files.createTempDirectory("acra-s10-review-api-");
        try {
            LocalWorkspaceStore workspace = new LocalWorkspaceStore(temp);
            var project = workspace.createProject("Review API", "Phase 6");
            var target = workspace.addTarget(
                    project.id(), "Authorized API", "https://api.example.test/api/v1",
                    "STAGING", "AUTH-P6-API", "IMPORT_ONLY");

            String spec = """
                    {"openapi":"3.0.3","info":{"title":"API","version":"1"},"paths":{
                      "/users/{userId}":{"get":{"responses":{"200":{"description":"ok"}}}},
                      "/admin":{"get":{"responses":{"200":{"description":"ok"}}}}
                    }}
                    """;
            StandaloneImportService imports = new StandaloneImportService(workspace);
            imports.importText(project.id(), target.id(), "OPENAPI", "api.json", spec);

            String har = """
                    {"log":{"entries":[{
                      "request":{"method":"GET","url":"https://api.example.test/api/v1/users/42"},
                      "response":{"status":403,"content":{"mimeType":"application/json","text":"{\\\"error\\\":\\\"forbidden\\\"}"}}
                    }]}}
                    """;
            imports.importText(project.id(), target.id(), "HAR", "capture.har", har);

            SecurityContextService contexts = new SecurityContextService(workspace);
            contexts.addPrincipal(project.id(), "user-b", "User B", "BEARER");
            contexts.addRole(project.id(), "standard-user", "Standard User");
            contexts.addTenant(project.id(), "tenant-a", "Tenant A");
            contexts.addResource(project.id(), "user-42", "user", "", "tenant-a", "ACTIVE");
            contexts.addExpectation(
                    project.id(), target.id(), "/api/v1/users/{user_id}", "READ",
                    "user-b", "standard-user", "tenant-a", "user-42", "DENY", "non-owner");

            try (StandaloneServer server = new StandaloneServer(workspace, 0)) {
                server.start();
                HttpClient client = HttpClient.newHttpClient();

                HttpResponse<String> candidates = get(client,
                        server.baseUri().resolve("api/candidates?projectId=" + project.id()));
                check(candidates.statusCode() == 200, "candidates returns 200");
                check(candidates.body().contains("\"coreState\":\"INCONCLUSIVE\""),
                        "Core candidate remains inconclusive");
                check(candidates.body().contains("\"reviewState\":\"NEEDS_MORE_EVIDENCE\""),
                        "default review state requires evidence");

                Matcher candidateMatcher = CANDIDATE_ID.matcher(candidates.body());
                check(candidateMatcher.find(), "candidate id returned");
                String candidateId = candidateMatcher.group(1);

                HttpResponse<String> review = post(
                        client,
                        server.baseUri().resolve("api/candidates"),
                        server.csrfToken(),
                        "projectId=" + enc(project.id().toString())
                                + "&candidateId=" + enc(candidateId)
                                + "&state=UNDER_REVIEW"
                                + "&note=" + enc("Analyst review only"));
                check(review.statusCode() == 200, "review update returns 200");

                HttpResponse<String> candidatesAfter = get(client,
                        server.baseUri().resolve("api/candidates?projectId=" + project.id()));
                check(candidatesAfter.body().contains("\"coreState\":\"INCONCLUSIVE\""),
                        "review does not alter Core candidate state");
                check(candidatesAfter.body().contains("\"reviewState\":\"UNDER_REVIEW\""),
                        "review state persisted");

                HttpResponse<String> coverage = get(client,
                        server.baseUri().resolve("api/coverage?projectId=" + project.id()));
                check(coverage.statusCode() == 200, "coverage returns 200");
                check(!coverage.body().contains("\"disposition\":\"TESTED\""),
                        "coverage does not claim tested without active execution");
                check(coverage.body().contains("\"disposition\":\"INCONCLUSIVE\""),
                        "passive/context endpoint is inconclusive");
                check(coverage.body().contains("\"disposition\":\"UNTESTED\""),
                        "unconfigured endpoint remains untested");

                HttpResponse<String> report1 = get(client,
                        server.baseUri().resolve("api/report?projectId=" + project.id() + "&format=JSON"));
                HttpResponse<String> report2 = get(client,
                        server.baseUri().resolve("api/report?projectId=" + project.id() + "&format=JSON"));
                check(report1.statusCode() == 200 && report2.statusCode() == 200, "report endpoint returns 200");
                check(report1.body().contains("\\"confirmedFindingCount\\":0"),
                        "report content keeps confirmed findings at zero");
                Matcher sha1 = SHA.matcher(report1.body());
                Matcher sha2 = SHA.matcher(report2.body());
                check(sha1.find() && sha2.find() && sha1.group(1).equals(sha2.group(1)),
                        "unchanged report SHA is deterministic");

                HttpResponse<String> markdown = get(client,
                        server.baseUri().resolve("api/report?projectId=" + project.id() + "&format=MARKDOWN"));
                check(markdown.statusCode() == 200, "Markdown report returns 200");
                check(markdown.body().contains("Confirmed findings: **0**"),
                        "Markdown report keeps zero confirmed findings");
            }

            System.out.println("SPRINT10_STANDALONE_REVIEW_REPORTING_API PASS assertions=" + assertions);
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

    private static HttpResponse<String> get(HttpClient client, java.net.URI uri) throws Exception {
        return client.send(HttpRequest.newBuilder(uri).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> post(HttpClient client, java.net.URI uri, String csrf, String body) throws Exception {
        return client.send(
                HttpRequest.newBuilder(uri)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .header("X-ACRA-CSRF", csrf)
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
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
