package io.acra.core.reporting.s10;

import io.acra.core.coverage.S10CoverageFamily;
import io.acra.core.domain.authorization.AuthorizationDecision;
import java.util.List;

public record S10ReportObservation(
        S10CoverageFamily family,
        String observationId,
        String executionId,
        String testId,
        String endpoint,
        String resourceId,
        String action,
        AuthorizationDecision observedDecision,
        String referenceFingerprint,
        List<String> evidenceIds) {

    public S10ReportObservation {
        if (family == null) throw new IllegalArgumentException("family required");
        observationId = required(observationId, "observationId");
        executionId = required(executionId, "executionId");
        testId = required(testId, "testId");
        endpoint = required(endpoint, "endpoint");
        resourceId = required(resourceId, "resourceId");
        action = required(action, "action");
        observedDecision = observedDecision == null ? AuthorizationDecision.UNKNOWN : observedDecision;
        referenceFingerprint = referenceFingerprint == null ? "" : referenceFingerprint.strip();
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " required");
        return value.strip();
    }
}
