package io.acra.core.active.execution;

import io.acra.core.domain.common.Validation;

public record ExecutionFailure(ExecutionErrorType type, String message, boolean retryable, String causeClass) {
    public ExecutionFailure {
        if (type == null) throw new IllegalArgumentException("execution error type required");
        message = Validation.requireNonBlank(message, "execution error message");
        causeClass = causeClass == null ? "" : causeClass;
    }
}
