package io.acra.core.active.safety;

import java.util.List;
import java.util.TreeMap;

public final class HierarchicalConcurrencyController {
    private static final class Counter {
        private int limit;
        private int running;

        Counter(int limit) { this.limit = limit; }
    }

    private final TreeMap<ConcurrencyKey, Counter> counters = new TreeMap<>();

    public synchronized void configure(ConcurrencyKey key, int limit) {
        if (key == null || limit < 1) throw new IllegalArgumentException("concurrency key/limit");
        Counter counter = counters.computeIfAbsent(key, ignored -> new Counter(limit));
        if (limit < counter.running) throw new IllegalArgumentException("limit below running count");
        counter.limit = limit;
    }

    public synchronized boolean canAcquire(List<ConcurrencyKey> keys) {
        if (keys == null || keys.isEmpty()) return false;
        return keys.stream().distinct().allMatch(key -> {
            Counter counter = counters.get(key);
            return counter != null && counter.running < counter.limit;
        });
    }

    public synchronized ConcurrencyLease acquire(List<ConcurrencyKey> keys) {
        List<ConcurrencyKey> distinct = keys == null ? List.of() : keys.stream().distinct().sorted().toList();
        if (!canAcquire(distinct)) throw new IllegalStateException("concurrency limit reached");
        distinct.forEach(key -> counters.get(key).running++);
        return new ConcurrencyLease(this, distinct);
    }

    synchronized void release(List<ConcurrencyKey> keys) {
        keys.forEach(key -> {
            Counter counter = counters.get(key);
            if (counter == null || counter.running < 1) throw new IllegalStateException("invalid concurrency release");
            counter.running--;
        });
    }

    public synchronized int running(ConcurrencyKey key) {
        Counter counter = counters.get(key);
        if (counter == null) throw new IllegalArgumentException("concurrency key not configured");
        return counter.running;
    }
}
