package io.acra.core.active.analysis;

import io.acra.core.domain.authorization.AuthorizationDecision;
import java.util.Comparator;
import java.util.List;

public final class ExpectedDecisionResolver {
    public ExpectedDecisionResolution resolve(List<ExpectedDecisionCandidate> candidates) {
        List<ExpectedDecisionCandidate> ordered = (candidates == null ? List.<ExpectedDecisionCandidate>of() : candidates)
                .stream().filter(candidate -> candidate.decision() != AuthorizationDecision.UNKNOWN)
                .sorted(Comparator.comparingInt(candidate -> candidate.source().precedence())).toList();
        if (ordered.isEmpty()) {
            return new ExpectedDecisionResolution(AuthorizationDecision.UNKNOWN, ExpectedDecisionSource.UNKNOWN,
                    "UNKNOWN", List.of(), 0.0, false);
        }
        ExpectedDecisionCandidate selected = ordered.getFirst();
        boolean conflict = ordered.stream()
                .filter(candidate -> candidate.source() == selected.source())
                .anyMatch(candidate -> candidate.decision() != selected.decision());
        if (conflict) {
            return new ExpectedDecisionResolution(AuthorizationDecision.UNKNOWN, selected.source(),
                    selected.policyId(), selected.evidenceIds(), 0.0, true);
        }
        return new ExpectedDecisionResolution(selected.decision(), selected.source(), selected.policyId(),
                selected.evidenceIds(), selected.confidence(), false);
    }
}
