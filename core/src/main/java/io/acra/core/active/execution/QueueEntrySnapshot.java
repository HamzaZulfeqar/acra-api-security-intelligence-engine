package io.acra.core.active.execution;

import io.acra.core.domain.testing.TestState;

public record QueueEntrySnapshot(
        String testId,
        String signature,
        TestState state,
        TestPriority priority,
        long sequence,
        int attempts,
        String reason) {
    public QueueEntrySnapshot {
        if (testId == null || testId.isBlank() || signature == null || signature.isBlank()
                || state == null || priority == null || sequence < 0 || attempts < 0) {
            throw new IllegalArgumentException("invalid queue entry snapshot");
        }
        reason = reason == null ? "" : reason;
    }
}
