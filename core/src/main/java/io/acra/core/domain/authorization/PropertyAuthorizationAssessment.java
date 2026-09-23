package io.acra.core.domain.authorization;

import io.acra.core.engine.PolicyValidationEvaluator;
import io.acra.core.security.UniversalRedactor;
import java.util.List;

/** Typed property-level authorization assessment. */
public record PropertyAuthorizationAssessment(
        String assessmentId,
        String observationId,
        String executionId,
        String testId,
        String principalId,
        String roleId,
        String tenantId,
        String endpoint,
        String property,
        PolicyValidationEvaluator.PropertyOperation operation,
        AuthorizationDecision expectedDecision,
        AuthorizationDecision observedDecision,
        PolicyValidationState state,
        String confidence,
        List<String> evidenceIds,
        String rationale,
        List<String> reasons) {
    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public PropertyAuthorizationAssessment {
        assessmentId = safe(assessmentId);
        observationId = safe(observationId);
        executionId = safe(executionId);
        testId = safe(testId);
        principalId = safe(principalId);
        roleId = safe(roleId);
        tenantId = safe(tenantId);
        endpoint = safe(endpoint);
        property = safe(property);
        expectedDecision = expectedDecision == null ? AuthorizationDecision.UNKNOWN : expectedDecision;
        observedDecision = observedDecision == null ? AuthorizationDecision.UNKNOWN : observedDecision;
        state = state == null ? PolicyValidationState.INCONCLUSIVE : state;
        confidence = safe(confidence);
        evidenceIds = safe(evidenceIds);
        rationale = safe(rationale);
        reasons = safe(reasons);
    }

    private static String safe(String value) {
        return REDACTOR.redactText(value == null ? "" : value);
    }

    private static List<String> safe(List<String> values) {
        return List.copyOf(values == null ? List.<String>of() : values).stream()
                .map(PropertyAuthorizationAssessment::safe).distinct().sorted().toList();
    }
}
