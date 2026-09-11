package io.acra.core.active.safety;

import io.acra.core.domain.common.Validation;

public record BudgetKey(BudgetScope scope, String id) implements Comparable<BudgetKey> {
    public BudgetKey {
        if (scope == null) throw new IllegalArgumentException("budget scope required");
        id = Validation.requireNonBlank(id, "budget id");
    }

    @Override
    public int compareTo(BudgetKey other) {
        int byScope = scope.compareTo(other.scope);
        return byScope == 0 ? id.compareTo(other.id) : byScope;
    }
}
