package io.acra.core.domain.finding;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.security.UniversalRedactor;
import java.util.List;

public record FindingCandidate(
        String candidateId,
        FindingCandidateState state,
        String projectId,
        List<String> testIds,
        List<String> executionIds,
        List<String> observationIds,
        List<String> assessmentIds,
        List<String> dimensions,
        String endpoint,
        String resourceId,
        String principalId,
        String tenantRelationship,
        AuthorizationDecision expectedDecision,
        AuthorizationDecision observedDecision,
        List<String> supportingEvidenceIds,
        List<String> contradictoryEvidence,
        List<String> policyReferences,
        String confidence,
        String rationale,
        FindingFingerprint fingerprint) {

    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public FindingCandidate {
        candidateId = safe(candidateId);
        state = state == null ? FindingCandidateState.INCONCLUSIVE : state;
        projectId = safe(projectId);
        testIds = safe(testIds);
        executionIds = safe(executionIds);
        observationIds = safe(observationIds);
        assessmentIds = safe(assessmentIds);
        dimensions = safe(dimensions);
        endpoint = safe(endpoint);
        resourceId = safe(resourceId);
        principalId = safe(principalId);
        tenantRelationship = safe(tenantRelationship);
        expectedDecision = expectedDecision == null ? AuthorizationDecision.UNKNOWN : expectedDecision;
        observedDecision = observedDecision == null ? AuthorizationDecision.UNKNOWN : observedDecision;
        supportingEvidenceIds = safe(supportingEvidenceIds);
        contradictoryEvidence = safe(contradictoryEvidence);
        policyReferences = safe(policyReferences);
        confidence = safe(confidence);
        rationale = safe(rationale);
    }

    private static String safe(String value) {
        return REDACTOR.redactText(value == null ? "" : value);
    }

    private static List<String> safe(List<String> values) {
        return List.copyOf(values == null ? List.<String>of() : values).stream()
                .map(FindingCandidate::safe).filter(value -> !value.isBlank()).distinct().sorted().toList();
    }
}
