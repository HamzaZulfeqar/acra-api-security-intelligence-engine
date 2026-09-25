package io.acra.core.domain.authorization;

import java.util.List;

public record PolicyGraphIntegrationResult(
        PolicyGraphIntegrationState state,
        int evidenceAdded,
        int nodesAdded,
        int edgesAdded,
        List<String> reasons) {

    public PolicyGraphIntegrationResult {
        state = state == null ? PolicyGraphIntegrationState.INCONCLUSIVE : state;
        if (evidenceAdded < 0 || nodesAdded < 0 || edgesAdded < 0) throw new IllegalArgumentException("negative count");
        reasons = List.copyOf(reasons == null ? List.of() : reasons);
    }
}
