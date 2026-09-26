package io.acra.core.reporting.finding;

import io.acra.core.domain.finding.FindingLifecycleState;
import java.time.Instant;
import java.util.List;

public record FindingReviewTrailEntry(
        FindingLifecycleState fromState,
        FindingLifecycleState toState,
        Instant occurredAt,
        List<String> evidenceIds) {

    public FindingReviewTrailEntry {
        if (fromState == null) throw new IllegalArgumentException("fromState required");
        if (toState == null) throw new IllegalArgumentException("toState required");
        if (occurredAt == null) throw new IllegalArgumentException("occurredAt required");
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds).stream()
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .sorted()
                .toList();
        if (evidenceIds.isEmpty()) throw new IllegalArgumentException("evidenceIds required");
    }
}
