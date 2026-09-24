package io.acra.core.oauth;

import java.util.List;

public record OAuthEvidenceBinding(
        String projectId,
        String executionId,
        String testId,
        String requestId,
        String observationObjectId,
        List<String> evidenceObjectIds) {

    public OAuthEvidenceBinding {
        projectId = required(projectId, "projectId");
        executionId = required(executionId, "executionId");
        testId = required(testId, "testId");
        requestId = required(requestId, "requestId");
        observationObjectId = required(observationObjectId, "observationObjectId");
        evidenceObjectIds = List.copyOf(evidenceObjectIds == null ? List.of() : evidenceObjectIds);
        if (evidenceObjectIds.isEmpty()) throw new IllegalArgumentException("evidenceObjectIds required");
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " required");
        return value.strip();
    }
}
