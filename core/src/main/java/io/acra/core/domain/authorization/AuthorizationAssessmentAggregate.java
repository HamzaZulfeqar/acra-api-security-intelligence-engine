package io.acra.core.domain.authorization;

import java.util.List;
import io.acra.core.security.UniversalRedactor;

/** Correlated authorization assessments. Not a confirmed vulnerability finding. */
public record AuthorizationAssessmentAggregate(
        String correlationId,
        List<String> assessmentIds,
        List<String> observationIds,
        List<String> executionIds,
        List<String> evidenceIds,
        CorrelationState state,
        String confidence,
        String rationale,
        List<String> conflicts) {
    private static final UniversalRedactor REDACTOR = new UniversalRedactor();
    public AuthorizationAssessmentAggregate {
        correlationId = REDACTOR.redactText(correlationId);
        assessmentIds = safe(assessmentIds);
        observationIds = safe(observationIds);
        executionIds = safe(executionIds);
        evidenceIds = safe(evidenceIds);
        conflicts = safe(conflicts);
        state = state == null ? CorrelationState.INSUFFICIENT : state;
        confidence = confidence == null ? "INSUFFICIENT" : REDACTOR.redactText(confidence);
        rationale = REDACTOR.redactText(rationale);
    }
    private static List<String> safe(List<String> values) {
        return List.copyOf(values == null ? List.<String>of() : values).stream()
                .map(REDACTOR::redactText).distinct().sorted().toList();
    }
}
