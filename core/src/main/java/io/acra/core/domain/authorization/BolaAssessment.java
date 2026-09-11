package io.acra.core.domain.authorization;

import java.util.List;
import io.acra.core.security.UniversalRedactor;

/** Structured BOLA reasoning result. This is not a confirmed vulnerability finding. */
public record BolaAssessment(
        String assessmentId,
        String observationId,
        String executionId,
        String testId,
        String principalId,
        String resourceId,
        String ownerPrincipalId,
        String action,
        AuthorizationDecision expectedDecision,
        AuthorizationDecision observedDecision,
        BolaAssessmentStatus status,
        BolaConfidence confidence,
        List<String> evidenceIds,
        String rationale) {
    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public BolaAssessment {
        assessmentId = safe(assessmentId);
        observationId = safe(observationId);
        executionId = safe(executionId);
        testId = safe(testId);
        principalId = safe(principalId);
        resourceId = safe(resourceId);
        ownerPrincipalId = safe(ownerPrincipalId);
        action = safe(action);
        evidenceIds = List.copyOf(evidenceIds == null ? List.<String>of() : evidenceIds).stream()
                .map(BolaAssessment::safe).toList();
        status = status == null ? BolaAssessmentStatus.INCONCLUSIVE : status;
        confidence = confidence == null ? BolaConfidence.INSUFFICIENT : confidence;
        rationale = safe(rationale);
    }

    private static String safe(String value) { return REDACTOR.redactText(value); }
}
