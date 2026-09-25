package io.acra.core.reporting.s10;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.FindingCandidateState;
import java.util.List;

public record S10ReportFindingCandidate(
        String candidateId,
        FindingCandidateState state,
        String endpoint,
        String resourceId,
        AuthorizationDecision expectedDecision,
        AuthorizationDecision observedDecision,
        String confidence,
        List<String> policyReferences,
        List<String> dimensions,
        List<String> evidenceIds) {

    public S10ReportFindingCandidate {
        candidateId = required(candidateId, "candidateId");
        state = state == null ? FindingCandidateState.INCONCLUSIVE : state;
        endpoint = required(endpoint, "endpoint");
        resourceId = required(resourceId, "resourceId");
        expectedDecision = expectedDecision == null ? AuthorizationDecision.UNKNOWN : expectedDecision;
        observedDecision = observedDecision == null ? AuthorizationDecision.UNKNOWN : observedDecision;
        confidence = confidence == null || confidence.isBlank() ? "INSUFFICIENT" : confidence.strip();
        policyReferences = List.copyOf(policyReferences == null ? List.of() : policyReferences);
        dimensions = List.copyOf(dimensions == null ? List.of() : dimensions);
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " required");
        return value.strip();
    }
}
