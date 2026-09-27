package io.acra.standalone.tests;

import io.acra.standalone.service.StandaloneImportService;
import io.acra.standalone.store.LocalWorkspaceStore;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public final class StandaloneImportInventoryTestSuite {
    private static int assertions;

    private StandaloneImportInventoryTestSuite() {}

    public static void main(String[] args) throws Exception {
        Path temp = Files.createTempDirectory("acra-s10-import-");
        try {
            LocalWorkspaceStore store = new LocalWorkspaceStore(temp);
            var project = store.createProject("Import Review", "Phase 2");
            var target = store.addTarget(
                    project.id(),
                    "Authorized API",
                    "https://api.example.test/api/v1",
                    "STAGING",
                    "AUTH-2026-001",
                    "IMPORT_ONLY"
            );
            StandaloneImportService service = new StandaloneImportService(store);

            String openApi = """
                    {
                      "openapi":"3.0.3",
                      "info":{"title":"Authorized API","version":"1"},
                      "paths":{
                        "/users/{userId}":{
                          "get":{"operationId":"getUser","responses":{"200":{"description":"ok"}}}
                        }
                      }
                    }
                    """;
            var openApiSummary = service.importText(
                    project.id(), target.id(), "OPENAPI", "authorized-api.json", openApi);
            check(openApiSummary.observations() == 1, "OpenAPI observation imported");
            check(openApiSummary.inventorySize() == 1, "OpenAPI inventory created");

            String har = """
                    {
                      "log":{
                        "entries":[
                          {
                            "request":{"method":"GET","url":"https://api.example.test/api/v1/users/42"},
                            "response":{"status":200}
                          }
                        ]
                      }
                    }
                    """;
            var harSummary = service.importText(project.id(), target.id(), "HAR", "capture.har", har);
            check(harSummary.observations() == 1, "HAR observation imported");
            check(harSummary.inventorySize() == 1, "HAR correlated with declared route");

            var inventory = store.listInventory(project.id());
            check(inventory.size() == 1, "single deduplicated endpoint");
            var userEndpoint = inventory.getFirst();
            check(userEndpoint.canonicalPath().equals("/api/v1/users/{user_id}"), "canonical user route");
            check(userEndpoint.documented(), "documented state preserved");
            check(userEndpoint.observationCount() == 2, "observation count merged");
            check(userEndpoint.sourceTypes().contains("OPENAPI") && userEndpoint.sourceTypes().contains("HAR"),
                    "source provenance merged");
            check(userEndpoint.responseStatuses().contains(200), "HAR response status retained");

            String raw = "GET /api/v1/orders/99 HTTP/1.1\r\nHost: api.example.test\r\n\r\n";
            var rawSummary = service.importText(project.id(), target.id(), "RAW_HTTP", "request.txt", raw);
            check(rawSummary.inventorySize() == 2, "raw HTTP endpoint added");
            check(store.listInventory(project.id()).stream()
                    .anyMatch(record -> record.canonicalPath().equals("/api/v1/orders/{order_id}")),
                    "raw HTTP canonical resource route");

            String outOfScopeHar = """
                    {"log":{"entries":[{"request":{"method":"GET","url":"https://outside.example.test/api/v1/users/42"},"response":{"status":200}}]}}
                    """;
            expectFailure(() -> service.importText(
                    project.id(), target.id(), "HAR", "outside.har", outOfScopeHar),
                    "out-of-scope HAR rejected");

            LocalWorkspaceStore reopened = new LocalWorkspaceStore(temp);
            check(reopened.listInventory(project.id()).size() == 2, "inventory persists across reopen");

            System.out.println("SPRINT10_STANDALONE_IMPORT_INVENTORY PASS assertions=" + assertions);
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
