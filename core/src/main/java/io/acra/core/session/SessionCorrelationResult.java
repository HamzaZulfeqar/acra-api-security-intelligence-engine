package io.acra.core.session;

import java.util.List;

public record SessionCorrelationResult(
        String correlationId,
        String sessionId,
        String previousObservationId,
        String currentObservationId,
        SessionCorrelationState state,
        boolean previousIdentityVerified,
        boolean currentIdentityVerified,
        boolean tokenRotated,
        List<SessionContextDimension> driftDimensions,
        List<String> evidenceIds,
        List<String> reasons) {

    public SessionCorrelationResult {
        if (correlationId == null || correlationId.isBlank()) {
            throw new IllegalArgumentException("correlationId required");
        }
        sessionId = sessionId == null ? "" : sessionId;
        previousObservationId = previousObservationId == null ? "" : previousObservationId;
        currentObservationId = currentObservationId == null ? "" : currentObservationId;
        state = state == null ? SessionCorrelationState.INCONCLUSIVE : state;
        driftDimensions = List.copyOf(driftDimensions == null ? List.of() : driftDimensions);
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
        reasons = List.copyOf(reasons == null ? List.of() : reasons);
    }

    public boolean verifiedContextDrift() {
        return state == SessionCorrelationState.CONTEXT_DRIFT
                && previousIdentityVerified
                && currentIdentityVerified
                && !driftDimensions.isEmpty();
    }
}
