package io.acra.standalone.tests;

import io.acra.standalone.service.SecurityContextService;
import io.acra.standalone.service.StandaloneImportService;
import io.acra.standalone.store.LocalWorkspaceStore;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public final class StandaloneSecurityContextTestSuite {
    private static int assertions;

    private StandaloneSecurityContextTestSuite() {}

    public static void main(String[] args) throws Exception {
        Path temp = Files.createTempDirectory("acra-s10-context-");
        try {
            LocalWorkspaceStore workspace = new LocalWorkspaceStore(temp);
            var project = workspace.createProject("Context Review", "Phase 3");
            var target = workspace.addTarget(
                    project.id(),
                    "Authorized API",
                    "https://api.example.test/api/v1",
                    "STAGING",
                    "AUTH-CONTEXT-001",
                    "IMPORT_ONLY"
            );

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
            new StandaloneImportService(workspace).importText(
                    project.id(), target.id(), "OPENAPI", "authorized-api.json", openApi);

            SecurityContextService service = new SecurityContextService(workspace);
            service.addPrincipal(project.id(), "user-a", "User A", "BEARER");
            service.addPrincipal(project.id(), "user-b", "User B", "BEARER");
            service.addRole(project.id(), "standard-user", "Standard User");
            service.addTenant(project.id(), "tenant-a", "Tenant A");
            service.addResource(project.id(), "user-42", "user", "user-a", "tenant-a", "ACTIVE");

            var expectation = service.addExpectation(
                    project.id(),
                    target.id(),
                    "/api/v1/users/{user_id}",
                    "READ",
                    "user-b",
                    "standard-user",
                    "tenant-a",
                    "user-42",
                    "DENY",
                    "Non-owner should not read this resource"
            );

            check(expectation.expectedDecision().name().equals("DENY"), "expected DENY retained");
            check(service.principals(project.id()).size() == 2, "principals persisted");
            check(service.roles(project.id()).size() == 1, "role persisted");
            check(service.tenants(project.id()).size() == 1, "tenant persisted");
            check(service.resources(project.id()).getFirst().ownerPrincipalId().equals("user-a"), "resource owner persisted");
            check(service.expectations(project.id()).size() == 1, "expectation persisted");

            SecurityContextService reopened = new SecurityContextService(new LocalWorkspaceStore(temp));
            check(reopened.principals(project.id()).size() == 2, "principals survive reopen");
            check(reopened.expectations(project.id()).getFirst().resourceId().equals("user-42"),
                    "expectation survives reopen");

            expectFailure(() -> service.addPrincipal(project.id(), "user-a", "Duplicate", "BEARER"),
                    "duplicate principal rejected");

            expectFailure(() -> service.addResource(
                    project.id(), "user-99", "user", "missing-user", "tenant-a", "ACTIVE"),
                    "unknown owner rejected");

            expectFailure(() -> service.addExpectation(
                    project.id(), target.id(), "/api/v1/users/{user_id}", "READ",
                    "user-a", "missing-role", "tenant-a", "user-42", "ALLOW", ""),
                    "unknown role rejected");

            expectFailure(() -> service.addExpectation(
                    project.id(), target.id(), "/api/v1/admin", "READ",
                    "user-a", "standard-user", "tenant-a", "user-42", "DENY", ""),
                    "unknown endpoint rejected");

            expectFailure(() -> service.addExpectation(
                    project.id(), target.id(), "/api/v1/users/{user_id}", "READ",
                    "user-a", "standard-user", "tenant-a", "user-42", "ALLOW",
                    "Authorization: Bearer abc.def.ghi"),
                    "secret-bearing rationale rejected");

            System.out.println("SPRINT10_STANDALONE_SECURITY_CONTEXT PASS assertions=" + assertions);
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
