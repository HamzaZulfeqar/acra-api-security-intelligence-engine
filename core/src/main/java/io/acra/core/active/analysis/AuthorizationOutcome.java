package io.acra.core.active.analysis;

public enum AuthorizationOutcome {
    ALLOW,
    DENY,
    AUTHENTICATION_REQUIRED,
    NOT_FOUND,
    PARTIAL,
    ERROR,
    UNKNOWN;

    public boolean deniedEquivalent() {
        return this == DENY || this == AUTHENTICATION_REQUIRED || this == NOT_FOUND;
    }
}
