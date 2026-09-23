package io.acra.core.domain.authorization;

public enum TenantRelationship {
    SAME_TENANT,
    CROSS_TENANT,
    GLOBAL,
    SHARED,
    DELEGATED,
    UNKNOWN,
    CONFLICTING
}
