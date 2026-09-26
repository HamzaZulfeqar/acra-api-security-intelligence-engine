package io.acra.standalone.tests;

import io.acra.standalone.http.StandaloneServer;
import io.acra.standalone.store.LocalWorkspaceStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StandaloneSecurityContextApiTestSuite {
    private static int assertions;
    private static final Pattern ID_PATTERN = Pattern.compile("\"id\":\"([^\"]+)\"");

    private StandaloneSecurityContextApiTestSuite() {}

    public static void main(String[] args) throws Exception {
        Path temp = Files.createTempDirectory("acra-s10-context-api-");
        try (StandaloneServer server = new StandaloneServer(new LocalWorkspaceStore(temp), 0)) {
            server.start();
            HttpClient client = HttpClient.newHttpClient();
            URI base = server.baseUri();

            String projectJson = post(client, base.resolve("api/projects"), server.csrfToken(),
                    "name=Context+API&description=Phase+3");
            String projectId = extractId(projectJson);
            check(!projectId.isBlank(), "project created");

            String targetJson = post(client, base.resolve("api/targets"), server.csrfToken(),
                    "projectId="+enc(projectId)
                            +"&displayName=Authorized+API"
                            +"&baseUrl="+enc("https://api.example.test/api/v1")
                            +"&environment=STAGING"
                            +"&authorizationReference=AUTH-CTX-API"
                            +"&testingMode=IMPORT_ONLY");
            String targetId = extractId(targetJson);
            check(!targetId.isBlank(), "target created");

            String openApi = """
                    {"openapi":"3.0.3","info":{"title":"API","version":"1"},"paths":{
                      "/users/{userId}":{"get":{"responses":{"200":{"description":"ok"}}}}
                    }}
                    """;
            post(client, base.resolve("api/import"), server.csrfToken(),
                    "projectId="+enc(projectId)
                            +"&targetId="+enc(targetId)
                            +"&importType=OPENAPI"
                            +"&sourceReference=api.json"
                            +"&content="+enc(openApi));

            post(client, base.resolve("api/context"), server.csrfToken(),
                    "projectId="+enc(projectId)+"&kind=PRINCIPAL&principalId=user-a&displayName=User+A&authenticationType=BEARER");
            post(client, base.resolve("api/context"), server.csrfToken(),
                    "projectId="+enc(projectId)+"&kind=ROLE&roleId=standard-user&name=Standard+User");
            post(client, base.resolve("api/context"), server.csrfToken(),
                    "projectId="+enc(projectId)+"&kind=TENANT&tenantId=tenant-a&name=Tenant+A");
            post(client, base.resolve("api/context"), server.csrfToken(),
                    "projectId="+enc(projectId)+"&kind=RESOURCE&resourceId=user-42&resourceType=user&ownerPrincipalId=user-a&tenantId=tenant-a&state=ACTIVE");
            post(client, base.resolve("api/context"), server.csrfToken(),
                    "projectId="+enc(projectId)
                            +"&kind=EXPECTATION"
                            +"&targetId="+enc(targetId)
                            +"&endpoint="+enc("/api/v1/users/{user_id}")
                            +"&action=READ"
                            +"&principalId=user-a"
                            +"&roleId=standard-user"
                            +"&tenantId=tenant-a"
                            +"&resourceId=user-42"
                            +"&expectedDecision=ALLOW"
                            +"&rationale="+enc("Owner read policy"));

            HttpResponse<String> context = client.send(
                    HttpRequest.newBuilder(base.resolve("api/context?projectId="+enc(projectId))).GET().build(),
                    HttpResponse.BodyHandlers.ofString());

            check(context.statusCode() == 200, "context GET succeeds");
            check(context.body().contains("\"principalId\":\"user-a\""), "principal returned");
            check(context.body().contains("\"roleId\":\"standard-user\""), "role returned");
            check(context.body().contains("\"tenantId\":\"tenant-a\""), "tenant returned");
            check(context.body().contains("\"resourceId\":\"user-42\""), "resource returned");
            check(context.body().contains("\"expectedDecision\":\"ALLOW\""), "expectation returned");

            HttpResponse<String> badReference = postResponse(
                    client,
                    base.resolve("api/context"),
                    server.csrfToken(),
                    "projectId="+enc(projectId)
                            +"&kind=EXPECTATION"
                            +"&targetId="+enc(targetId)
                            +"&endpoint="+enc("/api/v1/users/{user_id}")
                            +"&action=READ"
                            +"&principalId=missing"
                            +"&expectedDecision=DENY");
            check(badReference.statusCode() == 400, "unknown principal fails closed");

            System.out.println("SPRINT10_STANDALONE_SECURITY_CONTEXT_API PASS assertions=" + assertions);
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

    private static String post(HttpClient client, URI uri, String csrf, String body) throws Exception {
        HttpResponse<String> response = postResponse(client, uri, csrf, body);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new AssertionError("POST failed: " + response.statusCode() + " " + response.body());
        }
        return response.body();
    }

    private static HttpResponse<String> postResponse(HttpClient client, URI uri, String csrf, String body) throws Exception {
        return client.send(
                HttpRequest.newBuilder(uri)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .header("X-ACRA-CSRF", csrf)
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static String extractId(String json) {
        Matcher matcher = ID_PATTERN.matcher(json);
        if (!matcher.find()) throw new AssertionError("id missing from response: " + json);
        return matcher.group(1);
    }

    private static String enc(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
}
