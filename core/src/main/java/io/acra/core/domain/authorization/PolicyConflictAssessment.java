package io.acra.core.domain.authorization;

import java.util.List;

public record PolicyConflictAssessment(
        String assessmentId,
        boolean conflicting,
        PolicyResolutionState resolutionState,
        List<String> reasons,
        List<String> evidenceIds) {

    public PolicyConflictAssessment {
        resolutionState = resolutionState == null ? PolicyResolutionState.UNKNOWN : resolutionState;
        reasons = List.copyOf(reasons == null ? List.of() : reasons);
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
    }
}
