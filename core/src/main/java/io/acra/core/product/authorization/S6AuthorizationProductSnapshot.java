package io.acra.core.product.authorization;

import io.acra.core.domain.authorization.AuthorizationPolicySnapshot;
import io.acra.core.domain.authorization.S6AuthorizationAnalysisResult;
import java.util.List;

public record S6AuthorizationProductSnapshot(
        AuthorizationPolicySnapshot policy,
        List<S6AuthorizationAnalysisResult> analyses) {

    public S6AuthorizationProductSnapshot {
        analyses = List.copyOf(analyses == null ? List.of() : analyses);
    }

    public boolean hasPolicy() {
        return policy != null;
    }
}
