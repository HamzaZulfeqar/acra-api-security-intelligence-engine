package io.acra.standalone.tests;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.standalone.model.CandidateReviewState;
import io.acra.standalone.model.StandaloneCoverageDisposition;
import io.acra.standalone.service.SecurityContextService;
import io.acra.standalone.service.StandaloneImportService;
import io.acra.standalone.service.StandaloneReviewReportingService;
import io.acra.standalone.store.LocalWorkspaceStore;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public final class StandaloneReviewReportingTestSuite {
    private static int assertions;

    private StandaloneReviewReportingTestSuite() {}

    public static void main(String[] args) throws Exception {
        Path temp = Files.createTempDirectory("acra-s10-review-report-");
        try {
            LocalWorkspaceStore workspace = new LocalWorkspaceStore(temp);
            var project = workspace.createProject("Review Report", "Phase 6");
            var target = workspace.addTarget(
                    project.id(), "Authorized API", "https://api.example.test/api/v1",
                    "STAGING", "AUTH-P6-001", "IMPORT_ONLY");

            String spec = """
                    {
                      "openapi":"3.0.3",
                      "info":{"title":"API","version":"1"},
                      "paths":{
                        "/users/{userId}":{"get":{"responses":{"200":{"description":"ok"}}}},
                        "/admin":{"get":{"responses":{"200":{"description":"ok"}}}}
                      }
                    }
                    """;
            StandaloneImportService imports = new StandaloneImportService(workspace);
            imports.importText(project.id(), target.id(), "OPENAPI", "api.json", spec);

            String har = """
                    {"log":{"entries":[{
                      "request":{"method":"GET","url":"https://api.example.test/api/v1/users/42"},
                      "response":{"status":200,"content":{"mimeType":"application/json","text":"{\\\"id\\\":42}"}}
                    }]}}
                    """;
            imports.importText(project.id(), target.id(), "HAR", "capture.har", har);

            SecurityContextService contexts = new SecurityContextService(workspace);
            contexts.addPrincipal(project.id(), "user-a", "User A", "BEARER");
            contexts.addPrincipal(project.id(), "user-b", "User B", "BEARER");
            contexts.addRole(project.id(), "standard-user", "Standard User");
            contexts.addTenant(project.id(), "tenant-a", "Tenant A");
            contexts.addResource(project.id(), "user-42", "user", "user-a", "tenant-a", "ACTIVE");
            contexts.addExpectation(
                    project.id(), target.id(), "/api/v1/users/{user_id}", "READ",
                    "user-a", "standard-user", "tenant-a", "user-42", "ALLOW", "owner");
            contexts.addExpectation(
                    project.id(), target.id(), "/api/v1/users/{user_id}", "READ",
                    "user-b", "standard-user", "tenant-a", "user-42", "DENY", "non-owner");

            StandaloneReviewReportingService service = new StandaloneReviewReportingService(workspace);
            var candidates = service.candidates(project.id());
            check(candidates.size() == 2, "two review candidates projected");
            check(candidates.stream().allMatch(r -> r.candidate().state() == FindingCandidateState.INCONCLUSIVE),
                    "candidates remain inconclusive");
            check(candidates.stream().allMatch(r -> r.candidate().observedDecision() == AuthorizationDecision.UNKNOWN),
                    "observed decisions remain unknown");
            check(candidates.stream().allMatch(r -> r.review().state() == CandidateReviewState.NEEDS_MORE_EVIDENCE),
                    "default review state requires evidence");

            String candidateId = candidates.getFirst().candidate().candidateId();
            service.updateReview(project.id(), candidateId, "UNDER_REVIEW", "Analyst is checking context");
            check(service.candidates(project.id()).stream()
                    .filter(r -> r.candidate().candidateId().equals(candidateId))
                    .findFirst().orElseThrow().review().state() == CandidateReviewState.UNDER_REVIEW,
                    "review update persists");

            expectFailure(() -> service.updateReview(
                    project.id(), candidateId, "UNDER_REVIEW", "Bearer secret-review-token"),
                    "secret-bearing review note rejected");

            var coverage = service.coverage(project.id());
            check(coverage.size() == 2, "two endpoint coverage rows");
            check(coverage.stream().anyMatch(r -> r.endpoint().equals("/api/v1/users/{user_id}")
                    && r.disposition() == StandaloneCoverageDisposition.INCONCLUSIVE),
                    "context plus passive evidence remains inconclusive");
            check(coverage.stream().anyMatch(r -> r.endpoint().equals("/api/v1/admin")
                    && r.disposition() == StandaloneCoverageDisposition.UNTESTED),
                    "endpoint without context is untested");

            var json1 = service.report(project.id(), "JSON");
            var json2 = service.report(project.id(), "JSON");
            check(json1.content().equals(json2.content()), "JSON report deterministic");
            check(json1.sha256().equals(json2.sha256()), "JSON digest deterministic");
            check(json1.content().contains("\"confirmedFindingCount\":0"), "confirmed finding count fixed at zero");
            check(json1.content().contains("NO_AUTOMATIC_CONFIRMED_FINDINGS"), "report preserves limitation");

            var md1 = service.report(project.id(), "MARKDOWN");
            var md2 = service.report(project.id(), "MARKDOWN");
            check(md1.content().equals(md2.content()), "Markdown report deterministic");
            check(md1.sha256().equals(md2.sha256()), "Markdown digest deterministic");

            StandaloneReviewReportingService reopened =
                    new StandaloneReviewReportingService(new LocalWorkspaceStore(temp));
            check(reopened.candidates(project.id()).stream()
                    .filter(r -> r.candidate().candidateId().equals(candidateId))
                    .findFirst().orElseThrow().review().state() == CandidateReviewState.UNDER_REVIEW,
                    "review survives restart");

            System.out.println("SPRINT10_STANDALONE_REVIEW_REPORTING PASS assertions=" + assertions);
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
