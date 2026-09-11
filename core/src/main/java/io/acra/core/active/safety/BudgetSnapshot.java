package io.acra.core.active.safety;

public record BudgetSnapshot(int allocated, int reserved, int executed, int remaining) {
    public BudgetSnapshot {
        if (allocated < 0 || reserved < 0 || executed < 0 || remaining < 0
                || remaining != allocated - reserved - executed) {
            throw new IllegalArgumentException("invalid budget snapshot");
        }
    }
}
