package io.acra.core.domain.authorization;

public enum AuthorizationScopeType {
    GLOBAL,
    TENANT,
    RESOURCE,
    ENDPOINT,
    FUNCTION,
    PROPERTY,
    SHARED,
    UNKNOWN
}
