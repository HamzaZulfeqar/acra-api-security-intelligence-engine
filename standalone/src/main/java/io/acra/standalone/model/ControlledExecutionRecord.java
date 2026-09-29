package io.acra.standalone.model;

import io.acra.core.active.analysis.AuthorizationOutcome;
import io.acra.core.active.analysis.DifferentialClassification;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.testing.TestState;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ControlledExecutionRecord(
        UUID runId,
        UUID projectId,
        UUID targetId,
        UUID expectationId,
        String testId,
        String executionId,
        String observationId,
        String endpoint,
        String requestPath,
        String mutatedPath,
        AuthorizationDecision expectedDecision,
        AuthorizationOutcome observedDecision,
        DifferentialClassification differentialClassification,
        TestState state,
        UUID evidenceArtifactId,
        int coreEvidenceObjectCount,
        Instant createdAt
) {
    public ControlledExecutionRecord {
        Objects.requireNonNull(runId, "runId");
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(targetId, "targetId");
        Objects.requireNonNull(expectationId, "expectationId");
        testId = required(testId, "testId");
        executionId = required(executionId, "executionId");
        observationId = normalize(observationId);
        endpoint = required(endpoint, "endpoint");
        requestPath = required(requestPath, "requestPath");
        mutatedPath = required(mutatedPath, "mutatedPath");
        Objects.requireNonNull(expectedDecision, "expectedDecision");
        Objects.requireNonNull(observedDecision, "observedDecision");
        Objects.requireNonNull(differentialClassification, "differentialClassification");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(evidenceArtifactId, "evidenceArtifactId");
        if (coreEvidenceObjectCount < 0) throw new IllegalArgumentException("coreEvidenceObjectCount must be non-negative");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    private static String required(String value, String field) {
        String normalized = normalize(value);
        if (normalized.isBlank()) throw new IllegalArgumentException(field + " is required");
        return normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
