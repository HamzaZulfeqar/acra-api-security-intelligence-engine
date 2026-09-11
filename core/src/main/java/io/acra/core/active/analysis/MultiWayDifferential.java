package io.acra.core.active.analysis;

import java.util.List;

public record MultiWayDifferential(
        AuthorizationOutcome baselineOutcome,
        AuthorizationOutcome positiveControlOutcome,
        AuthorizationOutcome negativeControlOutcome,
        AuthorizationOutcome mutationOutcome,
        List<PairwiseDifference> differences,
        DifferentialClassification classification,
        List<String> reasons) {
    public MultiWayDifferential {
        if (baselineOutcome == null || positiveControlOutcome == null || negativeControlOutcome == null
                || mutationOutcome == null || classification == null) {
            throw new IllegalArgumentException("differential outcomes required");
        }
        differences = List.copyOf(differences == null ? List.of() : differences);
        reasons = List.copyOf(reasons == null ? List.of() : reasons);
    }
}
