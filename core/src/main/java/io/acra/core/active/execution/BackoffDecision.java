package io.acra.core.active.execution;

import java.time.Duration;
import java.util.Set;

public record BackoffDecision(Set<BackoffAction> actions, Duration delay, boolean retryAllowed, String reason) {
    public BackoffDecision {
        actions = Set.copyOf(actions == null ? Set.of() : actions);
        if (delay == null || delay.isNegative()) throw new IllegalArgumentException("backoff delay");
        reason = reason == null ? "" : reason;
    }

    public static BackoffDecision none() {
        return new BackoffDecision(Set.of(BackoffAction.NONE), Duration.ZERO, false, "");
    }
}
