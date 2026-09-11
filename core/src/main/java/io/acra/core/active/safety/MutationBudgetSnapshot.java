package io.acra.core.active.safety;

public record MutationBudgetSnapshot(int generated, int deduplicated, int skipped, int executed, int failed) {}
