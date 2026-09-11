package io.acra.core.active.analysis;

import io.acra.core.analysis.ResponseDifference;
import io.acra.core.analysis.semantic.ResourceSemanticMatch;

public record PairwiseDifference(
        String left,
        String right,
        ResponseDifference responseDifference,
        ResourceSemanticMatch resourceMatch,
        AuthorizationOutcome leftOutcome,
        AuthorizationOutcome rightOutcome) {
    public PairwiseDifference {
        if (left == null || left.isBlank() || right == null || right.isBlank()
                || responseDifference == null || resourceMatch == null || leftOutcome == null || rightOutcome == null) {
            throw new IllegalArgumentException("complete pairwise difference required");
        }
    }
}
