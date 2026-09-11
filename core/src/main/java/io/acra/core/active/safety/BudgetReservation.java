package io.acra.core.active.safety;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BudgetReservation implements AutoCloseable {
    private final HierarchicalBudgetManager manager;
    private final List<BudgetKey> keys;
    private final int count;
    private final AtomicBoolean finished = new AtomicBoolean();

    BudgetReservation(HierarchicalBudgetManager manager, List<BudgetKey> keys, int count) {
        this.manager = manager;
        this.keys = keys;
        this.count = count;
    }

    public void commit() {
        if (!finished.compareAndSet(false, true)) throw new IllegalStateException("reservation already completed");
        manager.commit(keys, count);
    }

    @Override
    public void close() {
        if (finished.compareAndSet(false, true)) manager.release(keys, count);
    }
}
