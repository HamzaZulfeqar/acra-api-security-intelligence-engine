package io.acra.core.property;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.engine.PolicyValidationEvaluator;
import io.acra.core.security.UniversalRedactor;
import java.util.LinkedHashSet;
import java.util.List;

public record PropertyAccessObservation(
        String observationId,
        String executionId,
        String testId,
        String endpoint,
        String property,
        PolicyValidationEvaluator.PropertyOperation operation,
        AuthorizationDecision observedDecision,
        List<String> evidenceIds) {

    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public PropertyAccessObservation {
        observationId = requiredSafe(observationId, "observationId");
        executionId = requiredSafe(executionId, "executionId");
        testId = requiredSafe(testId, "testId");
        endpoint = requiredSafe(endpoint, "endpoint");
        property = requiredSafe(property, "property");
        if (!endpoint.startsWith("/") || endpoint.contains("?") || endpoint.contains("#")) {
            throw new IllegalArgumentException("endpoint must be an absolute path without query or fragment");
        }
        if (operation == null) throw new IllegalArgumentException("operation required");
        observedDecision = observedDecision == null ? AuthorizationDecision.UNKNOWN : observedDecision;
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds).stream()
                .map(value -> requiredSafe(value, "evidenceId"))
                .toList();
        if (evidenceIds.isEmpty()) throw new IllegalArgumentException("evidenceIds required");
        if (new LinkedHashSet<>(evidenceIds).size() != evidenceIds.size()) {
            throw new IllegalArgumentException("duplicate evidenceIds");
        }
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
