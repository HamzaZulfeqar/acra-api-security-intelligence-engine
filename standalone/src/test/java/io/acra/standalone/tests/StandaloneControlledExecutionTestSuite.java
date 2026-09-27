package io.acra.standalone.tests;

import com.sun.net.httpserver.HttpServer;
import io.acra.core.active.analysis.AuthorizationOutcome;
import io.acra.core.active.analysis.DifferentialClassification;
import io.acra.core.domain.testing.TestState;
import io.acra.standalone.service.SecurityContextService;
import io.acra.standalone.service.StandaloneControlledExecutionService;
import io.acra.standalone.service.StandaloneEvidenceService;
import io.acra.standalone.service.StandaloneImportService;
import io.acra.standalone.store.LocalWorkspaceStore;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;

public final class StandaloneControlledExecutionTestSuite {
    private static int assertions;

    private StandaloneControlledExecutionTestSuite() {}

    public static void main(String[] args) throws Exception {
        Path temp = Files.createTempDirectory("acra-s10-active-");
        AtomicInteger requests = new AtomicInteger();

        HttpServer fixture = HttpServer.create(
                new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        fixture.createContext("/api/v1/demo", exchange -> {
            requests.incrementAndGet();
            String authorization = exchange.getRequestHeaders().getFirst("Authorization");
            boolean testedContext = "Bearer viewer-phase7-secret".equals(authorization);
            boolean positiveContext = "Bearer admin-phase7-secret".equals(authorization);
            boolean variant = exchange.getRequestURI().getPath().equals("/api/v1/demo/");
            int status = positiveContext || (variant && testedContext) ? 200 : 403;
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
            var project = workspace.createProject("Controlled Active", "Phase 7");
            var target = workspace.addTarget(
                    project.id(),
                    "Controlled Lab",
                    "http://127.0.0.1:" + port + "/api/v1",
                    "LAB",
                    "AUTH-P7-001",
                    "CONTROLLED_LAB"
            );

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

            StandaloneControlledExecutionService service = new StandaloneControlledExecutionService(workspace);
            String testedSecret = "Bearer viewer-phase7-secret";
            String positiveSecret = "Bearer admin-phase7-secret";
            var run = service.executeRouteEquivalence(
                    project.id(),
                    target.id(),
                    expectation.id(),
                    "/api/v1/demo",
                    testedSecret,
                    positiveSecret,
                    true);

            check(run.state() == TestState.COMPLETED, "controlled execution completes");
            check(run.expectedDecision().name().equals("DENY"), "configured DENY retained");
            check(run.observedDecision() == AuthorizationOutcome.ALLOW, "route variant observed ALLOW");
            check(run.differentialClassification() == DifferentialClassification.UNEXPECTED_CHANGE,
                    "DENY to ALLOW route divergence is unexpected");
            check(run.requestPath().equals("/api/v1/demo"), "baseline path retained");
            check(run.mutatedPath().equals("/api/v1/demo/"), "generated trailing slash variant retained");
            check(run.coreEvidenceObjectCount() > 0, "Core evidence chain produced");
            check(requests.get() == 4, "existing executor emitted exactly four controlled variants");
            check(service.executions(project.id()).size() == 1, "execution summary persisted");

            StandaloneEvidenceService evidence = new StandaloneEvidenceService(workspace);
            var activeArtifact = evidence.artifacts(project.id()).stream()
                    .filter(value -> value.evidenceType().equals("ACTIVE_EXECUTION"))
                    .findFirst().orElseThrow();
            String preview = evidence.redactedContent(project.id(), activeArtifact.evidenceId());
            check(!preview.contains(testedSecret) && !preview.contains(positiveSecret),
                    "ephemeral authorization values are not persisted");
            check(preview.contains(run.executionId()), "execution evidence summary retains lineage");

            int beforeConfirmationBlock = requests.get();
            expectFailure(() -> service.executeRouteEquivalence(
                    project.id(), target.id(), expectation.id(), "/api/v1/demo", testedSecret, positiveSecret, false),
                    IllegalArgumentException.class,
                    "missing confirmation blocked");
            check(requests.get() == beforeConfirmationBlock, "confirmation block sends no requests");

            service.engageKillSwitch("test operator stop");
            int beforeKillBlock = requests.get();
            expectFailure(() -> service.executeRouteEquivalence(
                    project.id(), target.id(), expectation.id(), "/api/v1/demo", testedSecret, positiveSecret, true),
                    IllegalStateException.class,
                    "kill switch blocks active execution");
            check(requests.get() == beforeKillBlock, "kill switch block sends no requests");
            service.resetKillSwitch(true, "test operator reset");
            check(!service.killSwitchEngaged(), "kill switch reset requires explicit confirmation");

            var externalTarget = workspace.addTarget(
                    project.id(),
                    "External Lab Label",
                    "http://example.com/api/v1",
                    "LAB",
                    "AUTH-P7-EXT",
                    "CONTROLLED_LAB");
            expectFailure(() -> service.executeRouteEquivalence(
                    project.id(), externalTarget.id(), expectation.id(), "/api/v1/demo", testedSecret, positiveSecret, true),
                    IllegalArgumentException.class,
                    "external target blocked before dispatch");

            var passiveTarget = workspace.addTarget(
                    project.id(),
                    "Passive Loopback",
                    "http://127.0.0.1:" + port + "/api/v1",
                    "LAB",
                    "AUTH-P7-PASSIVE",
                    "PASSIVE");
            expectFailure(() -> service.executeRouteEquivalence(
                    project.id(), passiveTarget.id(), expectation.id(), "/api/v1/demo", testedSecret, positiveSecret, true),
                    IllegalArgumentException.class,
                    "non-CONTROLLED_LAB target blocked");

            check(service.executions(project.id()).size() == 1,
                    "blocked attempts do not create execution summaries");

            System.out.println("SPRINT10_STANDALONE_CONTROLLED_EXECUTION PASS assertions=" + assertions);
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

    private static void expectFailure(
            ThrowingRunnable runnable,
            Class<? extends Throwable> expected,
            String message
    ) throws Exception {
        assertions++;
        try {
            runnable.run();
            throw new AssertionError(message);
        } catch (Throwable actual) {
            if (!expected.isInstance(actual)) {
                throw new AssertionError(message + " expected=" + expected.getName()
                        + " actual=" + actual.getClass().getName(), actual);
            }
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
