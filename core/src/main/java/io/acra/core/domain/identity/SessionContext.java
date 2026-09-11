package io.acra.core.domain.identity;

import io.acra.core.domain.common.*;
import java.time.Instant;

public record SessionContext(String sessionId, String principalId, AuthenticationType authenticationMethod,
                             String tokenFingerprint, Instant createdAt, Instant lastObservedAt, SessionState state) {
    public SessionContext {
        sessionId = Validation.requireNonBlank(sessionId, "sessionId");
        principalId = principalId == null ? "" : principalId;
        if (authenticationMethod == null) authenticationMethod = AuthenticationType.UNKNOWN;
        tokenFingerprint = tokenFingerprint == null ? "" : tokenFingerprint;
        if (createdAt == null || lastObservedAt == null) throw new DomainValidationException("session timestamps are required");
        if (state == null) state = SessionState.UNKNOWN;
    }
}
