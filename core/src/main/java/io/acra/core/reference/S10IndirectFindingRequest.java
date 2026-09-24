package io.acra.core.reference;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.security.UniversalRedactor;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public record S10IndirectFindingRequest(
        String projectId,
        String testId,
        String executionId,
        String observationId,
        String endpoint,
        String referenceFingerprint,
        String referenceKind,
        String resolvedResourceId,
        String action,
        String principalId,
        String tenantId,
        String policyReference,
        AuthorizationDecision expectedDecision,
        AuthorizationDecision observedDecision,
        List<String> evidenceIds) {

    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public S10IndirectFindingRequest {
        projectId = required(projectId, "projectId");
        testId = required(testId, "testId");
        executionId = required(executionId, "executionId");
        observationId = required(observationId, "observationId");
        endpoint = requiredPath(endpoint);
        referenceFingerprint = required(referenceFingerprint, "referenceFingerprint").toLowerCase(Locale.ROOT);
        if (!referenceFingerprint.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("referenceFingerprint must be SHA-256 hex");
        }
        referenceKind = required(referenceKind, "referenceKind");
        resolvedResourceId = required(resolvedResourceId, "resolvedResourceId");
        action = required(action, "action");
        principalId = required(principalId, "principalId");
        tenantId = required(tenantId, "tenantId");
        policyReference = required(policyReference, "policyReference");
        expectedDecision = expectedDecision == null ? AuthorizationDecision.UNKNOWN : expectedDecision;
        observedDecision = observedDecision == null ? AuthorizationDecision.UNKNOWN : observedDecision;
        evidenceIds = safeEvidence(evidenceIds);
    }

    private static List<String> safeEvidence(List<String> values) {
        List<String> safe = List.copyOf(values == null ? List.of() : values).stream()
                .map(value -> required(value, "evidenceId"))
                .toList();
        if (safe.isEmpty()) throw new IllegalArgumentException("evidenceIds required");
        if (new LinkedHashSet<>(safe).size() != safe.size()) {
            throw new IllegalArgumentException("duplicate evidenceIds");
        }
        return safe;
    }

    private static String requiredPath(String value) {
        String path = required(value, "endpoint");
        if (!path.startsWith("/") || path.contains("?") || path.contains("#")) {
            throw new IllegalArgumentException("endpoint must be an absolute path without query or fragment");
        }
        return path;
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " required");
        String normalized = value.strip();
        if (!normalized.equals(REDACTOR.redactText(normalized))) {
            throw new IllegalArgumentException(name + " contains sensitive material");
        }
        return normalized;
    }
}
