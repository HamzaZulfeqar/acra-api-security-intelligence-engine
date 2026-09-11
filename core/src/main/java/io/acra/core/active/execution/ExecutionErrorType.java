package io.acra.core.active.execution;

public enum ExecutionErrorType {
    TIMEOUT,
    CONNECTION_ERROR,
    TLS_ERROR,
    INVALID_REQUEST,
    HTTP_ERROR,
    SCOPE_BLOCKED,
    BUDGET_EXCEEDED,
    RATE_LIMITED,
    CANCELLED,
    AUTH_CONTEXT_INVALID,
    TARGET_UNSTABLE,
    INTERNAL_ENGINE_ERROR
}
