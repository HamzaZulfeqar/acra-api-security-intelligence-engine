package io.acra.core.session;

public enum SessionCorrelationState {
    BASELINE,
    STABLE,
    TOKEN_ROTATED,
    CONTEXT_DRIFT,
    UNVERIFIED_IDENTITY,
    INCONCLUSIVE
}
