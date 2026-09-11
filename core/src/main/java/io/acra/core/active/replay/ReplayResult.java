package io.acra.core.active.replay;

import io.acra.core.active.execution.TestExecutionResult;

public record ReplayResult(boolean verified, String reason, TestExecutionResult freshExecution) {
    public ReplayResult {
        reason = reason == null ? "" : reason;
        if (verified && freshExecution == null) throw new IllegalArgumentException("verified replay requires fresh execution");
        if (!verified && reason.isBlank()) throw new IllegalArgumentException("failed replay requires reason");
    }
}
