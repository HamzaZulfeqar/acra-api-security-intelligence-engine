package io.acra.core.active.model;

public enum ExecutionEnvironment {
    LAB,
    AUTHORIZED_DEV,
    AUTHORIZED_QA,
    AUTHORIZED_STAGING,
    UNKNOWN,
    OUT_OF_SCOPE;

    public boolean executableByDefault() {
        return this == LAB;
    }

    public boolean requiresExplicitAuthorization() {
        return this == AUTHORIZED_DEV || this == AUTHORIZED_QA || this == AUTHORIZED_STAGING;
    }
}
