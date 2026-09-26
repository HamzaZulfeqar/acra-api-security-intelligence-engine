package io.acra.core.product.finding;

import io.acra.core.domain.finding.FindingLifecycleState;
import java.util.List;

public record FindingReviewSnapshot(
        String projectId,
        List<FindingReviewCase> cases) {

    public FindingReviewSnapshot {
        if (projectId == null || projectId.isBlank()) throw new IllegalArgumentException("projectId required");
        cases = List.copyOf(cases == null ? List.of() : cases);
        if (cases.stream().anyMatch(value -> value == null)) {
            throw new IllegalArgumentException("cases contains null");
        }
        if (cases.stream().anyMatch(value -> !projectId.equals(value.finding().projectId()))) {
            throw new IllegalArgumentException("snapshot project mismatch");
        }
    }

    public int totalCount() {
        return cases.size();
    }

    public long count(FindingLifecycleState state) {
        if (state == null) throw new IllegalArgumentException("state required");
        return cases.stream().filter(value -> value.finding().state() == state).count();
    }

    public long openReviewCount() {
        return cases.stream()
                .filter(value -> !value.finding().terminal())
                .count();
    }

    public long terminalCount() {
        return cases.stream()
                .filter(value -> value.finding().terminal())
                .count();
    }
}
