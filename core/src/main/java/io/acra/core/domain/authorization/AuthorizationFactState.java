package io.acra.core.domain.authorization;

public enum AuthorizationFactState {
    VERIFIED,
    MISSING,
    CONFLICTING,
    REDACTED,
    UNKNOWN
}
