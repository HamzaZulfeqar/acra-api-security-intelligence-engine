package io.acra.standalone.tests;

import com.sun.net.httpserver.HttpServer;
import io.acra.core.domain.finding.FindingConfidence;
import io.acra.core.domain.finding.FindingLifecycleState;
import io.acra.core.domain.finding.FindingSeverity;
import io.acra.standalone.service.SecurityContextService;
import io.acra.standalone.service.StandaloneControlledExecutionService;
import io.acra.standalone.service.StandaloneEvidenceService;
import io.acra.standalone.service.StandaloneFindingLifecycleService;
import io.acra.standalone.service.StandaloneImportService;
import io.acra.standalone.store.LocalWorkspaceStore;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class StandaloneFindingLifecycleTestSuite {
    private static int assertions;

    private StandaloneFindingLifecycleTestSuite() {}

    public static void main(String[] args) throws Exception {
        Path temp = Files.createTempDirectory("acra-s11-findings-");
        AtomicInteger requests = new AtomicInteger();

        HttpServer fixture = HttpServer.create(
                new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        fixture.createContext("/api/v1/demo", exchange -> {
            requests.incrementAndGet();
            String authorization = exchange.getRequestHeaders().getFirst("Authorization");
            boolean positiveControl = "Bearer admin-s11-secret".equals(authorization);
            boolean vulnerableViewer = "Bearer viewer-s11-secret".equals(authorization);
            boolean routeVariant = exchange.getRequestURI().getPath().equals("/api/v1/demo/");
            int status = positiveControl || (vulnerableViewer && routeVariant) ? 200 : 403;
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
            int port = fixture.getAddress().getPort();
            LocalWorkspaceStore workspace = new LocalWorkspaceStore(temp);
            var project = workspace.createProject("Sprint 11 Finding Review", "Standalone lifecycle");
            var target = workspace.addTarget(
                    project.id(),
                    "Controlled Lab",
                    "http://127.0.0.1:" + port + "/api/v1",
                    "LAB",
                    "AUTH-S11-001",
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

            StandaloneControlledExecutionService active = new StandaloneControlledExecutionService(workspace);
            var violationRun = active.executeRouteEquivalence(
                    project.id(),
                    target.id(),
                    expectation.id(),
                    "/api/v1/demo",
                    "Bearer viewer-s11-secret",
                    "Bearer admin-s11-secret",
                    true);

            StandaloneFindingLifecycleService findings = new StandaloneFindingLifecycleService(workspace);
            check(findings.eligibleExecutions(project.id()).size() == 1,
                    "verified DENY-to-ALLOW execution is eligible");

            var opened = findings.openFromExecution(project.id(), violationRun.runId());
            check(opened.finding().state() == FindingLifecycleState.NEEDS_REVIEW,
                    "eligible execution opens in NEEDS_REVIEW");
            check(opened.candidate().state().name().equals("CANDIDATE"),
                    "execution is projected as review candidate");
            check(opened.candidate().expectedDecision().name().equals("DENY"),
                    "candidate expected decision retained");
            check(opened.candidate().observedDecision().name().equals("ALLOW"),
                    "candidate observed decision retained");
            check(opened.finding().severity() == FindingSeverity.LOW,
                    "base impact profile yields independent LOW severity");
            check(opened.finding().confidence() == FindingConfidence.HIGH,
                    "verified active evidence yields HIGH candidate confidence");
            check(opened.finding().supportingEvidenceIds().contains(
                    violationRun.evidenceArtifactId().toString()),
                    "active execution evidence anchors finding");

            var repeated = findings.openFromExecution(project.id(), violationRun.runId());
            check(repeated.finding().findingId().equals(opened.finding().findingId()),
                    "reopening same source execution is idempotent");
            check(findings.findings(project.id()).size() == 1,
                    "idempotent intake does not duplicate finding");

            StandaloneEvidenceService evidence = new StandaloneEvidenceService(workspace);
            var validationEvidence = evidence.captureExecutionSummary(
                    project.id(), target.id(), "review-validation", "Independent validation evidence");
            var confirmationEvidence = evidence.captureExecutionSummary(
                    project.id(), target.id(), "review-confirmation", "Independent confirmation evidence");

            expectFailure(() -> findings.transition(
                    project.id(),
                    opened.finding().findingId(),
                    "CONFIRMED",
                    "reviewer-a",
                    "Attempted direct confirmation",
                    List.of(validationEvidence.evidenceId())),
                    "direct NEEDS_REVIEW to CONFIRMED is rejected");

            var validated = findings.transition(
                    project.id(),
                    opened.finding().findingId(),
                    "VALIDATED",
                    "reviewer-a",
                    "Execution lineage and authorization context validated",
                    List.of(validationEvidence.evidenceId()));
            check(validated.finding().state() == FindingLifecycleState.VALIDATED,
                    "review advances to VALIDATED");

            expectFailure(() -> findings.transition(
                    project.id(),
                    validated.finding().findingId(),
                    "CONFIRMED",
                    "reviewer-secret",
                    "Authorization: Bearer secret-review-token",
                    List.of(confirmationEvidence.evidenceId())),
                    "secret-bearing review reason is rejected");

            var confirmed = findings.transition(
                    project.id(),
                    validated.finding().findingId(),
                    "CONFIRMED",
                    "reviewer-b",
                    "Independent review confirms the evidence-backed authorization violation",
                    List.of(confirmationEvidence.evidenceId()));
            check(confirmed.finding().state() == FindingLifecycleState.CONFIRMED,
                    "validated finding can be explicitly confirmed");
            check(confirmed.finding().history().size() == 2,
                    "review history contains validation and confirmation");
            check(confirmed.finding().severity() == FindingSeverity.LOW,
                    "severity remains immutable through review");
            check(confirmed.finding().confidence() == FindingConfidence.HIGH,
                    "confidence remains immutable through review");

            StandaloneFindingLifecycleService reopened =
                    new StandaloneFindingLifecycleService(new LocalWorkspaceStore(temp));
            var persisted = reopened.finding(project.id(), confirmed.finding().findingId());
            check(persisted.finding().state() == FindingLifecycleState.CONFIRMED,
                    "confirmed finding persists across restart");
            check(persisted.finding().history().size() == 2,
                    "review history persists across restart");

            int beforeNonViolation = requests.get();
            var nonViolationRun = active.executeRouteEquivalence(
                    project.id(),
                    target.id(),
                    expectation.id(),
                    "/api/v1/demo",
                    "Bearer denied-s11-context",
                    "Bearer admin-s11-secret",
                    true);
            check(requests.get() == beforeNonViolation + 4,
                    "non-violation control still executes through Core four-way differential");
            check(!nonViolationRun.observedDecision().name().equals("ALLOW"),
                    "non-violation control does not observe ALLOW");
            expectFailure(() -> findings.openFromExecution(project.id(), nonViolationRun.runId()),
                    "non-violation execution cannot enter finding lifecycle");

            var otherProject = workspace.createProject("Other Project", "Isolation");
            expectFailure(() -> findings.openFromExecution(otherProject.id(), violationRun.runId()),
                    "cross-project execution intake fails closed");

            System.out.println("SPRINT11_STANDALONE_FINDING_LIFECYCLE PASS assertions=" + assertions);
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
