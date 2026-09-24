package io.acra.core.session;

import java.util.List;

public record SessionEvidenceBinding(
        String projectId,
        String executionId,
        String testId,
        String observationObjectId,
        List<String> evidenceObjectIds) {

    public SessionEvidenceBinding {
        projectId = required(projectId, "projectId");
        executionId = required(executionId, "executionId");
        testId = required(testId, "testId");
        observationObjectId = required(observationObjectId, "observationObjectId");
        evidenceObjectIds = List.copyOf(evidenceObjectIds == null ? List.of() : evidenceObjectIds);
        if (evidenceObjectIds.isEmpty()) throw new IllegalArgumentException("evidenceObjectIds required");
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " required");
        return value.strip();
    }
}
