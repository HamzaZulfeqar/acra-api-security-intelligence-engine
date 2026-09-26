package io.acra.standalone.service;

import io.acra.core.domain.http.HttpMethod;
import io.acra.core.domain.http.HttpProtocol;
import io.acra.core.domain.http.HttpRequest;
import io.acra.core.domain.http.HttpTransaction;
import io.acra.core.extraction.defaults.DefaultUriExtractor;
import io.acra.core.imports.HarEndpointImporter;
import io.acra.core.imports.ImportedEndpointObservation;
import io.acra.core.imports.RawHttpRequestImporter;
import io.acra.core.openapi.OpenApiImporter;
import io.acra.core.route.RouteTemplateEngine;
import io.acra.core.security.TokenFingerprint;
import io.acra.standalone.model.ImportSummary;
import io.acra.standalone.model.InventoryRecord;
import io.acra.standalone.model.TargetRecord;
import io.acra.standalone.store.LocalWorkspaceStore;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class StandaloneImportService {
    private static final int MAX_IMPORT_CHARS = 2 * 1024 * 1024;

    private final LocalWorkspaceStore store;
    private final OpenApiImporter openApiImporter = new OpenApiImporter();
    private final HarEndpointImporter harImporter = new HarEndpointImporter();
    private final RawHttpRequestImporter rawHttpImporter = new RawHttpRequestImporter();
    private final RouteTemplateEngine routeTemplateEngine = new RouteTemplateEngine();
    private final DefaultUriExtractor uriExtractor = new DefaultUriExtractor();

    public StandaloneImportService(LocalWorkspaceStore store) {
        this.store = java.util.Objects.requireNonNull(store);
    }

    public ImportSummary importText(
            UUID projectId,
            UUID targetId,
            String importType,
            String sourceReference,
            String content
    ) throws IOException {
        if (content == null || content.isBlank()) throw new IllegalArgumentException("import content is required");
        if (content.length() > MAX_IMPORT_CHARS) throw new IllegalArgumentException("import content exceeds 2 MiB character limit");

        TargetRecord target = store.findTarget(projectId, targetId);
        String type = normalizeType(importType);
        List<Seed> seeds = switch (type) {
            case "OPENAPI" -> importOpenApi(target, content, sourceReference);
            case "HAR" -> importHar(target, content, sourceReference);
            case "RAW_HTTP" -> importRawHttp(target, content, sourceReference);
            default -> throw new IllegalArgumentException("unsupported import type");
        };

        Set<String> uniqueIds = new HashSet<>();
        for (Seed seed : seeds) {
            InventoryRecord record = toRecord(projectId, targetId, seed);
            store.upsertInventory(record);
            uniqueIds.add(record.id());
        }

        return new ImportSummary(type, seeds.size(), uniqueIds.size(), store.listInventory(projectId).size());
    }

    private List<Seed> importOpenApi(TargetRecord target, String content, String sourceReference) {
        var document = openApiImporter.importText(content, sourceReference);
        List<Seed> seeds = new ArrayList<>(document.operations().size());
        for (var operation : document.operations()) {
            URI uri = uriForOpenApiPath(target.baseUri(), operation.path());
            requireWithinTarget(target, uri);
            seeds.add(new Seed(
                    operation.method(),
                    uri,
                    0,
                    "OPENAPI",
                    true
            ));
        }
        return List.copyOf(seeds);
    }

    private List<Seed> importHar(TargetRecord target, String content, String sourceReference) {
        List<Seed> seeds = new ArrayList<>();
        for (ImportedEndpointObservation observation : harImporter.importText(content, sourceReference)) {
            requireWithinTarget(target, observation.uri());
            seeds.add(new Seed(
                    observation.method(),
                    observation.uri(),
                    observation.responseStatus(),
                    "HAR",
                    false
            ));
        }
        return List.copyOf(seeds);
    }

    private List<Seed> importRawHttp(TargetRecord target, String content, String sourceReference) {
        ImportedEndpointObservation observation = rawHttpImporter.importText(content, target.baseUri(), sourceReference);
        requireWithinTarget(target, observation.uri());
        return List.of(new Seed(
                observation.method(),
                observation.uri(),
                observation.responseStatus(),
                "RAW_HTTP",
                false
        ));
    }

    private InventoryRecord toRecord(UUID projectId, UUID targetId, Seed seed) {
        String rawPath = seed.uri().getRawPath();
        if (rawPath == null || rawPath.isBlank()) rawPath = "/";
        String canonical = canonicalPath(seed, rawPath);
        int port = effectivePort(seed.uri());
        String material = seed.method() + "|" + seed.uri().getScheme().toLowerCase(Locale.ROOT) + "|"
                + seed.uri().getHost().toLowerCase(Locale.ROOT) + "|" + port + "|" + canonical;
        String id = "inv-" + TokenFingerprint.sha256(material).substring(0, 24);
        Instant now = Instant.now();
        Set<Integer> statuses = seed.responseStatus() >= 100 ? Set.of(seed.responseStatus()) : Set.of();
        return new InventoryRecord(
                id,
                projectId,
                targetId,
                seed.method(),
                seed.uri().getScheme().toLowerCase(Locale.ROOT),
                seed.uri().getHost().toLowerCase(Locale.ROOT),
                port,
                rawPath,
                canonical,
                Set.of(seed.sourceType()),
                1,
                seed.documented(),
                statuses,
                now,
                now
        );
    }

    private String canonicalPath(Seed seed, String rawPath) {
        if (seed.documented()) {
            return routeTemplateEngine.parse(rawPath).canonical();
        }

        String rawTarget = rawPath;
        String query = seed.uri().getRawQuery();
        if (query != null && !query.isBlank()) rawTarget += "?" + query;

        HttpRequest request = HttpRequest.of(
                seed.method(),
                seed.uri().getScheme(),
                seed.uri().getHost(),
                effectivePort(seed.uri()),
                rawTarget,
                List.of(),
                new byte[0],
                HttpProtocol.HTTP_1_1
        );
        HttpTransaction transaction = new HttpTransaction(
                request,
                null,
                Instant.now(),
                "import-canonicalization",
                seed.sourceType(),
                java.util.Map.of()
        );
        return uriExtractor.extract(transaction).canonicalPath();
    }

    private static URI uriForOpenApiPath(URI targetBase, String operationPath) {
        String basePath = normalizeBasePath(targetBase.getPath());
        String opPath = operationPath == null || operationPath.isBlank() ? "/" : operationPath;
        if (!opPath.startsWith("/")) opPath = "/" + opPath;

        String finalPath;
        if (basePath.equals("/") || opPath.equals(basePath) || opPath.startsWith(basePath + "/")) {
            finalPath = opPath;
        } else {
            finalPath = (basePath.endsWith("/") ? basePath.substring(0, basePath.length() - 1) : basePath) + opPath;
        }

        String authority = targetBase.getHost() + (targetBase.getPort() > 0 ? ":" + targetBase.getPort() : "");
        return URI.create(targetBase.getScheme() + "://" + authority + finalPath);
    }

    private static void requireWithinTarget(TargetRecord target, URI candidate) {
        URI base = target.baseUri();
        if (!base.getScheme().equalsIgnoreCase(candidate.getScheme())) {
            throw new IllegalArgumentException("import contains endpoint outside target scheme");
        }
        if (!base.getHost().equalsIgnoreCase(candidate.getHost())) {
            throw new IllegalArgumentException("import contains endpoint outside target host");
        }
        if (effectivePort(base) != effectivePort(candidate)) {
            throw new IllegalArgumentException("import contains endpoint outside target port");
        }

        String basePath = normalizeBasePath(base.getPath());
        String candidatePath = normalizeBasePath(candidate.getPath());
        if (!basePath.equals("/")
                && !candidatePath.equals(basePath)
                && !candidatePath.startsWith(basePath + "/")) {
            throw new IllegalArgumentException("import contains endpoint outside target base path");
        }
    }

    private static int effectivePort(URI uri) {
        if (uri.getPort() > 0) return uri.getPort();
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private static String normalizeBasePath(String path) {
        if (path == null || path.isBlank()) return "/";
        String normalized = path.startsWith("/") ? path : "/" + path;
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String normalizeType(String value) {
        String normalized = value == null ? "" : value.strip().toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) throw new IllegalArgumentException("importType is required");
        return normalized;
    }

    private record Seed(
            HttpMethod method,
            URI uri,
            int responseStatus,
            String sourceType,
            boolean documented
    ) {}
}
