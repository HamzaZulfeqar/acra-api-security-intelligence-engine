package io.acra.core.reference;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.security.UniversalRedactor;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public record IndirectReferenceResolution(
        String resolutionId,
        String sourceObservationId,
        String executionId,
        String testId,
        String endpoint,
        String referenceFingerprint,
        String referenceKind,
        String resolvedResourceId,
        String action,
        AuthorizationDecision observedDecision,
        IndirectReferenceSource source,
        List<String> evidenceIds) {

    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public IndirectReferenceResolution {
        resolutionId = requiredSafe(resolutionId, "resolutionId");
        sourceObservationId = requiredSafe(sourceObservationId, "sourceObservationId");
        executionId = requiredSafe(executionId, "executionId");
        testId = requiredSafe(testId, "testId");
        endpoint = requiredPath(endpoint);
        referenceFingerprint = requiredSafe(referenceFingerprint, "referenceFingerprint").toLowerCase(Locale.ROOT);
        if (!referenceFingerprint.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("referenceFingerprint must be SHA-256 hex");
        }
        referenceKind = requiredSafe(referenceKind, "referenceKind");
        resolvedResourceId = requiredSafe(resolvedResourceId, "resolvedResourceId");
        action = requiredSafe(action, "action");
        observedDecision = observedDecision == null ? AuthorizationDecision.UNKNOWN : observedDecision;
        source = source == null ? IndirectReferenceSource.OBSERVED : source;
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds).stream()
                .map(value -> requiredSafe(value, "evidenceId"))
                .toList();
        if (evidenceIds.isEmpty()) throw new IllegalArgumentException("evidenceIds required");
        if (new LinkedHashSet<>(evidenceIds).size() != evidenceIds.size()) {
            throw new IllegalArgumentException("duplicate evidenceIds");
        }
    }

    private static String requiredPath(String value) {
        String path = requiredSafe(value, "endpoint");
        if (!path.startsWith("/") || path.contains("?") || path.contains("#")) {
            throw new IllegalArgumentException("endpoint must be an absolute path without query or fragment");
        }
        return path;
    }

    private static String requiredSafe(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " required");
        String normalized = value.strip();
        if (!normalized.equals(REDACTOR.redactText(normalized))) {
            throw new IllegalArgumentException(name + " contains sensitive material");
        }
        return normalized;
    }
}
