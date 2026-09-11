package io.acra.core.active.safety;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ConcurrencyLease implements AutoCloseable {
    private final HierarchicalConcurrencyController controller;
    private final List<ConcurrencyKey> keys;
    private final AtomicBoolean closed = new AtomicBoolean();

    ConcurrencyLease(HierarchicalConcurrencyController controller, List<ConcurrencyKey> keys) {
        this.controller = controller;
        this.keys = keys;
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) controller.release(keys);
    }
}
