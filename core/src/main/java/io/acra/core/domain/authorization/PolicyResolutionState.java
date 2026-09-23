package io.acra.core.domain.authorization;

public enum PolicyResolutionState {
    RESOLVED_ALLOW,
    RESOLVED_DENY,
    CONFLICTING,
    INCOMPLETE,
    UNKNOWN,
    NOT_APPLICABLE
}
