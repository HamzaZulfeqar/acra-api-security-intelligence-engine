package io.acra.standalone.tests;

import com.sun.net.httpserver.HttpServer;
import io.acra.core.active.execution.AuthorizedIpHttpTransport;
import io.acra.core.domain.testing.TestState;
import io.acra.standalone.service.SecurityContextService;
import io.acra.standalone.service.StandaloneControlledExecutionService;
import io.acra.standalone.service.StandaloneEvidenceService;
import io.acra.standalone.service.StandaloneImportService;
import io.acra.standalone.store.LocalWorkspaceStore;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;

/** Exercise the external transport on an authorized, non-loopback CI interface only. */
public final class StandaloneAuthorizedIpExecutionTestSuite {
    private StandaloneAuthorizedIpExecutionTestSuite() { }

    public static void main(String[] args) throws Exception {
        InetAddress address = NetworkInterface.networkInterfaces()
                .flatMap(network -> network.inetAddresses())
                .filter(ip -> ip instanceof Inet4Address && AuthorizedIpHttpTransport.permittedIpv4(ip.getHostAddress()))
                .findFirst().orElseThrow(() -> new IllegalStateException("CI needs a non-loopback IPv4 interface"));
        Path temp = Files.createTempDirectory("acra-authorized-ip-");
        AtomicInteger requests = new AtomicInteger();
        HttpServer fixture = HttpServer.create(new InetSocketAddress(address, 0), 0);
        fixture.createContext("/api/v1/demo", exchange -> {
            requests.incrementAndGet();
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            boolean allowed = "Bearer approved-control".equals(auth)
                    || ("Bearer limited-user".equals(auth)
                        && exchange.getRequestURI().getPath().endsWith("/"));
            byte[] body = (allowed ? "{\"ok\":true}" : "{\"error\":\"denied\"}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(allowed ? 200 : 403, body.length);
            try (var out = exchange.getResponseBody()) { out.write(body); }
        });
        fixture.start();
        try {
            LocalWorkspaceStore workspace = new LocalWorkspaceStore(temp);
            var project = workspace.createProject("Authorized IPv4", "CI isolated fixture");
            var target = workspace.addTarget(project.id(), "Authorized staging",
                    "http://" + address.getHostAddress() + ":" + fixture.getAddress().getPort() + "/api/v1",
                    "STAGING", "CI-ROE-001", "SAFE_ACTIVE");
            new StandaloneImportService(workspace).importText(project.id(), target.id(), "OPENAPI", "fixture.json",
                    """
                    {"openapi":"3.0.3","info":{"title":"Isolated fixture","version":"1"},
                     "paths":{"/demo":{"get":{"responses":{"200":{"description":"ok"}}}}}}
                    """);
            SecurityContextService context = new SecurityContextService(workspace);
            context.addPrincipal(project.id(), "limited", "Limited", "BEARER");
            context.addRole(project.id(), "viewer", "Viewer");
            context.addTenant(project.id(), "tenant", "Tenant");
            context.addResource(project.id(), "resource", "demo", "limited", "tenant", "ACTIVE");
            var expectation = context.addExpectation(project.id(), target.id(), "/api/v1/demo", "READ",
                    "limited", "viewer", "tenant", "resource", "DENY", "baseline denied");
            StandaloneControlledExecutionService service = new StandaloneControlledExecutionService(workspace);
            try {
                service.executeRouteEquivalence(project.id(), target.id(), expectation.id(),
                        "/api/v1/demo", "Bearer limited-user", "Bearer approved-control", false);
                throw new AssertionError("missing confirmation must block");
            } catch (IllegalArgumentException expected) { }
            if (requests.get() != 0) throw new AssertionError("blocked run dispatched a request");
            var result = service.executeRouteEquivalence(project.id(), target.id(), expectation.id(),
                    "/api/v1/demo", "Bearer limited-user", "Bearer approved-control", true);
            if (result.state() != TestState.COMPLETED || requests.get() != 4) {
                throw new AssertionError("authorized staging differential did not complete exactly four requests");
            }
            var evidence = new StandaloneEvidenceService(workspace);
            for (var artifact : evidence.artifacts(project.id())) {
                String preview = evidence.redactedContent(project.id(), artifact.evidenceId());
                if (preview.contains("Bearer limited-user") || preview.contains("Bearer approved-control")) {
                    throw new AssertionError("transient identity leaked into persisted evidence");
                }
            }
            service.engageKillSwitch("operator stop");
            try {
                service.executeRouteEquivalence(project.id(), target.id(), expectation.id(),
                        "/api/v1/demo", "Bearer limited-user", "Bearer approved-control", true);
                throw new AssertionError("kill switch must block");
            } catch (IllegalStateException expected) { }
            if (requests.get() != 4) throw new AssertionError("kill switch dispatched a request");
            service.resetKillSwitch(true, "continue safety rejection tests");
            if (AuthorizedIpHttpTransport.permittedIpv4("127.0.0.1")
                    || AuthorizedIpHttpTransport.permittedIpv4("169.254.169.254")
                    || AuthorizedIpHttpTransport.permittedIpv4("api.example.test")) {
                throw new AssertionError("unsafe host admitted");
            }
            for (String invalidPath : new String[] {
                    "/api/v1/../admin", "/api/v1/%2e%2e/admin", "//outside.invalid/api/v1/demo"}) {
                try {
                    service.executeRouteEquivalence(project.id(), target.id(), expectation.id(),
                            invalidPath, "Bearer limited-user", "Bearer approved-control", true);
                    throw new AssertionError("unsafe path admitted: " + invalidPath);
                } catch (IllegalArgumentException expected) { }
            }
            var production = workspace.addTarget(project.id(), "Production", target.baseUri().toString(),
                    "PRODUCTION", "CI-ROE-001", "SAFE_ACTIVE");
            try {
                service.executeRouteEquivalence(project.id(), production.id(), expectation.id(),
                        "/api/v1/demo", "Bearer limited-user", "Bearer approved-control", true);
                throw new AssertionError("production target admitted");
            } catch (IllegalArgumentException expected) { }
            if (requests.get() != 4) throw new AssertionError("rejected inputs dispatched requests");
            System.out.println("STANDALONE_AUTHORIZED_IP_EXECUTION PASS host=" + address.getHostAddress());
        } finally {
            fixture.stop(0);
            try (var paths = Files.walk(temp)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }
}
