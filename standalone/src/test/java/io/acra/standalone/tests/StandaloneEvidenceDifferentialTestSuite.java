package io.acra.standalone.tests;

import io.acra.standalone.service.SecurityContextService;
import io.acra.standalone.service.StandaloneEvidenceService;
import io.acra.standalone.service.StandaloneImportService;
import io.acra.standalone.store.LocalWorkspaceStore;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public final class StandaloneEvidenceDifferentialTestSuite {
    private static int assertions;

    private StandaloneEvidenceDifferentialTestSuite() {}

    public static void main(String[] args) throws Exception {
        Path temp = Files.createTempDirectory("acra-s10-evidence-");
        try {
            LocalWorkspaceStore workspace = new LocalWorkspaceStore(temp);
            var project = workspace.createProject("Evidence Review", "Phase 5");
            var target = workspace.addTarget(
                    project.id(),
                    "Authorized API",
                    "https://api.example.test/api/v1",
                    "STAGING",
                    "AUTH-P5-001",
                    "IMPORT_ONLY"
            );

            StandaloneImportService imports = new StandaloneImportService(workspace);
            String harAllow = """
                    {
                      "log":{"entries":[{
                        "request":{
                          "method":"GET",
                          "url":"https://api.example.test/api/v1/users/42",
                          "headers":[{"name":"Authorization","value":"Bearer secret-token-value"}]
                        },
                        "response":{
                          "status":200,
                          "content":{"mimeType":"application/json","text":"{\\\"id\\\":42,\\\"name\\\":\\\"A\\\"}"}
                        }
                      }]}
                    }
                    """;
            String harDeny = """
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

            imports.importText(project.id(), target.id(), "HAR", "allow.har", harAllow);
            imports.importText(project.id(), target.id(), "HAR", "deny.har", harDeny);

            StandaloneEvidenceService evidence = new StandaloneEvidenceService(workspace);
            var artifacts = evidence.artifacts(project.id());
            var samples = evidence.samples(project.id());

            check(artifacts.size() == 2, "two import artifacts archived");
            check(samples.size() == 2, "two HTTP samples archived");
            check(artifacts.getFirst().redactionApplied(), "bearer secret redaction detected");
            String stored = evidence.redactedContent(project.id(), artifacts.getFirst().evidenceId());
            check(!stored.contains("secret-token-value"), "raw bearer token not persisted");
            check(stored.contains("<redacted>"), "redaction marker persisted");
            check(!artifacts.getFirst().originalSha256().isBlank(), "original digest retained");

            var diff = evidence.compareHttp(
                    project.id(),
                    samples.getFirst().sampleId(),
                    samples.getLast().sampleId(),
                    "NORMALIZED");
            check(!diff.equivalent(), "different responses not equivalent");
            check(diff.changedSignals().contains("status"), "status differential detected");
            check(diff.leftStatus() != diff.rightStatus(), "status values retained");

            SecurityContextService contexts = new SecurityContextService(workspace);
            contexts.addPrincipal(project.id(), "user-a", "User A", "BEARER");
            contexts.addPrincipal(project.id(), "user-b", "User B", "BEARER");
            contexts.addRole(project.id(), "standard-user", "Standard User");
            contexts.addTenant(project.id(), "tenant-a", "Tenant A");
            contexts.addResource(project.id(), "user-42", "user", "user-a", "tenant-a", "ACTIVE");
            var allow = contexts.addExpectation(
                    project.id(), target.id(), "/api/v1/users/{user_id}", "READ",
                    "user-a", "standard-user", "tenant-a", "user-42", "ALLOW", "owner");
            var deny = contexts.addExpectation(
                    project.id(), target.id(), "/api/v1/users/{user_id}", "READ",
                    "user-b", "standard-user", "tenant-a", "user-42", "DENY", "non-owner");

            var contextDiff = evidence.compareAuthorization(project.id(), allow.id(), deny.id());
            check(!contextDiff.equivalent(), "authorization contexts differ");
            check(contextDiff.changedFields().contains("principal"), "principal difference detected");
            check(contextDiff.changedFields().contains("expectedDecision"), "decision difference detected");

            int artifactCount = evidence.artifacts(project.id()).size();
            String outside = """
                    {"log":{"entries":[{"request":{"method":"GET","url":"https://outside.example.test/api/v1/users/42"},"response":{"status":200}}]}}
                    """;
            expectFailure(() -> imports.importText(
                    project.id(), target.id(), "HAR", "outside.har", outside),
                    "out-of-scope import rejected");
            check(evidence.artifacts(project.id()).size() == artifactCount,
                    "out-of-scope content was not archived");

            StandaloneEvidenceService reopened = new StandaloneEvidenceService(new LocalWorkspaceStore(temp));
            check(reopened.artifacts(project.id()).size() == 2, "evidence survives restart");
            check(reopened.samples(project.id()).size() == 2, "samples survive restart");

            System.out.println("SPRINT10_STANDALONE_EVIDENCE_DIFFERENTIAL PASS assertions=" + assertions);
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
