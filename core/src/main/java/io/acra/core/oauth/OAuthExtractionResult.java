package io.acra.core.oauth;

import io.acra.core.domain.evidence.Evidence;
import java.util.List;

public record OAuthExtractionResult(
        OAuthContextObservation observation,
        List<Evidence> evidence) {

    public OAuthExtractionResult {
        if (observation == null) throw new IllegalArgumentException("observation required");
        evidence = List.copyOf(evidence == null ? List.of() : evidence);
        if (evidence.isEmpty()) throw new IllegalArgumentException("evidence required");
    }
}
