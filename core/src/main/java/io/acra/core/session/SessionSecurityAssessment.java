package io.acra.core.session;

import java.util.List;

public record SessionSecurityAssessment(
        String assessmentId,
        String sessionId,
        String previousObservationId,
        String currentObservationId,
        SessionSecurityAssessmentState state,
        boolean tokenRotated,
        boolean identityVerified,
        List<SessionContextDimension> driftDimensions,
        List<String> evidenceIds,
        String rationale,
        List<String> reasons) {

    public SessionSecurityAssessment {
        if (assessmentId == null || assessmentId.isBlank()) throw new IllegalArgumentException("assessmentId required");
        sessionId = sessionId == null ? "" : sessionId;
        previousObservationId = previousObservationId == null ? "" : previousObservationId;
        currentObservationId = currentObservationId == null ? "" : currentObservationId;
        state = state == null ? SessionSecurityAssessmentState.INCONCLUSIVE : state;
        driftDimensions = List.copyOf(driftDimensions == null ? List.of() : driftDimensions);
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
        rationale = rationale == null ? "" : rationale;
        reasons = List.copyOf(reasons == null ? List.of() : reasons);
    }

    public boolean reviewCandidate() {
        return state == SessionSecurityAssessmentState.CANDIDATE
                && identityVerified
                && !driftDimensions.isEmpty();
    }
}
