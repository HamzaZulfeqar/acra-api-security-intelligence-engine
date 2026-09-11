package io.acra.core.active.analysis;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.common.Validation;
import java.util.List;

public record ExpectedDecisionCandidate(
        AuthorizationDecision decision,
        ExpectedDecisionSource source,
        String policyId,
        List<String> evidenceIds,
        double confidence) {
    public ExpectedDecisionCandidate {
        if (decision == null || source == null) throw new IllegalArgumentException("decision and source required");
        policyId = Validation.requireNonBlank(policyId, "policyId");
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
        if (confidence < 0.0 || confidence > 1.0) throw new IllegalArgumentException("confidence");
    }
}
