package io.acra.core.domain.finding;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.security.UniversalRedactor;
import java.util.List;

/** Evidence-bound authorization finding candidate. This is not a confirmed finding. */
public record FindingCandidate(
        String candidateId,
        FindingFingerprint fingerprint,
        String category,
        String observationId,
        String executionId,
        String testId,
        String projectId,
        String principalId,
        String resourceId,
        String endpoint,
        AuthorizationDecision expectedDecision,
        AuthorizationDecision observedDecision,
        FindingCandidateState state,
        String confidence,
        List<String> assessmentIds,
        List<String> evidenceIds,
        String rationale,
        List<String> reasons) {
    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public FindingCandidate {
        candidateId = safe(candidateId);
        category = safe(category);
        observationId = safe(observationId);
        executionId = safe(executionId);
        testId = safe(testId);
        projectId = safe(projectId);
        principalId = safe(principalId);
        resourceId = safe(resourceId);
        endpoint = safe(endpoint);
        expectedDecision = expectedDecision == null ? AuthorizationDecision.UNKNOWN : expectedDecision;
        observedDecision = observedDecision == null ? AuthorizationDecision.UNKNOWN : observedDecision;
        state = state == null ? FindingCandidateState.INCONCLUSIVE : state;
        confidence = safe(confidence);
        assessmentIds = safe(assessmentIds);
        evidenceIds = safe(evidenceIds);
        rationale = safe(rationale);
        reasons = safe(reasons);
        if (fingerprint == null) {
            fingerprint = FindingFingerprint.of(endpoint, resourceId, principalId, "UNKNOWN", category, "authorization");
        }
    }

    private static String safe(String value) {
        return REDACTOR.redactText(value == null ? "" : value);
    }

    private static List<String> safe(List<String> values) {
        return List.copyOf(values == null ? List.<String>of() : values).stream()
                .map(FindingCandidate::safe).distinct().sorted().toList();
    }
}
