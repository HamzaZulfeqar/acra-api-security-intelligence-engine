package io.acra.core.active.safety;

import io.acra.core.domain.common.Validation;

public record ConcurrencyKey(ConcurrencyScope scope, String id) implements Comparable<ConcurrencyKey> {
    public ConcurrencyKey {
        if (scope == null) throw new IllegalArgumentException("concurrency scope required");
        id = Validation.requireNonBlank(id, "concurrency id");
    }

    @Override
    public int compareTo(ConcurrencyKey other) {
        int byScope = scope.compareTo(other.scope);
        return byScope == 0 ? id.compareTo(other.id) : byScope;
    }
}
