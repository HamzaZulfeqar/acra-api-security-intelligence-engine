package io.acra.standalone.service;

import io.acra.core.analysis.PassiveDifferentialComparator;
import io.acra.core.analysis.ResponseComparisonMode;
import io.acra.core.domain.http.HttpProtocol;
import io.acra.core.domain.http.HttpResponse;
import io.acra.core.imports.RawHttpRequestImporter;
import io.acra.core.openapi.MiniJson;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.security.UniversalRedactor;
import io.acra.standalone.model.AuthorizationContextDifferentialRecord;
import io.acra.standalone.model.AuthorizationExpectationRecord;
import io.acra.standalone.model.EvidenceArtifactRecord;
import io.acra.standalone.model.EvidenceDifferentialRecord;
import io.acra.standalone.model.HttpEvidenceSampleRecord;
import io.acra.standalone.model.TargetRecord;
import io.acra.standalone.store.EvidenceArchiveStore;
import io.acra.standalone.store.LocalWorkspaceStore;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class StandaloneEvidenceService {
    private static final int MAX_SAMPLE_BODY = 262_144;

    private final LocalWorkspaceStore workspace;
    private final EvidenceArchiveStore store;
    private final SecurityContextService contextService;
    private final UniversalRedactor redactor = new UniversalRedactor();
    private final PassiveDifferentialComparator comparator = new PassiveDifferentialComparator();
    private final RawHttpRequestImporter rawHttpImporter = new RawHttpRequestImporter();

    public StandaloneEvidenceService(LocalWorkspaceStore workspace) {
        this.workspace = java.util.Objects.requireNonNull(workspace);
        this.store = new EvidenceArchiveStore(workspace);
        this.contextService = new SecurityContextService(workspace);
    }

    public EvidenceArtifactRecord captureImport(
            UUID projectId,
            UUID targetId,
            String evidenceType,
            String sourceReference,
            String content
    ) throws IOException {
        TargetRecord target = workspace.findTarget(projectId, targetId);
        if (content == null || content.isBlank()) throw new IllegalArgumentException("evidence content is required");

        String type = required(evidenceType, "evidenceType").toUpperCase(Locale.ROOT);
        String redactedContent = redactor.redactText(content);
        UUID evidenceId = UUID.randomUUID();
        Instant createdAt = Instant.now();

        List<HttpEvidenceSampleRecord> samples = switch (type) {
            case "HAR" -> harSamples(evidenceId, projectId, targetId, content, createdAt);
            case "RAW_HTTP" -> List.of(rawHttpSample(
                    evidenceId, projectId, targetId, target, content, createdAt));
            case "OPENAPI" -> List.of();
            default -> throw new IllegalArgumentException("unsupported evidence type");
        };

        EvidenceArtifactRecord artifact = new EvidenceArtifactRecord(
                evidenceId,
                projectId,
                targetId,
                type,
                sourceReference,
                TokenFingerprint.sha256(content),
                !redactedContent.equals(content),
                content.length(),
                redactedContent.length(),
                samples.size(),
                createdAt
        );
        store.save(artifact, redactedContent, samples);
        return artifact;
    }

    public EvidenceArtifactRecord captureExecutionSummary(
            UUID projectId,
            UUID targetId,
            String sourceReference,
            String summary
    ) throws IOException {
        workspace.findTarget(projectId, targetId);
        if (summary == null || summary.isBlank()) throw new IllegalArgumentException("execution summary is required");
        String redacted = redactor.redactText(summary);
        EvidenceArtifactRecord artifact = new EvidenceArtifactRecord(
                UUID.randomUUID(),
                projectId,
                targetId,
                "ACTIVE_EXECUTION",
                sourceReference,
                TokenFingerprint.sha256(summary),
                !redacted.equals(summary),
                summary.length(),
                redacted.length(),
                0,
                Instant.now());
        store.save(artifact, redacted, List.of());
        return artifact;
    }

    public List<EvidenceArtifactRecord> artifacts(UUID projectId) throws IOException {
        return store.listArtifacts(projectId);
    }

    public List<HttpEvidenceSampleRecord> samples(UUID projectId) throws IOException {
        return store.listSamples(projectId);
    }

    public String redactedContent(UUID projectId, UUID evidenceId) throws IOException {
        return store.readRedactedContent(projectId, evidenceId);
    }

    public EvidenceDifferentialRecord compareHttp(
            UUID projectId,
            UUID leftSampleId,
            UUID rightSampleId,
            String mode
    ) throws IOException {
        HttpEvidenceSampleRecord left = store.findSample(projectId, leftSampleId);
        HttpEvidenceSampleRecord right = store.findSample(projectId, rightSampleId);
        if (!left.hasResponse() || !right.hasResponse()) {
            throw new IllegalArgumentException("both HTTP evidence samples must contain responses");
        }

        ResponseComparisonMode comparisonMode;
        try {
            comparisonMode = ResponseComparisonMode.valueOf(required(mode, "mode").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("unsupported response comparison mode", ex);
        }

        HttpResponse leftResponse = response(left);
        HttpResponse rightResponse = response(right);
        var difference = comparator.compare(leftResponse, rightResponse, comparisonMode);

        return new EvidenceDifferentialRecord(
                left.sampleId(),
                right.sampleId(),
                comparisonMode,
                difference.equivalent(),
                difference.changedSignals(),
                left.method().equals(right.method()),
                left.requestUrl().equals(right.requestUrl()),
                left.responseStatus(),
                right.responseStatus()
        );
    }

    public AuthorizationContextDifferentialRecord compareAuthorization(
            UUID projectId,
            UUID leftExpectationId,
            UUID rightExpectationId
    ) throws IOException {
        AuthorizationExpectationRecord left = expectation(projectId, leftExpectationId);
        AuthorizationExpectationRecord right = expectation(projectId, rightExpectationId);
        List<String> changed = new ArrayList<>();

        compare(changed, "target", left.targetId(), right.targetId());
        compare(changed, "endpoint", left.endpoint(), right.endpoint());
        compare(changed, "action", left.action(), right.action());
        compare(changed, "principal", left.principalId(), right.principalId());
        compare(changed, "role", left.roleId(), right.roleId());
        compare(changed, "tenant", left.tenantId(), right.tenantId());
        compare(changed, "resource", left.resourceId(), right.resourceId());
        compare(changed, "expectedDecision", left.expectedDecision(), right.expectedDecision());

        return new AuthorizationContextDifferentialRecord(
                left.id(),
                right.id(),
                changed.isEmpty(),
                changed
        );
    }

    private AuthorizationExpectationRecord expectation(UUID projectId, UUID id) throws IOException {
        return contextService.expectations(projectId).stream()
                .filter(record -> record.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown authorization expectation"));
    }

    @SuppressWarnings("unchecked")
    private List<HttpEvidenceSampleRecord> harSamples(
            UUID evidenceId,
            UUID projectId,
            UUID targetId,
            String content,
            Instant createdAt
    ) {
        Object parsed = MiniJson.parse(content);
        if (!(parsed instanceof Map<?, ?> root)) throw new IllegalArgumentException("HAR root must be an object");
        Object logObject = root.get("log");
        if (!(logObject instanceof Map<?, ?> log)) throw new IllegalArgumentException("HAR log object is required");
        Object entriesObject = log.get("entries");
        if (!(entriesObject instanceof List<?> entries)) throw new IllegalArgumentException("HAR entries are required");

        List<HttpEvidenceSampleRecord> out = new ArrayList<>();
        for (Object value : entries) {
            if (!(value instanceof Map<?, ?> entry)) continue;
            Map<String, Object> request = entry.get("request") instanceof Map<?, ?> requestMap
                    ? (Map<String, Object>) requestMap : Map.of();
            Map<String, Object> response = entry.get("response") instanceof Map<?, ?> responseMap
                    ? (Map<String, Object>) responseMap : Map.of();

            String method = text(request.get("method"));
            String url = redactor.redactText(text(request.get("url")));
            String requestBody = "";
            if (request.get("postData") instanceof Map<?, ?> postData) {
                requestBody = redactor.redactText(text(postData.get("text")));
            }

            int status = response.get("status") instanceof Number number ? number.intValue() : 0;
            String contentType = "";
            String responseBody = "";
            if (response.get("content") instanceof Map<?, ?> responseContent) {
                contentType = redactor.redactText(text(responseContent.get("mimeType")));
                responseBody = redactor.redactText(text(responseContent.get("text")));
            }

            out.add(new HttpEvidenceSampleRecord(
                    UUID.randomUUID(),
                    evidenceId,
                    projectId,
                    targetId,
                    clip(method, 32),
                    clip(url, 4096),
                    status,
                    clip(contentType, 240),
                    clip(requestBody, MAX_SAMPLE_BODY),
                    clip(responseBody, MAX_SAMPLE_BODY),
                    createdAt
            ));
        }
        return List.copyOf(out);
    }

    private HttpEvidenceSampleRecord rawHttpSample(
            UUID evidenceId,
            UUID projectId,
            UUID targetId,
            TargetRecord target,
            String content,
            Instant createdAt
    ) {
        var observation = rawHttpImporter.importText(content, target.baseUri(), "standalone-evidence");
        String normalized = content.replace("\r\n", "\n");
        int separator = normalized.indexOf("\n\n");
        String body = separator < 0 ? "" : normalized.substring(separator + 2);
        return new HttpEvidenceSampleRecord(
                UUID.randomUUID(),
                evidenceId,
                projectId,
                targetId,
                observation.method().name(),
                clip(redactor.redactText(observation.uri().toString()), 4096),
                0,
                "",
                clip(redactor.redactText(body), MAX_SAMPLE_BODY),
                "",
                createdAt
        );
    }

    private static HttpResponse response(HttpEvidenceSampleRecord record) {
        return new HttpResponse(
                record.responseStatus(),
                List.of(),
                record.responseBodyRedacted().getBytes(StandardCharsets.UTF_8),
                record.responseContentType(),
                HttpProtocol.UNKNOWN,
                new byte[0]
        );
    }

    private static void compare(List<String> changed, String field, Object left, Object right) {
        if (!java.util.Objects.equals(left, right)) changed.add(field);
    }

    private static String required(String value, String field) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) throw new IllegalArgumentException(field + " is required");
        return normalized;
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String clip(String value, int max) {
        String safe = value == null ? "" : value;
        if (safe.length() <= max) return safe;
        return safe.substring(0, max);
    }
}
