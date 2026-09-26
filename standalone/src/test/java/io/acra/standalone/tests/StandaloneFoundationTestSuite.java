package io.acra.standalone.tests;

import io.acra.standalone.http.StandaloneServer;
import io.acra.standalone.model.ProjectRecord;
import io.acra.standalone.model.TargetRecord;
import io.acra.standalone.store.LocalWorkspaceStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public final class StandaloneFoundationTestSuite {
    private static int assertions;

    private StandaloneFoundationTestSuite() {}

    public static void main(String[] args) throws Exception {
        Path temp = Files.createTempDirectory("acra-standalone-test-");
        try {
            persistenceAndValidation(temp);
            liveHttpContract(temp.resolve("live"));
            System.out.println("SPRINT10_STANDALONE_FOUNDATION PASS assertions=" + assertions);
        } finally {
            if (Files.exists(temp)) {
                try (var walk = Files.walk(temp)) {
                    for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
                }
            }
        }
    }

    private static void persistenceAndValidation(Path root) throws Exception {
        LocalWorkspaceStore store = new LocalWorkspaceStore(root);
        ProjectRecord project = store.createProject("Authorized API Review", "Standalone mode");
        check(store.listProjects().size() == 1, "project persists");

        TargetRecord target = store.addTarget(
                project.id(),
                "Local API",
                "http://127.0.0.1:8081/api/",
                "LAB",
                "LAB-AUTH-001",
                "PASSIVE");
        check(target.baseUri().getHost().equals("127.0.0.1"), "IP target accepted");
        check(store.listTargets(project.id()).size() == 1, "target persists");

        LocalWorkspaceStore reopened = new LocalWorkspaceStore(root);
        check(reopened.listProjects().getFirst().id().equals(project.id()), "project reopens");
        check(reopened.listTargets(project.id()).getFirst().id().equals(target.id()), "target reopens");

        expectFailure(() -> store.addTarget(project.id(), "FTP", "ftp://127.0.0.1/file",
                "LAB", "AUTH", "PASSIVE"), "non-http target rejected");
        expectFailure(() -> store.addTarget(project.id(), "Credential URL", "https://user:pass@example.test/api",
                "LAB", "AUTH", "PASSIVE"), "embedded credentials rejected");
        expectFailure(() -> store.addTarget(project.id(), "No auth", "https://example.test/api",
                "STAGING", "", "PASSIVE"), "authorization reference required");
    }

    private static void liveHttpContract(Path root) throws Exception {
        LocalWorkspaceStore store = new LocalWorkspaceStore(root);
        try (StandaloneServer server = new StandaloneServer(store, 0)) {
            server.start();
            HttpClient client = HttpClient.newHttpClient();
            URI base = server.baseUri();

            HttpResponse<String> health = client.send(
                    HttpRequest.newBuilder(base.resolve("api/health"))
                            .header("Host", "127.0.0.1:" + server.port())
                            .GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            check(health.statusCode() == 200, "health endpoint");
            check(health.body().contains("\"burpRequired\":false"), "Burp not required");
            check(health.body().contains("\"coreLinked\":true"), "core link exposed");

            String projectBody = "name=Local+Assessment&description=Standalone+test";
            HttpResponse<String> created = client.send(
                    HttpRequest.newBuilder(base.resolve("api/projects"))
                            .header("Host", "127.0.0.1:" + server.port())
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .header("X-ACRA-CSRF", server.csrfToken())
                            .POST(HttpRequest.BodyPublishers.ofString(projectBody))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            check(created.statusCode() == 201, "project API create");
            check(created.body().contains("Local Assessment"), "project API response");

            HttpResponse<String> denied = client.send(
                    HttpRequest.newBuilder(base.resolve("api/projects"))
                            .header("Host", "127.0.0.1:" + server.port())
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .POST(HttpRequest.BodyPublishers.ofString(projectBody))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            check(denied.statusCode() == 403, "CSRF enforced");
        }
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }

    private static void expectFailure(ThrowingRunnable runnable, String message) throws Exception {
        assertions++;
        try {
            runnable.run();
            throw new AssertionError(message);
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
