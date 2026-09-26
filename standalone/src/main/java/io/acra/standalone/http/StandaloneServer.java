package io.acra.standalone.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.acra.core.domain.http.HttpMethod;
import io.acra.standalone.model.CoreAuthorizationProjectionRecord;
import io.acra.standalone.model.AuthorizationContextDifferentialRecord;
import io.acra.standalone.model.CoreProjectionSnapshot;
import io.acra.standalone.model.EvidenceArtifactRecord;
import io.acra.standalone.model.EvidenceDifferentialRecord;
import io.acra.standalone.model.HttpEvidenceSampleRecord;
import io.acra.standalone.model.ImportSummary;
import io.acra.standalone.model.InventoryRecord;
import io.acra.standalone.model.ProjectRecord;
import io.acra.standalone.model.CandidateReviewRecord;
import io.acra.standalone.model.ControlledExecutionRecord;
import io.acra.standalone.model.StandaloneCandidateRecord;
import io.acra.standalone.model.StandaloneCoverageRecord;
import io.acra.standalone.model.StandaloneCoverageSummary;
import io.acra.standalone.model.StandaloneFindingReviewRecord;
import io.acra.standalone.model.StandaloneReportArtifact;
import io.acra.standalone.model.AuthorizationExpectationRecord;
import io.acra.standalone.model.PrincipalContextRecord;
import io.acra.standalone.model.ResourceContextRecord;
import io.acra.standalone.model.RoleContextRecord;
import io.acra.standalone.model.TargetRecord;
import io.acra.standalone.model.TenantContextRecord;
import io.acra.standalone.service.SecurityContextService;
import io.acra.standalone.service.StandaloneControlledExecutionService;
import io.acra.standalone.service.StandaloneCoreProjectionService;
import io.acra.standalone.service.StandaloneEvidenceService;
import io.acra.standalone.service.StandaloneFindingLifecycleService;
import io.acra.standalone.service.StandaloneFindingReproductionService;
import io.acra.standalone.service.StandaloneImportService;
import io.acra.standalone.service.StandaloneReviewReportingService;
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
    private static final int MAX_IMPORT_BODY_BYTES = 6 * 1024 * 1024;

    private final LocalWorkspaceStore store;
    private final StandaloneImportService importService;
    private final SecurityContextService contextService;
    private final StandaloneCoreProjectionService projectionService;
    private final StandaloneEvidenceService evidenceService;
    private final StandaloneReviewReportingService reviewReportingService;
    private final StandaloneControlledExecutionService controlledExecutionService;
    private final StandaloneFindingLifecycleService findingLifecycleService;
    private final StandaloneFindingReproductionService findingReproductionService;
    private final HttpServer server;
    private final String csrfToken;

    public StandaloneServer(LocalWorkspaceStore store, int port) throws IOException {
        this.store = store;
        this.importService = new StandaloneImportService(store);
        this.contextService = new SecurityContextService(store);
        this.projectionService = new StandaloneCoreProjectionService(store);
        this.evidenceService = new StandaloneEvidenceService(store);
        this.reviewReportingService = new StandaloneReviewReportingService(store);
        this.controlledExecutionService = new StandaloneControlledExecutionService(store);
        this.findingLifecycleService = new StandaloneFindingLifecycleService(store);
        this.findingReproductionService = new StandaloneFindingReproductionService(store);
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
        server.createContext("/api/import", this::handleImport);
        server.createContext("/api/inventory", this::handleInventory);
        server.createContext("/api/context", this::handleContext);
        server.createContext("/api/projection", this::handleProjection);
        server.createContext("/api/evidence", this::handleEvidence);
        server.createContext("/api/differential", this::handleDifferential);
        server.createContext("/api/candidates", this::handleCandidates);
        server.createContext("/api/coverage", this::handleCoverage);
        server.createContext("/api/report", this::handleReport);
        server.createContext("/api/active", this::handleActive);
        server.createContext("/api/findings", this::handleFindings);
        server.createContext("/api/finding-reproduction", this::handleFindingReproduction);
        server.createContext("/", this::handleStatic);
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        if (!allowRequest(exchange, "GET", false)) return;
        HttpSupport.sendJson(exchange, 200,
                "{\"status\":\"UP\",\"product\":\"ACRA\",\"mode\":\"STANDALONE\"," +
                "\"bind\":\"127.0.0.1\",\"burpRequired\":false,\"coreLinked\":true," +
                "\"imports\":[\"OPENAPI\",\"HAR\",\"RAW_HTTP\"]," +
                "\"csrfToken\":" + HttpSupport.jsonString(csrfToken) + "}");
    }

    private void handleCapabilities(HttpExchange exchange) throws IOException {
        if (!allowRequest(exchange, "GET", false)) return;
        String methods = java.util.Arrays.stream(HttpMethod.values())
                .map(value -> HttpSupport.jsonString(value.name()))
                .collect(java.util.stream.Collectors.joining(","));
        HttpSupport.sendJson(exchange, 200,
                "{\"core\":\"ACRA Core\",\"httpMethods\":[" + methods + "]," +
                "\"imports\":[\"OPENAPI\",\"HAR\",\"RAW_HTTP\"]," +
                "\"workspaces\":[\"Targets\",\"API Inventory\",\"Authorization\",\"Object Access\"," +
                "\"Function Access\",\"Property Access\",\"Workflow\",\"Routing\",\"Evidence\"," +
                "\"Candidates\",\"Findings\",\"Coverage\",\"Reports\"]}");
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

    private void handleImport(HttpExchange exchange) throws IOException {
        if (!allowRequest(exchange, "POST", true)) return;
        try {
            Map<String, String> form = HttpSupport.parseForm(HttpSupport.readBody(exchange, MAX_IMPORT_BODY_BYTES));
            UUID projectId = UUID.fromString(required(form, "projectId"));
            UUID targetId = UUID.fromString(required(form, "targetId"));
            ImportSummary summary = importService.importText(
                    projectId,
                    targetId,
                    required(form, "importType"),
                    form.getOrDefault("sourceReference", ""),
                    required(form, "content")
            );
            HttpSupport.sendJson(exchange, 201, importSummaryJson(summary));
        } catch (IllegalArgumentException ex) {
            sendBadRequest(exchange, ex);
        }
    }

    private void handleInventory(HttpExchange exchange) throws IOException {
        if (!allowRequest(exchange, "GET", false)) return;
        try {
            Map<String, String> query = HttpSupport.parseQuery(exchange.getRequestURI().getRawQuery());
            UUID projectId = UUID.fromString(required(query, "projectId"));
            sendInventory(exchange, store.listInventory(projectId));
        } catch (IllegalArgumentException ex) {
            sendBadRequest(exchange, ex);
        }
    }

    private void handleContext(HttpExchange exchange) throws IOException {
        if ("GET".equals(exchange.getRequestMethod())) {
            if (!allowRequest(exchange, "GET", false)) return;
            try {
                Map<String, String> query = HttpSupport.parseQuery(exchange.getRequestURI().getRawQuery());
                UUID projectId = UUID.fromString(required(query, "projectId"));
                sendContext(exchange, projectId);
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
                String kind = required(form, "kind").strip().toUpperCase(java.util.Locale.ROOT);

                String json = switch (kind) {
                    case "PRINCIPAL" -> principalJson(contextService.addPrincipal(
                            projectId,
                            required(form, "principalId"),
                            form.getOrDefault("displayName", ""),
                            required(form, "authenticationType")));
                    case "ROLE" -> roleJson(contextService.addRole(
                            projectId,
                            required(form, "roleId"),
                            required(form, "name")));
                    case "TENANT" -> tenantJson(contextService.addTenant(
                            projectId,
                            required(form, "tenantId"),
                            form.getOrDefault("name", "")));
                    case "RESOURCE" -> resourceJson(contextService.addResource(
                            projectId,
                            required(form, "resourceId"),
                            required(form, "resourceType"),
                            form.getOrDefault("ownerPrincipalId", ""),
                            form.getOrDefault("tenantId", ""),
                            form.getOrDefault("state", "")));
                    case "EXPECTATION" -> expectationJson(contextService.addExpectation(
                            projectId,
                            UUID.fromString(required(form, "targetId")),
                            required(form, "endpoint"),
                            required(form, "action"),
                            required(form, "principalId"),
                            form.getOrDefault("roleId", ""),
                            form.getOrDefault("tenantId", ""),
                            form.getOrDefault("resourceId", ""),
                            required(form, "expectedDecision"),
                            form.getOrDefault("rationale", "")));
                    default -> throw new IllegalArgumentException("unsupported context kind");
                };
                HttpSupport.sendJson(exchange, 201, json);
            } catch (IllegalArgumentException ex) {
                sendBadRequest(exchange, ex);
            }
            return;
        }

        methodNotAllowed(exchange);
    }

    private void handleProjection(HttpExchange exchange) throws IOException {
        if (!allowRequest(exchange, "GET", false)) return;
        try {
            Map<String, String> query = HttpSupport.parseQuery(exchange.getRequestURI().getRawQuery());
            UUID projectId = UUID.fromString(required(query, "projectId"));
            HttpSupport.sendJson(exchange, 200, projectionJson(projectionService.project(projectId)));
        } catch (IllegalArgumentException ex) {
            sendBadRequest(exchange, ex);
        }
    }

    private void handleEvidence(HttpExchange exchange) throws IOException {
        if (!allowRequest(exchange, "GET", false)) return;
        try {
            Map<String, String> query = HttpSupport.parseQuery(exchange.getRequestURI().getRawQuery());
            UUID projectId = UUID.fromString(required(query, "projectId"));
            String evidenceId = query.getOrDefault("evidenceId", "");
            if (!evidenceId.isBlank()) {
                UUID id = UUID.fromString(evidenceId);
                EvidenceArtifactRecord artifact = evidenceService.artifacts(projectId).stream()
                        .filter(value -> value.evidenceId().equals(id))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("unknown evidence artifact"));
                String content = evidenceService.redactedContent(projectId, id);
                HttpSupport.sendJson(exchange, 200,
                        "{\"artifact\":" + evidenceArtifactJson(artifact) +
                        ",\"redactedContent\":" + HttpSupport.jsonString(content) + "}");
                return;
            }

            String artifacts = evidenceService.artifacts(projectId).stream()
                    .map(StandaloneServer::evidenceArtifactJson)
                    .collect(java.util.stream.Collectors.joining(",", "[", "]"));
            String samples = evidenceService.samples(projectId).stream()
                    .map(StandaloneServer::httpEvidenceSampleJson)
                    .collect(java.util.stream.Collectors.joining(",", "[", "]"));
            HttpSupport.sendJson(exchange, 200,
                    "{\"artifacts\":" + artifacts + ",\"samples\":" + samples + "}");
        } catch (IllegalArgumentException ex) {
            sendBadRequest(exchange, ex);
        }
    }

    private void handleDifferential(HttpExchange exchange) throws IOException {
        if (!allowRequest(exchange, "GET", false)) return;
        try {
            Map<String, String> query = HttpSupport.parseQuery(exchange.getRequestURI().getRawQuery());
            UUID projectId = UUID.fromString(required(query, "projectId"));
            String type = required(query, "type").strip().toUpperCase(java.util.Locale.ROOT);

            if ("HTTP".equals(type)) {
                EvidenceDifferentialRecord diff = evidenceService.compareHttp(
                        projectId,
                        UUID.fromString(required(query, "leftId")),
                        UUID.fromString(required(query, "rightId")),
                        query.getOrDefault("mode", "NORMALIZED"));
                HttpSupport.sendJson(exchange, 200, evidenceDifferentialJson(diff));
                return;
            }

            if ("AUTHORIZATION".equals(type)) {
                AuthorizationContextDifferentialRecord diff = evidenceService.compareAuthorization(
                        projectId,
                        UUID.fromString(required(query, "leftId")),
                        UUID.fromString(required(query, "rightId")));
                HttpSupport.sendJson(exchange, 200, authorizationDifferentialJson(diff));
                return;
            }

            throw new IllegalArgumentException("unsupported differential type");
        } catch (IllegalArgumentException ex) {
            sendBadRequest(exchange, ex);
        }
    }

    private void handleCandidates(HttpExchange exchange) throws IOException {
        if ("GET".equals(exchange.getRequestMethod())) {
            if (!allowRequest(exchange, "GET", false)) return;
            try {
                Map<String, String> query = HttpSupport.parseQuery(exchange.getRequestURI().getRawQuery());
                UUID projectId = UUID.fromString(required(query, "projectId"));
                String json = reviewReportingService.candidates(projectId).stream()
                        .map(StandaloneServer::candidateJson)
                        .collect(java.util.stream.Collectors.joining(",", "[", "]"));
                HttpSupport.sendJson(exchange, 200, json);
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
                CandidateReviewRecord review = reviewReportingService.updateReview(
                        projectId,
                        required(form, "candidateId"),
                        required(form, "state"),
                        form.getOrDefault("note", ""));
                HttpSupport.sendJson(exchange, 200, candidateReviewJson(review));
            } catch (IllegalArgumentException ex) {
                sendBadRequest(exchange, ex);
            }
            return;
        }
        methodNotAllowed(exchange);
    }

    private void handleCoverage(HttpExchange exchange) throws IOException {
        if (!allowRequest(exchange, "GET", false)) return;
        try {
            Map<String, String> query = HttpSupport.parseQuery(exchange.getRequestURI().getRawQuery());
            UUID projectId = UUID.fromString(required(query, "projectId"));
            List<StandaloneCoverageRecord> rows = reviewReportingService.coverage(projectId);
            StandaloneCoverageSummary summary = StandaloneCoverageSummary.from(rows);
            String entries = rows.stream().map(StandaloneServer::coverageJson)
                    .collect(java.util.stream.Collectors.joining(",", "[", "]"));
            HttpSupport.sendJson(exchange, 200,
                    "{\"summary\":" + coverageSummaryJson(summary) + ",\"entries\":" + entries + "}");
        } catch (IllegalArgumentException ex) {
            sendBadRequest(exchange, ex);
        }
    }

    private void handleReport(HttpExchange exchange) throws IOException {
        if (!allowRequest(exchange, "GET", false)) return;
        try {
            Map<String, String> query = HttpSupport.parseQuery(exchange.getRequestURI().getRawQuery());
            UUID projectId = UUID.fromString(required(query, "projectId"));
            StandaloneReportArtifact artifact = reviewReportingService.report(
                    projectId, query.getOrDefault("format", "JSON"));
            HttpSupport.sendJson(exchange, 200, reportArtifactJson(artifact));
        } catch (IllegalArgumentException ex) {
            sendBadRequest(exchange, ex);
        }
    }

    private void handleActive(HttpExchange exchange) throws IOException {
        if ("GET".equals(exchange.getRequestMethod())) {
            if (!allowRequest(exchange, "GET", false)) return;
            try {
                Map<String, String> query = HttpSupport.parseQuery(exchange.getRequestURI().getRawQuery());
                UUID projectId = UUID.fromString(required(query, "projectId"));
                String executions = controlledExecutionService.executions(projectId).stream()
                        .map(StandaloneServer::controlledExecutionJson)
                        .collect(java.util.stream.Collectors.joining(",", "[", "]"));
                HttpSupport.sendJson(exchange, 200,
                        "{\"killSwitchEngaged\":" + controlledExecutionService.killSwitchEngaged()
                        + ",\"executions\":" + executions + "}");
            } catch (IllegalArgumentException ex) {
                sendBadRequest(exchange, ex);
            }
            return;
        }

        if ("POST".equals(exchange.getRequestMethod())) {
            if (!allowRequest(exchange, "POST", true)) return;
            try {
                Map<String, String> form = HttpSupport.parseForm(HttpSupport.readBody(exchange, MAX_BODY_BYTES));
                String action = required(form, "action").strip().toUpperCase(java.util.Locale.ROOT);

                if ("EXECUTE_ROUTE_EQUIVALENCE".equals(action)) {
                    UUID projectId = UUID.fromString(required(form, "projectId"));
                    ControlledExecutionRecord record = controlledExecutionService.executeRouteEquivalence(
                            projectId,
                            UUID.fromString(required(form, "targetId")),
                            UUID.fromString(required(form, "expectationId")),
                            required(form, "concretePath"),
                            required(form, "testedAuthorizationValue"),
                            required(form, "positiveControlAuthorizationValue"),
                            Boolean.parseBoolean(form.getOrDefault("confirmed", "false")));
                    HttpSupport.sendJson(exchange, 201, controlledExecutionJson(record));
                    return;
                }

                if ("KILL".equals(action)) {
                    controlledExecutionService.engageKillSwitch(form.getOrDefault("reason", "standalone operator stop"));
                    HttpSupport.sendJson(exchange, 200,
                            "{\"killSwitchEngaged\":true}");
                    return;
                }

                if ("RESET_KILL".equals(action)) {
                    controlledExecutionService.resetKillSwitch(
                            Boolean.parseBoolean(form.getOrDefault("confirmed", "false")),
                            form.getOrDefault("reason", "standalone operator reset"));
                    HttpSupport.sendJson(exchange, 200,
                            "{\"killSwitchEngaged\":" + controlledExecutionService.killSwitchEngaged() + "}");
                    return;
                }

                throw new IllegalArgumentException("unsupported active action");
            } catch (IllegalArgumentException ex) {
                sendBadRequest(exchange, ex);
            } catch (IllegalStateException ex) {
                sendConflict(exchange, ex);
            }
            return;
        }

        methodNotAllowed(exchange);
    }

    private void handleFindings(HttpExchange exchange) throws IOException {
        if ("GET".equals(exchange.getRequestMethod())) {
            if (!allowRequest(exchange, "GET", false)) return;
            try {
                Map<String, String> query = HttpSupport.parseQuery(exchange.getRequestURI().getRawQuery());
                UUID projectId = UUID.fromString(required(query, "projectId"));
                String eligible = findingLifecycleService.eligibleExecutions(projectId).stream()
                        .map(StandaloneServer::controlledExecutionJson)
                        .collect(java.util.stream.Collectors.joining(",", "[", "]"));
                String findings = findingLifecycleService.findings(projectId).stream()
                        .map(StandaloneServer::reviewedFindingJson)
                        .collect(java.util.stream.Collectors.joining(",", "[", "]"));
                HttpSupport.sendJson(exchange, 200,
                        "{\"eligibleExecutions\":" + eligible + ",\"findings\":" + findings + "}");
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
                String action = required(form, "action").strip().toUpperCase(java.util.Locale.ROOT);

                if ("OPEN".equals(action)) {
                    StandaloneFindingReviewRecord record = findingLifecycleService.openFromExecution(
                            projectId,
                            UUID.fromString(required(form, "runId")));
                    HttpSupport.sendJson(exchange, 201, reviewedFindingJson(record));
                    return;
                }

                if ("TRANSITION".equals(action)) {
                    StandaloneFindingReviewRecord record = findingLifecycleService.transition(
                            projectId,
                            required(form, "findingId"),
                            required(form, "targetState"),
                            required(form, "reviewerReference"),
                            required(form, "reason"),
                            List.of(UUID.fromString(required(form, "evidenceId"))));
                    HttpSupport.sendJson(exchange, 200, reviewedFindingJson(record));
                    return;
                }

                throw new IllegalArgumentException("unsupported finding action");
            } catch (IllegalArgumentException ex) {
                sendBadRequest(exchange, ex);
            }
            return;
        }

        methodNotAllowed(exchange);
    }

    private void handleFindingReproduction(HttpExchange exchange) throws IOException {
        if (!allowRequest(exchange, "GET", false)) return;
        try {
            Map<String, String> query = HttpSupport.parseQuery(exchange.getRequestURI().getRawQuery());
            UUID projectId = UUID.fromString(required(query, "projectId"));
            String findingId = required(query, "findingId");
            String format = required(query, "format").strip().toUpperCase(java.util.Locale.ROOT);

            String json = switch (format) {
                case "JSON" -> findingExportJson(findingReproductionService.json(projectId, findingId));
                case "SARIF" -> findingExportJson(findingReproductionService.sarif(projectId, findingId));
                case "BURP_DRAFT" -> findingBurpDraftJson(
                        findingReproductionService.burpDraft(projectId, findingId));
                default -> throw new IllegalArgumentException("unsupported reproduction format");
            };
            HttpSupport.sendJson(exchange, 200, json);
        } catch (IllegalArgumentException ex) {
            sendBadRequest(exchange, ex);
        }
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

    private static void sendInventory(HttpExchange exchange, List<InventoryRecord> inventory) throws IOException {
        String json = inventory.stream().map(StandaloneServer::inventoryJson)
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
        HttpSupport.sendJson(exchange, 200, json);
    }

    private void sendContext(HttpExchange exchange, UUID projectId) throws IOException {
        String principals = contextService.principals(projectId).stream()
                .map(StandaloneServer::principalJson)
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
        String roles = contextService.roles(projectId).stream()
                .map(StandaloneServer::roleJson)
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
        String tenants = contextService.tenants(projectId).stream()
                .map(StandaloneServer::tenantJson)
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
        String resources = contextService.resources(projectId).stream()
                .map(StandaloneServer::resourceJson)
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
        String expectations = contextService.expectations(projectId).stream()
                .map(StandaloneServer::expectationJson)
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
        HttpSupport.sendJson(exchange, 200,
                "{\"principals\":" + principals +
                ",\"roles\":" + roles +
                ",\"tenants\":" + tenants +
                ",\"resources\":" + resources +
                ",\"expectations\":" + expectations + "}");
    }

    private static String principalJson(PrincipalContextRecord record) {
        return "{\"id\":" + HttpSupport.jsonString(record.id().toString()) +
                ",\"principalId\":" + HttpSupport.jsonString(record.principalId()) +
                ",\"displayName\":" + HttpSupport.jsonString(record.displayName()) +
                ",\"authenticationType\":" + HttpSupport.jsonString(record.authenticationType().name()) +
                ",\"createdAt\":" + HttpSupport.jsonString(record.createdAt().toString()) + "}";
    }

    private static String roleJson(RoleContextRecord record) {
        return "{\"id\":" + HttpSupport.jsonString(record.id().toString()) +
                ",\"roleId\":" + HttpSupport.jsonString(record.roleId()) +
                ",\"name\":" + HttpSupport.jsonString(record.name()) +
                ",\"createdAt\":" + HttpSupport.jsonString(record.createdAt().toString()) + "}";
    }

    private static String tenantJson(TenantContextRecord record) {
        return "{\"id\":" + HttpSupport.jsonString(record.id().toString()) +
                ",\"tenantId\":" + HttpSupport.jsonString(record.tenantId()) +
                ",\"name\":" + HttpSupport.jsonString(record.name()) +
                ",\"createdAt\":" + HttpSupport.jsonString(record.createdAt().toString()) + "}";
    }

    private static String resourceJson(ResourceContextRecord record) {
        return "{\"id\":" + HttpSupport.jsonString(record.id().toString()) +
                ",\"resourceId\":" + HttpSupport.jsonString(record.resourceId()) +
                ",\"resourceType\":" + HttpSupport.jsonString(record.resourceType()) +
                ",\"ownerPrincipalId\":" + HttpSupport.jsonString(record.ownerPrincipalId()) +
                ",\"tenantId\":" + HttpSupport.jsonString(record.tenantId()) +
                ",\"state\":" + HttpSupport.jsonString(record.state()) +
                ",\"createdAt\":" + HttpSupport.jsonString(record.createdAt().toString()) + "}";
    }

    private static String expectationJson(AuthorizationExpectationRecord record) {
        return "{\"id\":" + HttpSupport.jsonString(record.id().toString()) +
                ",\"targetId\":" + HttpSupport.jsonString(record.targetId().toString()) +
                ",\"endpoint\":" + HttpSupport.jsonString(record.endpoint()) +
                ",\"action\":" + HttpSupport.jsonString(record.action().name()) +
                ",\"principalId\":" + HttpSupport.jsonString(record.principalId()) +
                ",\"roleId\":" + HttpSupport.jsonString(record.roleId()) +
                ",\"tenantId\":" + HttpSupport.jsonString(record.tenantId()) +
                ",\"resourceId\":" + HttpSupport.jsonString(record.resourceId()) +
                ",\"expectedDecision\":" + HttpSupport.jsonString(record.expectedDecision().name()) +
                ",\"rationale\":" + HttpSupport.jsonString(record.rationale()) +
                ",\"createdAt\":" + HttpSupport.jsonString(record.createdAt().toString()) + "}";
    }

    private static String candidateJson(StandaloneCandidateRecord record) {
        var candidate = record.candidate();
        return "{\"candidateId\":" + HttpSupport.jsonString(candidate.candidateId()) +
                ",\"coreState\":" + HttpSupport.jsonString(candidate.state().name()) +
                ",\"reviewState\":" + HttpSupport.jsonString(record.review().state().name()) +
                ",\"endpoint\":" + HttpSupport.jsonString(candidate.endpoint()) +
                ",\"principalId\":" + HttpSupport.jsonString(candidate.principalId()) +
                ",\"resourceId\":" + HttpSupport.jsonString(candidate.resourceId()) +
                ",\"expectedDecision\":" + HttpSupport.jsonString(candidate.expectedDecision().name()) +
                ",\"observedDecision\":" + HttpSupport.jsonString(candidate.observedDecision().name()) +
                ",\"confidence\":" + HttpSupport.jsonString(candidate.confidence()) +
                ",\"rationale\":" + HttpSupport.jsonString(candidate.rationale()) +
                ",\"reviewNote\":" + HttpSupport.jsonString(record.review().note()) + "}";
    }

    private static String candidateReviewJson(CandidateReviewRecord record) {
        return "{\"candidateId\":" + HttpSupport.jsonString(record.candidateId()) +
                ",\"state\":" + HttpSupport.jsonString(record.state().name()) +
                ",\"note\":" + HttpSupport.jsonString(record.note()) +
                ",\"updatedAt\":" + HttpSupport.jsonString(record.updatedAt().toString()) + "}";
    }

    private static String coverageJson(StandaloneCoverageRecord record) {
        return "{\"coverageId\":" + HttpSupport.jsonString(record.coverageId()) +
                ",\"targetId\":" + HttpSupport.jsonString(record.targetId().toString()) +
                ",\"method\":" + HttpSupport.jsonString(record.method()) +
                ",\"endpoint\":" + HttpSupport.jsonString(record.endpoint()) +
                ",\"disposition\":" + HttpSupport.jsonString(record.disposition().name()) +
                ",\"expectationCount\":" + record.expectationCount() +
                ",\"passiveObservationCount\":" + record.passiveObservationCount() +
                ",\"reason\":" + HttpSupport.jsonString(record.reason()) + "}";
    }

    private static String coverageSummaryJson(StandaloneCoverageSummary record) {
        return "{\"total\":" + record.total() +
                ",\"tested\":" + record.tested() +
                ",\"untested\":" + record.untested() +
                ",\"partial\":" + record.partial() +
                ",\"inconclusive\":" + record.inconclusive() +
                ",\"notApplicable\":" + record.notApplicable() + "}";
    }

    private static String reportArtifactJson(StandaloneReportArtifact artifact) {
        return "{\"format\":" + HttpSupport.jsonString(artifact.format()) +
                ",\"sha256\":" + HttpSupport.jsonString(artifact.sha256()) +
                ",\"content\":" + HttpSupport.jsonString(artifact.content()) + "}";
    }

    private static String controlledExecutionJson(ControlledExecutionRecord record) {
        return "{\"runId\":" + HttpSupport.jsonString(record.runId().toString()) +
                ",\"targetId\":" + HttpSupport.jsonString(record.targetId().toString()) +
                ",\"expectationId\":" + HttpSupport.jsonString(record.expectationId().toString()) +
                ",\"testId\":" + HttpSupport.jsonString(record.testId()) +
                ",\"executionId\":" + HttpSupport.jsonString(record.executionId()) +
                ",\"observationId\":" + HttpSupport.jsonString(record.observationId()) +
                ",\"endpoint\":" + HttpSupport.jsonString(record.endpoint()) +
                ",\"requestPath\":" + HttpSupport.jsonString(record.requestPath()) +
                ",\"mutatedPath\":" + HttpSupport.jsonString(record.mutatedPath()) +
                ",\"expectedDecision\":" + HttpSupport.jsonString(record.expectedDecision().name()) +
                ",\"observedDecision\":" + HttpSupport.jsonString(record.observedDecision().name()) +
                ",\"differentialClassification\":" + HttpSupport.jsonString(record.differentialClassification().name()) +
                ",\"state\":" + HttpSupport.jsonString(record.state().name()) +
                ",\"evidenceArtifactId\":" + HttpSupport.jsonString(record.evidenceArtifactId().toString()) +
                ",\"coreEvidenceObjectCount\":" + record.coreEvidenceObjectCount() +
                ",\"createdAt\":" + HttpSupport.jsonString(record.createdAt().toString()) + "}";
    }

    private static String findingExportJson(
            io.acra.core.reporting.finding.FindingReproductionExportArtifact artifact) {
        return "{\"format\":" + HttpSupport.jsonString(artifact.format()) +
                ",\"mediaType\":" + HttpSupport.jsonString(artifact.mediaType()) +
                ",\"fileName\":" + HttpSupport.jsonString(artifact.fileName()) +
                ",\"sha256\":" + HttpSupport.jsonString(artifact.sha256()) +
                ",\"content\":" + HttpSupport.jsonString(artifact.content()) + "}";
    }

    private static String findingBurpDraftJson(
            io.acra.core.reporting.finding.FindingBurpIssueDraft draft) {
        String evidence = draft.evidenceIds().stream()
                .map(HttpSupport::jsonString)
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
        String limitations = draft.limitations().stream()
                .map(HttpSupport::jsonString)
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
        return "{\"format\":\"BURP_DRAFT\"" +
                ",\"draftId\":" + HttpSupport.jsonString(draft.draftId()) +
                ",\"reproductionId\":" + HttpSupport.jsonString(draft.reproductionId()) +
                ",\"findingId\":" + HttpSupport.jsonString(draft.findingId()) +
                ",\"lifecycleState\":" + HttpSupport.jsonString(draft.lifecycleState().name()) +
                ",\"name\":" + HttpSupport.jsonString(draft.name()) +
                ",\"detail\":" + HttpSupport.jsonString(draft.detail()) +
                ",\"remediation\":" + HttpSupport.jsonString(draft.remediation()) +
                ",\"targetEndpoint\":" + HttpSupport.jsonString(draft.targetEndpoint()) +
                ",\"severity\":" + HttpSupport.jsonString(draft.severity().name()) +
                ",\"confidence\":" + HttpSupport.jsonString(draft.confidence().name()) +
                ",\"typicalSeverity\":" + HttpSupport.jsonString(draft.typicalSeverity().name()) +
                ",\"publicationEligible\":" + draft.publicationEligible() +
                ",\"evidenceIds\":" + evidence +
                ",\"limitations\":" + limitations + "}";
    }

    private static String reviewedFindingJson(StandaloneFindingReviewRecord record) {
        var finding = record.finding();
        var candidate = record.candidate();
        var risk = record.risk();

        String evidence = finding.supportingEvidenceIds().stream()
                .map(HttpSupport::jsonString)
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
        String history = finding.history().stream()
                .map(StandaloneServer::findingTransitionJson)
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));

        return "{\"sourceRunId\":" + HttpSupport.jsonString(record.sourceRunId().toString()) +
                ",\"findingId\":" + HttpSupport.jsonString(finding.findingId()) +
                ",\"candidateId\":" + HttpSupport.jsonString(finding.candidateId()) +
                ",\"projectId\":" + HttpSupport.jsonString(finding.projectId()) +
                ",\"state\":" + HttpSupport.jsonString(finding.state().name()) +
                ",\"severity\":" + HttpSupport.jsonString(finding.severity().name()) +
                ",\"confidence\":" + HttpSupport.jsonString(finding.confidence().name()) +
                ",\"endpoint\":" + HttpSupport.jsonString(candidate.endpoint()) +
                ",\"principalId\":" + HttpSupport.jsonString(candidate.principalId()) +
                ",\"resourceId\":" + HttpSupport.jsonString(candidate.resourceId()) +
                ",\"expectedDecision\":" + HttpSupport.jsonString(candidate.expectedDecision().name()) +
                ",\"observedDecision\":" + HttpSupport.jsonString(candidate.observedDecision().name()) +
                ",\"candidateRationale\":" + HttpSupport.jsonString(candidate.rationale()) +
                ",\"internalRiskScore\":" + risk.internalRiskScore() +
                ",\"riskRationale\":" + HttpSupport.jsonString(risk.rationale()) +
                ",\"supportingEvidenceIds\":" + evidence +
                ",\"history\":" + history +
                ",\"openedAt\":" + HttpSupport.jsonString(finding.openedAt().toString()) +
                ",\"updatedAt\":" + HttpSupport.jsonString(finding.updatedAt().toString()) +
                ",\"terminal\":" + finding.terminal() + "}";
    }

    private static String findingTransitionJson(io.acra.core.domain.finding.FindingReviewTransition transition) {
        String evidence = transition.evidenceIds().stream()
                .map(HttpSupport::jsonString)
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
        return "{\"transitionId\":" + HttpSupport.jsonString(transition.transitionId()) +
                ",\"fromState\":" + HttpSupport.jsonString(transition.fromState().name()) +
                ",\"toState\":" + HttpSupport.jsonString(transition.toState().name()) +
                ",\"occurredAt\":" + HttpSupport.jsonString(transition.occurredAt().toString()) +
                ",\"reviewerReference\":" + HttpSupport.jsonString(transition.reviewerReference()) +
                ",\"reason\":" + HttpSupport.jsonString(transition.reason()) +
                ",\"evidenceIds\":" + evidence + "}";
    }

    private static String evidenceArtifactJson(EvidenceArtifactRecord record) {
        return "{\"evidenceId\":" + HttpSupport.jsonString(record.evidenceId().toString()) +
                ",\"targetId\":" + HttpSupport.jsonString(record.targetId().toString()) +
                ",\"evidenceType\":" + HttpSupport.jsonString(record.evidenceType()) +
                ",\"sourceReference\":" + HttpSupport.jsonString(record.sourceReference()) +
                ",\"originalSha256\":" + HttpSupport.jsonString(record.originalSha256()) +
                ",\"redactionApplied\":" + record.redactionApplied() +
                ",\"originalLength\":" + record.originalLength() +
                ",\"storedLength\":" + record.storedLength() +
                ",\"httpSampleCount\":" + record.httpSampleCount() +
                ",\"createdAt\":" + HttpSupport.jsonString(record.createdAt().toString()) + "}";
    }

    private static String httpEvidenceSampleJson(HttpEvidenceSampleRecord record) {
        return "{\"sampleId\":" + HttpSupport.jsonString(record.sampleId().toString()) +
                ",\"evidenceId\":" + HttpSupport.jsonString(record.evidenceId().toString()) +
                ",\"targetId\":" + HttpSupport.jsonString(record.targetId().toString()) +
                ",\"method\":" + HttpSupport.jsonString(record.method()) +
                ",\"requestUrl\":" + HttpSupport.jsonString(record.requestUrl()) +
                ",\"responseStatus\":" + record.responseStatus() +
                ",\"responseContentType\":" + HttpSupport.jsonString(record.responseContentType()) +
                ",\"hasResponse\":" + record.hasResponse() +
                ",\"createdAt\":" + HttpSupport.jsonString(record.createdAt().toString()) + "}";
    }

    private static String evidenceDifferentialJson(EvidenceDifferentialRecord record) {
        String changes = record.changedSignals().stream()
                .map(HttpSupport::jsonString)
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
        return "{\"leftSampleId\":" + HttpSupport.jsonString(record.leftSampleId().toString()) +
                ",\"rightSampleId\":" + HttpSupport.jsonString(record.rightSampleId().toString()) +
                ",\"mode\":" + HttpSupport.jsonString(record.mode().name()) +
                ",\"equivalent\":" + record.equivalent() +
                ",\"changedSignals\":" + changes +
                ",\"requestMethodEqual\":" + record.requestMethodEqual() +
                ",\"requestUrlEqual\":" + record.requestUrlEqual() +
                ",\"leftStatus\":" + record.leftStatus() +
                ",\"rightStatus\":" + record.rightStatus() + "}";
    }

    private static String authorizationDifferentialJson(AuthorizationContextDifferentialRecord record) {
        String changes = record.changedFields().stream()
                .map(HttpSupport::jsonString)
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
        return "{\"leftExpectationId\":" + HttpSupport.jsonString(record.leftExpectationId().toString()) +
                ",\"rightExpectationId\":" + HttpSupport.jsonString(record.rightExpectationId().toString()) +
                ",\"equivalent\":" + record.equivalent() +
                ",\"changedFields\":" + changes + "}";
    }

    private static String projectionJson(CoreProjectionSnapshot snapshot) {
        String authorization = snapshot.authorization().stream()
                .map(StandaloneServer::projectionRowJson)
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
        return "{\"projectId\":" + HttpSupport.jsonString(snapshot.projectId().toString()) +
                ",\"policyFingerprint\":" + HttpSupport.jsonString(snapshot.policyFingerprint()) +
                ",\"membershipCount\":" + snapshot.membershipCount() +
                ",\"roleAssignmentCount\":" + snapshot.roleAssignmentCount() +
                ",\"permissionCount\":" + snapshot.permissionCount() +
                ",\"ruleCount\":" + snapshot.ruleCount() +
                ",\"authorization\":" + authorization +
                ",\"workflowWorkspaceProjected\":" + snapshot.workflowWorkspaceProjected() +
                ",\"workflowResolutionCount\":" + snapshot.workflowResolutionCount() +
                ",\"routingWorkspaceProjected\":" + snapshot.routingWorkspaceProjected() +
                ",\"routingAssessmentCount\":" + snapshot.routingAssessmentCount() +
                ",\"propertyWorkspaceProjected\":" + snapshot.propertyWorkspaceProjected() +
                ",\"propertyAssessmentCount\":" + snapshot.propertyAssessmentCount() + "}";
    }

    private static String projectionRowJson(CoreAuthorizationProjectionRecord record) {
        String roles = record.effectiveRoleIds().stream()
                .map(HttpSupport::jsonString)
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
        String reasons = record.resolutionReasons().stream()
                .map(HttpSupport::jsonString)
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
        return "{\"expectationId\":" + HttpSupport.jsonString(record.expectationId().toString()) +
                ",\"endpoint\":" + HttpSupport.jsonString(record.endpoint()) +
                ",\"action\":" + HttpSupport.jsonString(record.action()) +
                ",\"principalId\":" + HttpSupport.jsonString(record.principalId()) +
                ",\"configuredDecision\":" + HttpSupport.jsonString(record.configuredDecision().name()) +
                ",\"resolvedDecision\":" + HttpSupport.jsonString(record.resolvedDecision().name()) +
                ",\"resolutionState\":" + HttpSupport.jsonString(record.resolutionState().name()) +
                ",\"effectiveRoleIds\":" + roles +
                ",\"resolutionReasons\":" + reasons +
                ",\"bolaStatus\":" + HttpSupport.jsonString(record.bolaStatus().name()) +
                ",\"bflaStatus\":" + HttpSupport.jsonString(record.bflaStatus().name()) + "}";
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

    private static String inventoryJson(InventoryRecord record) {
        String sources = record.sourceTypes().stream()
                .map(HttpSupport::jsonString)
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
        String statuses = record.responseStatuses().stream()
                .sorted()
                .map(String::valueOf)
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
        return "{\"id\":" + HttpSupport.jsonString(record.id()) +
                ",\"targetId\":" + HttpSupport.jsonString(record.targetId().toString()) +
                ",\"method\":" + HttpSupport.jsonString(record.method().name()) +
                ",\"scheme\":" + HttpSupport.jsonString(record.scheme()) +
                ",\"host\":" + HttpSupport.jsonString(record.host()) +
                ",\"port\":" + record.port() +
                ",\"rawPath\":" + HttpSupport.jsonString(record.rawPath()) +
                ",\"canonicalPath\":" + HttpSupport.jsonString(record.canonicalPath()) +
                ",\"sourceTypes\":" + sources +
                ",\"observationCount\":" + record.observationCount() +
                ",\"documented\":" + record.documented() +
                ",\"responseStatuses\":" + statuses +
                ",\"firstSeen\":" + HttpSupport.jsonString(record.firstSeen().toString()) +
                ",\"lastSeen\":" + HttpSupport.jsonString(record.lastSeen().toString()) + "}";
    }

    private static String importSummaryJson(ImportSummary summary) {
        return "{\"importType\":" + HttpSupport.jsonString(summary.importType()) +
                ",\"observations\":" + summary.observations() +
                ",\"uniqueEndpoints\":" + summary.uniqueEndpoints() +
                ",\"inventorySize\":" + summary.inventorySize() + "}";
    }

    private static void sendConflict(HttpExchange exchange, IllegalStateException ex) throws IOException {
        HttpSupport.sendJson(exchange, 409, "{\"error\":" + HttpSupport.jsonString(ex.getMessage()) + "}");
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
