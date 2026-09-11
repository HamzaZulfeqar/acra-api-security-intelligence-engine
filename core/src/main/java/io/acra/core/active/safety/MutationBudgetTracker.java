package io.acra.core.active.safety;

public final class MutationBudgetTracker {
    private final int limit;
    private int generated;
    private int deduplicated;
    private int skipped;
    private int executed;
    private int failed;

    public MutationBudgetTracker(int limit) {
        if (limit < 0) throw new IllegalArgumentException("mutation limit");
        this.limit = limit;
    }

    public synchronized boolean recordGenerated() {
        if (generated >= limit) return false;
        generated++;
        return true;
    }

    public synchronized void recordDeduplicated() { deduplicated++; }
    public synchronized void recordSkipped() { skipped++; }
    public synchronized void recordExecuted() { executed++; }
    public synchronized void recordFailed() { failed++; }

    public synchronized MutationBudgetSnapshot snapshot() {
        return new MutationBudgetSnapshot(generated, deduplicated, skipped, executed, failed);
    }
}
