package io.acra.core.domain.authorization;

import io.acra.core.security.UniversalRedactor;
import java.util.List;

public record AuthorizationCorrelationEnvelope(
        AuthorizationAssessmentAggregate aggregate,
        String projectId,
        String policyReference,
        List<String> testIds,
        List<String> propertyReferences,
        List<String> policyReviewIds,
        int independentExecutionCount,
        boolean independentlyCorroborated) {

    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public AuthorizationCorrelationEnvelope {
        projectId = safe(projectId);
        policyReference = safe(policyReference);
        testIds = safe(testIds);
        propertyReferences = safe(propertyReferences);
        policyReviewIds = safe(policyReviewIds);
        if (independentExecutionCount < 0) throw new IllegalArgumentException("independentExecutionCount");
        if (independentlyCorroborated && independentExecutionCount < 2) {
            throw new IllegalArgumentException("corroboration requires at least two independent executions");
        }
    }

    private static String safe(String value) {
        return REDACTOR.redactText(value == null ? "" : value);
    }

    private static List<String> safe(List<String> values) {
        return List.copyOf(values == null ? List.<String>of() : values).stream()
                .map(AuthorizationCorrelationEnvelope::safe).filter(value -> !value.isBlank()).distinct().sorted().toList();
    }
}
