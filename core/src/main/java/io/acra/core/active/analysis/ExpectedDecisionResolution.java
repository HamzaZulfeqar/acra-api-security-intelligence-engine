package io.acra.core.active.analysis;

import io.acra.core.domain.authorization.AuthorizationDecision;
import java.util.List;

public record ExpectedDecisionResolution(
        AuthorizationDecision decision,
        ExpectedDecisionSource source,
        String policyId,
        List<String> evidenceIds,
        double confidence,
        boolean conflict) {
    public ExpectedDecisionResolution {
        if (decision == null || source == null) throw new IllegalArgumentException("decision/source required");
        policyId = policyId == null ? "UNKNOWN" : policyId;
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
        if (confidence < 0.0 || confidence > 1.0) throw new IllegalArgumentException("confidence");
    }
}
