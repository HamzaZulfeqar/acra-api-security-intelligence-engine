package io.acra.core.product.reproduction;

import io.acra.core.domain.finding.FindingCandidateState;
import java.util.List;

public record S12ReproductionProductSnapshot(
        List<S12ReproductionProductEntry> entries) {

    public S12ReproductionProductSnapshot {
        entries = List.copyOf(entries == null ? List.of() : entries);
    }

    public int packageCount() {
        return entries.size();
    }

    public long candidateCount() {
        return entries.stream()
                .filter(value -> value.reproductionPackage().candidateState() == FindingCandidateState.CANDIDATE)
                .count();
    }

    public long rejectedCount() {
        return entries.stream()
                .filter(value -> value.reproductionPackage().candidateState() == FindingCandidateState.REJECTED)
                .count();
    }

    public long inconclusiveCount() {
        return entries.stream()
                .filter(value -> value.reproductionPackage().candidateState() == FindingCandidateState.INCONCLUSIVE)
                .count();
    }

    public long publishableProjectionCount() {
        return entries.stream().filter(value -> value.burpProjection().publishable()).count();
    }
}
