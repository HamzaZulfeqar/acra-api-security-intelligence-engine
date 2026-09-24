package io.acra.core.oauth;

import java.util.List;

public record OAuthContextCorrelation(
        String correlationId,
        String previousObservationId,
        String currentObservationId,
        OAuthContextCorrelationState state,
        List<OAuthContextDimension> driftDimensions,
        List<String> evidenceIds,
        List<String> reasons) {

    public OAuthContextCorrelation {
        if (correlationId == null || correlationId.isBlank()) {
            throw new IllegalArgumentException("correlationId required");
        }
        previousObservationId = previousObservationId == null ? "" : previousObservationId;
        currentObservationId = currentObservationId == null ? "" : currentObservationId;
        state = state == null ? OAuthContextCorrelationState.INCONCLUSIVE : state;
        driftDimensions = List.copyOf(driftDimensions == null ? List.of() : driftDimensions);
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
        reasons = List.copyOf(reasons == null ? List.of() : reasons);
    }

    public boolean driftObserved() {
        return state == OAuthContextCorrelationState.CONTEXT_DRIFT && !driftDimensions.isEmpty();
    }
}
