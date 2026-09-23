package io.acra.core.engine;

import io.acra.core.domain.authorization.AuthorizationPolicySnapshot;
import io.acra.core.domain.authorization.EffectiveAuthorizationRequest;

public record S6AuthorizationAnalysisRequest(
        AuthorizationAnalysisRequest s5Request,
        AuthorizationPolicySnapshot policySnapshot,
        EffectiveAuthorizationRequest effectiveRequest,
        boolean privilegedAction,
        String requiredRoleId) {

    public S6AuthorizationAnalysisRequest {
        if (s5Request == null) throw new IllegalArgumentException("s5Request required");
        if (policySnapshot == null) throw new IllegalArgumentException("policySnapshot required");
        if (effectiveRequest == null) throw new IllegalArgumentException("effectiveRequest required");
        requiredRoleId = requiredRoleId == null ? "" : requiredRoleId;
    }
}
