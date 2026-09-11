package io.acra.core.domain.authorization;

import java.util.List;
import io.acra.core.security.UniversalRedactor;

/** Function-level authorization assessment. Not a confirmed vulnerability finding. */
public record BflaAssessment(
        String assessmentId,
        String observationId,
        String executionId,
        String testId,
        String principalId,
        String roleId,
        String action,
        String endpoint,
        AuthorizationDecision expectedDecision,
        AuthorizationDecision observedDecision,
        BflaAssessmentStatus status,
        BflaConfidence confidence,
        List<String> evidenceIds,
        String rationale) {
    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public BflaAssessment {
        assessmentId = safe(assessmentId);
        observationId = safe(observationId);
        executionId = safe(executionId);
        testId = safe(testId);
        principalId = safe(principalId);
        roleId = safe(roleId);
        action = safe(action);
        endpoint = safe(endpoint);
        evidenceIds = List.copyOf(evidenceIds == null ? List.<String>of() : evidenceIds).stream()
                .map(BflaAssessment::safe).toList();
        status = status == null ? BflaAssessmentStatus.INCONCLUSIVE : status;
        confidence = confidence == null ? BflaConfidence.INSUFFICIENT : confidence;
        rationale = safe(rationale);
    }

    private static String safe(String value) { return REDACTOR.redactText(value); }
}
