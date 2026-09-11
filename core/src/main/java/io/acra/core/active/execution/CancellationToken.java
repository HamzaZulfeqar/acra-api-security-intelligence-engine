package io.acra.core.active.execution;

import java.util.concurrent.atomic.AtomicBoolean;

public final class CancellationToken {
    private final AtomicBoolean cancelled = new AtomicBoolean();

    public void cancel() { cancelled.set(true); }
    public boolean cancelled() { return cancelled.get(); }
    public void throwIfCancelled() {
        if (cancelled()) throw new java.util.concurrent.CancellationException("execution cancelled");
    }
}
