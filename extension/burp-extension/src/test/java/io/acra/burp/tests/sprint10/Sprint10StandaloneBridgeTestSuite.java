package io.acra.burp.tests.sprint10;

import com.sun.net.httpserver.HttpServer;
import io.acra.burp.bridge.BridgeRequestSerializer;
import io.acra.burp.bridge.StandaloneBridgeClient;
import io.acra.burp.traffic.TrafficIntelligencePipeline;
import io.acra.core.domain.http.HttpHeader;
import io.acra.core.domain.http.HttpMethod;
import io.acra.core.domain.http.HttpProtocol;
import io.acra.core.domain.http.HttpRequest;
import io.acra.core.domain.http.HttpResponse;
import io.acra.core.domain.http.HttpTransaction;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class Sprint10StandaloneBridgeTestSuite {
    private static int assertions;

    private Sprint10StandaloneBridgeTestSuite() {}

    public static void main(String[] args) throws Exception {
        AtomicInteger imports = new AtomicInteger();
        HttpServer server = HttpServer.create(
                new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);

        server.createContext("/api/health", exchange -> {
            byte[] body = """
                    {"status":"UP","coreLinked":true,"burpRequired":false,"csrfToken":"bridge-csrf"}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var out = exchange.getResponseBody()) {
                out.write(body);
            }
        });

        server.createContext("/api/import", exchange -> {
            String csrf = exchange.getRequestHeaders().getFirst("X-ACRA-CSRF");
            String raw = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> form = form(raw);
            boolean valid = "bridge-csrf".equals(csrf)
                    && "project-1".equals(form.get("projectId"))
                    && "target-1".equals(form.get("targetId"))
                    && "RAW_HTTP".equals(form.get("importType"))
                    && form.getOrDefault("content", "").startsWith("GET /api/v1/users/42 HTTP/1.1");
            if (!valid) {
                byte[] body = "{\"error\":\"invalid bridge handoff\"}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(400, body.length);
                try (var out = exchange.getResponseBody()) {
                    out.write(body);
                }
                return;
            }
            imports.incrementAndGet();
            byte[] body = """
                    {"importType":"RAW_HTTP","observations":1,"uniqueEndpoints":1,"inventorySize":3}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(201, body.length);
            try (var out = exchange.getResponseBody()) {
                out.write(body);
            }
        });

        server.start();
        try {
            int port = server.getAddress().getPort();
            StandaloneBridgeClient client = new StandaloneBridgeClient(
                    "http://127.0.0.1:" + port + "/");

            var probe = client.probe();
            check(probe.success(), "loopback standalone health probe succeeds");
            check(probe.message().contains("Core linked=true"), "health reports Core linkage");

            String raw = "GET /api/v1/users/42 HTTP/1.1\r\nHost: 127.0.0.1\r\nAuthorization: Bearer transient\r\n\r\n";
            var result = client.sendRawRequest("project-1", "target-1", "burp:tx-1", raw);
            check(result.success(), "explicit bridge handoff succeeds");
            check(result.observations() == 1, "handoff observation count retained");
            check(result.uniqueEndpoints() == 1, "handoff unique endpoint count retained");
            check(result.inventorySize() == 3, "handoff inventory count retained");
            check(imports.get() == 1, "bridge sends exactly one import request");

            expectFailure(() -> new StandaloneBridgeClient("https://127.0.0.1:" + port + "/"),
                    "bridge management URL refuses HTTPS/non-local-management scheme");
            expectFailure(() -> new StandaloneBridgeClient("http://example.com:8787/"),
                    "bridge refuses non-loopback management URL");

            HttpRequest fallback = HttpRequest.of(
                    HttpMethod.GET,
                    "http",
                    "127.0.0.1",
                    8080,
                    "/api/v1/demo?x=1",
                    List.of(new HttpHeader("Accept", "application/json")),
                    new byte[0],
                    HttpProtocol.HTTP_1_1);
            String serialized = new BridgeRequestSerializer().raw(fallback);
            check(serialized.startsWith("GET /api/v1/demo?x=1 HTTP/1.1\r\n"), "fallback request line serialized");
            check(serialized.contains("Host: 127.0.0.1:8080\r\n"), "fallback Host header serialized");

            TrafficIntelligencePipeline pipeline = new TrafficIntelligencePipeline(2);
            pipeline.process(transaction("tx-1", "/api/v1/one"));
            pipeline.process(transaction("tx-2", "/api/v1/two"));
            pipeline.process(transaction("tx-3", "/api/v1/three"));
            check(pipeline.transaction("tx-1").isEmpty(), "bounded bridge buffer evicts oldest transaction");
            check(pipeline.transaction("tx-2").isPresent(), "bounded bridge buffer retains second transaction");
            check(pipeline.transaction("tx-3").isPresent(), "bounded bridge buffer retains newest transaction");

            System.out.println("SPRINT10_STANDALONE_BRIDGE PASS assertions=" + assertions);
        } finally {
            server.stop(0);
        }
    }

    private static HttpTransaction transaction(String id, String path) {
        HttpRequest request = HttpRequest.of(
                HttpMethod.GET,
                "http",
                "127.0.0.1",
                8080,
                path,
                List.of(new HttpHeader("Accept", "application/json")),
                new byte[0],
                HttpProtocol.HTTP_1_1);
        HttpResponse response = new HttpResponse(
                200,
                List.of(new HttpHeader("Content-Type", "application/json")),
                "{\"ok\":true}".getBytes(StandardCharsets.UTF_8),
                "application/json",
                HttpProtocol.HTTP_1_1,
                new byte[0]);
        return new HttpTransaction(request, response, Instant.now(), id, "BRIDGE_TEST", Map.of());
    }

    private static Map<String, String> form(String body) {
        TreeMap<String, String> values = new TreeMap<>();
        for (String pair : body.split("&")) {
            int eq = pair.indexOf('=');
            if (eq < 0) continue;
            values.put(
                    URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
                    URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
        }
        return values;
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

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
