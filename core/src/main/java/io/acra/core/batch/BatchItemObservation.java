package io.acra.core.batch;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.security.UniversalRedactor;
import java.util.LinkedHashSet;
import java.util.List;

public record BatchItemObservation(
        String itemObservationId,
        String sourceObservationId,
        String executionId,
        String testId,
        String batchId,
        String itemKey,
        String endpoint,
        String resourceId,
        String action,
        AuthorizationDecision observedDecision,
        List<String> evidenceIds) {

    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public BatchItemObservation {
        itemObservationId = requiredSafe(itemObservationId, "itemObservationId");
        sourceObservationId = requiredSafe(sourceObservationId, "sourceObservationId");
        executionId = requiredSafe(executionId, "executionId");
        testId = requiredSafe(testId, "testId");
        batchId = requiredSafe(batchId, "batchId");
        itemKey = requiredSafe(itemKey, "itemKey");
        endpoint = requiredPath(endpoint);
        resourceId = requiredSafe(resourceId, "resourceId");
        action = requiredSafe(action, "action");
        observedDecision = observedDecision == null ? AuthorizationDecision.UNKNOWN : observedDecision;
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
