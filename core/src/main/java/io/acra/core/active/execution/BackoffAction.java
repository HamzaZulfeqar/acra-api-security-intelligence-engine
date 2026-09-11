package io.acra.core.active.execution;

public enum BackoffAction {
    NONE,
    PAUSE,
    DELAY,
    REDUCE_CONCURRENCY,
    RETRY
}
