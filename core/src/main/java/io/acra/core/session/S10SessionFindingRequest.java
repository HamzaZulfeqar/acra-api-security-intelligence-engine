package io.acra.core.session;

import io.acra.core.security.UniversalRedactor;
import java.util.List;

public record S10SessionFindingRequest(
        String projectId,
        String testId,
        String executionId,
        String observationId,
        String endpoint,
        String principalId,
        String tenantId,
        String ruleReference,
        List<String> evidenceIds) {

    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public S10SessionFindingRequest {
        projectId = required(projectId, "projectId");
        testId = required(testId, "testId");
        executionId = required(executionId, "executionId");
        observationId = required(observationId, "observationId");
        endpoint = required(endpoint, "endpoint");
        principalId = required(principalId, "principalId");
        tenantId = required(tenantId, "tenantId");
        ruleReference = required(ruleReference, "ruleReference");
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds).stream()
                .map(value -> required(value, "evidenceId"))
                .distinct()
                .sorted()
                .toList();
        if (evidenceIds.isEmpty()) throw new IllegalArgumentException("evidenceIds required");
    }

    public SessionEvidenceBinding evidenceBinding() {
        return new SessionEvidenceBinding(projectId, executionId, testId, observationId, evidenceIds);
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
