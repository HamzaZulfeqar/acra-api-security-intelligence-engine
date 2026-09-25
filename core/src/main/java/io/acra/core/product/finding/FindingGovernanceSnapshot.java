package io.acra.core.product.finding;

import io.acra.core.domain.finding.FindingLifecycleState;
import io.acra.core.domain.finding.GovernedFinding;
import java.util.Comparator;
import java.util.List;

public record FindingGovernanceSnapshot(
        List<GovernedFinding> findings) {

    public FindingGovernanceSnapshot {
        findings = List.copyOf(findings == null ? List.of() : findings).stream()
                .sorted(Comparator.comparing(GovernedFinding::findingId))
                .toList();
    }

    public int findingCount() {
        return findings.size();
    }

    public long stateCount(FindingLifecycleState state) {
        if (state == null) throw new IllegalArgumentException("state required");
        return findings.stream().filter(value -> value.state() == state).count();
    }

    public long queueCount(FindingGovernanceQueue queue) {
        if (queue == null) throw new IllegalArgumentException("queue required");
        return findings.stream()
                .filter(value -> FindingGovernanceQueue.from(value.state()) == queue)
                .count();
    }

    public List<GovernedFinding> queue(FindingGovernanceQueue queue) {
        if (queue == null) throw new IllegalArgumentException("queue required");
        return findings.stream()
                .filter(value -> FindingGovernanceQueue.from(value.state()) == queue)
                .toList();
    }

    public long confirmedHistoryCount() {
        return findings.stream().filter(GovernedFinding::confirmed).count();
    }
}
