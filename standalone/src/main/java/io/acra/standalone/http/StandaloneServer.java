package io.acra.standalone.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.acra.core.domain.http.HttpMethod;
import io.acra.standalone.model.ProjectRecord;
import io.acra.standalone.model.TargetRecord;
import io.acra.standalone.store.LocalWorkspaceStore;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

public final class StandaloneServer implements AutoCloseable {
    private static final int MAX_BODY_BYTES = 32 * 1024;

    private final LocalWorkspaceStore store;
    private final HttpServer server;
    private final String csrfToken;

    public StandaloneServer(LocalWorkspaceStore store, int port) throws IOException {
        this.store = store;
        this.server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), port), 0);
        this.server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        this.csrfToken = newCsrfToken();
        registerRoutes();
    }

    public void start() {
        server.start();
    }

    public int port() {
        return server.getAddress().getPort();
    }

    public URI baseUri() {
        return URI.create("http://127.0.0.1:" + port() + "/");
    }

    public String csrfToken() {
        return csrfToken;
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private void registerRoutes() {
        server.createContext("/api/health", this::handleHealth);
        server.createContext("/api/capabilities", this::handleCapabilities);
        server.createContext("/api/projects", this::handleProjects);
        server.createContext("/api/targets", this::handleTargets);
        server.createContext("/", this::handleStatic);
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        if (!allowRequest(exchange, "GET", false)) return;
        HttpSupport.sendJson(exchange, 200,
                "{\"status\":\"UP\",\"product\":\"ACRA\",\"mode\":\"STANDALONE\"," +
                "\"bind\":\"127.0.0.1\",\"burpRequired\":false,\"coreLinked\":true," +
                "\"csrfToken\":" + HttpSupport.jsonString(csrfToken) + "}");
    }

    private void handleCapabilities(HttpExchange exchange) throws IOException {
        if (!allowRequest(exchange, "GET", false)) return;
        String methods = java.util.Arrays.stream(HttpMethod.values())
                .map(value -> HttpSupport.jsonString(value.name()))
                .collect(java.util.stream.Collectors.joining(","));
        HttpSupport.sendJson(exchange, 200,
                "{\"core\":\"ACRA Core\",\"httpMethods\":[" + methods + "]," +
                "\"workspaces\":[\"Targets\",\"API Inventory\",\"Authorization\",\"Object Access\"," +
                "\"Function Access\",\"Property Access\",\"Workflow\",\"Routing\",\"Evidence\"," +
                "\"Candidates\",\"Coverage\",\"Reports\"]}");
    }

    private void handleProjects(HttpExchange exchange) throws IOException {
        if ("GET".equals(exchange.getRequestMethod())) {
            if (!allowRequest(exchange, "GET", false)) return;
            sendProjects(exchange, store.listProjects());
            return;
        }
        if ("POST".equals(exchange.getRequestMethod())) {
            if (!allowRequest(exchange, "POST", true)) return;
            try {
                Map<String, String> form = HttpSupport.parseForm(HttpSupport.readBody(exchange, MAX_BODY_BYTES));
                ProjectRecord record = store.createProject(form.get("name"), form.get("description"));
                HttpSupport.sendJson(exchange, 201, projectJson(record));
            } catch (IllegalArgumentException ex) {
                sendBadRequest(exchange, ex);
            }
            return;
        }
        methodNotAllowed(exchange);
    }

    private void handleTargets(HttpExchange exchange) throws IOException {
        if ("GET".equals(exchange.getRequestMethod())) {
            if (!allowRequest(exchange, "GET", false)) return;
            try {
                Map<String, String> query = HttpSupport.parseQuery(exchange.getRequestURI().getRawQuery());
                UUID projectId = UUID.fromString(required(query, "projectId"));
                sendTargets(exchange, store.listTargets(projectId));
            } catch (IllegalArgumentException ex) {
                sendBadRequest(exchange, ex);
            }
            return;
        }
        if ("POST".equals(exchange.getRequestMethod())) {
            if (!allowRequest(exchange, "POST", true)) return;
            try {
                Map<String, String> form = HttpSupport.parseForm(HttpSupport.readBody(exchange, MAX_BODY_BYTES));
                UUID projectId = UUID.fromString(required(form, "projectId"));
                TargetRecord target = store.addTarget(
                        projectId,
                        form.get("displayName"),
                        form.get("baseUrl"),
                        form.get("environment"),
                        form.get("authorizationReference"),
                        form.get("testingMode"));
                HttpSupport.sendJson(exchange, 201, targetJson(target));
            } catch (IllegalArgumentException ex) {
                sendBadRequest(exchange, ex);
            }
            return;
        }
        methodNotAllowed(exchange);
    }

    private void handleStatic(HttpExchange exchange) throws IOException {
        if (!allowRequest(exchange, "GET", false)) return;
        String path = exchange.getRequestURI().getPath();
        String resource = switch (path) {
            case "/", "/index.html" -> "/web/index.html";
            case "/app.js" -> "/web/app.js";
            case "/styles.css" -> "/web/styles.css";
            default -> null;
        };
        if (resource == null) {
            HttpSupport.send(exchange, 404, "text/plain; charset=utf-8", "Not found");
            return;
        }
        try (var in = StandaloneServer.class.getResourceAsStream(resource)) {
            if (in == null) {
                HttpSupport.send(exchange, 500, "text/plain; charset=utf-8", "Resource unavailable");
                return;
            }
            String body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            String type = resource.endsWith(".js") ? "application/javascript; charset=utf-8"
                    : resource.endsWith(".css") ? "text/css; charset=utf-8"
                    : "text/html; charset=utf-8";
            HttpSupport.send(exchange, 200, type, body);
        }
    }

    private boolean allowRequest(HttpExchange exchange, String expectedMethod, boolean csrfRequired) throws IOException {
        if (!isAllowedHost(exchange)) {
            HttpSupport.sendJson(exchange, 403, "{\"error\":\"invalid host header\"}");
            return false;
        }
        if (!expectedMethod.equals(exchange.getRequestMethod())) {
            methodNotAllowed(exchange);
            return false;
        }
        if (csrfRequired && !csrfToken.equals(exchange.getRequestHeaders().getFirst("X-ACRA-CSRF"))) {
            HttpSupport.sendJson(exchange, 403, "{\"error\":\"csrf validation failed\"}");
            return false;
        }
        return true;
    }

    private boolean isAllowedHost(HttpExchange exchange) {
        String host = exchange.getRequestHeaders().getFirst("Host");
        if (host == null) return false;
        String normalized = host.toLowerCase(java.util.Locale.ROOT);
        return normalized.equals("127.0.0.1:" + port())
                || normalized.equals("localhost:" + port())
                || normalized.equals("[::1]:" + port());
    }

    private static String required(Map<String, String> values, String key) {
        String value = values.get(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(key + " is required");
        return value;
    }

    private static void sendProjects(HttpExchange exchange, List<ProjectRecord> projects) throws IOException {
        String json = projects.stream().map(StandaloneServer::projectJson)
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
        HttpSupport.sendJson(exchange, 200, json);
    }

    private static void sendTargets(HttpExchange exchange, List<TargetRecord> targets) throws IOException {
        String json = targets.stream().map(StandaloneServer::targetJson)
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
        HttpSupport.sendJson(exchange, 200, json);
    }

    private static String projectJson(ProjectRecord p) {
        return "{\"id\":" + HttpSupport.jsonString(p.id().toString()) +
                ",\"name\":" + HttpSupport.jsonString(p.name()) +
                ",\"description\":" + HttpSupport.jsonString(p.description()) +
                ",\"createdAt\":" + HttpSupport.jsonString(p.createdAt().toString()) + "}";
    }

    private static String targetJson(TargetRecord t) {
        return "{\"id\":" + HttpSupport.jsonString(t.id().toString()) +
                ",\"projectId\":" + HttpSupport.jsonString(t.projectId().toString()) +
                ",\"displayName\":" + HttpSupport.jsonString(t.displayName()) +
                ",\"baseUrl\":" + HttpSupport.jsonString(t.baseUri().toString()) +
                ",\"environment\":" + HttpSupport.jsonString(t.environment()) +
                ",\"authorizationReference\":" + HttpSupport.jsonString(t.authorizationReference()) +
                ",\"testingMode\":" + HttpSupport.jsonString(t.testingMode()) +
                ",\"createdAt\":" + HttpSupport.jsonString(t.createdAt().toString()) + "}";
    }

    private static void sendBadRequest(HttpExchange exchange, IllegalArgumentException ex) throws IOException {
        HttpSupport.sendJson(exchange, 400, "{\"error\":" + HttpSupport.jsonString(ex.getMessage()) + "}");
    }

    private static void methodNotAllowed(HttpExchange exchange) throws IOException {
        HttpSupport.sendJson(exchange, 405, "{\"error\":\"method not allowed\"}");
    }

    private static String newCsrfToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
