package io.acra.core.coverage;

import io.acra.core.batch.BatchItemAuthorizationAssessment;
import io.acra.core.batch.BatchItemObservation;
import io.acra.core.batch.BatchItemPolicy;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.reference.IndirectReferenceAuthorizationAssessment;
import io.acra.core.reference.IndirectReferencePolicy;
import io.acra.core.reference.IndirectReferenceResolution;
import java.util.List;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

public final class S10AuthorizationCoverageTracker {
    private final NavigableMap<String, S10AuthorizationCoverageEntry> entries = new TreeMap<>();

    public synchronized S10AuthorizationCoverageEntry recordPolicy(BatchItemPolicy policy) {
        return merge(S10AuthorizationCoverageEntry.from(policy));
    }

    public synchronized S10AuthorizationCoverageEntry recordPolicy(IndirectReferencePolicy policy) {
        return merge(S10AuthorizationCoverageEntry.from(policy));
    }

    public synchronized S10AuthorizationCoverageEntry recordObservation(
            BatchItemPolicy policy, BatchItemObservation observation) {
        S10AuthorizationCoverageEntry current = recordPolicy(policy);
        S10AuthorizationCoverageEntry updated = current.withObservation(observation);
        entries.put(updated.coverageId(), updated);
        return updated;
    }

    public synchronized S10AuthorizationCoverageEntry recordObservation(
            IndirectReferencePolicy policy, IndirectReferenceResolution resolution) {
        S10AuthorizationCoverageEntry current = recordPolicy(policy);
        S10AuthorizationCoverageEntry updated = current.withObservation(resolution);
        entries.put(updated.coverageId(), updated);
        return updated;
    }

    public synchronized S10AuthorizationCoverageEntry recordAssessment(
            BatchItemPolicy policy,
            BatchItemObservation observation,
            BatchItemAuthorizationAssessment assessment,
            FindingCandidate finding) {
        S10AuthorizationCoverageEntry current = existing(S10AuthorizationCoverageEntry.from(policy).coverageId());
        S10AuthorizationCoverageEntry updated = current.withAssessment(observation, assessment, finding);
        entries.put(updated.coverageId(), updated);
        return updated;
    }

    public synchronized S10AuthorizationCoverageEntry recordAssessment(
            IndirectReferencePolicy policy,
            IndirectReferenceResolution resolution,
            IndirectReferenceAuthorizationAssessment assessment,
            FindingCandidate finding) {
        S10AuthorizationCoverageEntry current = existing(S10AuthorizationCoverageEntry.from(policy).coverageId());
        S10AuthorizationCoverageEntry updated = current.withAssessment(resolution, assessment, finding);
        entries.put(updated.coverageId(), updated);
        return updated;
    }

    public synchronized List<S10AuthorizationCoverageEntry> entries() {
        return List.copyOf(entries.values());
    }

    public synchronized int size() {
        return entries.size();
    }

    public synchronized S10AuthorizationCoverageSummary summary() {
        return S10AuthorizationCoverageSummary.fromEntries(entries());
    }

    public synchronized List<String> unobservedCoverageIds() {
        return ids(S10CoverageDisposition.UNOBSERVED);
    }

    public synchronized List<String> observedUnassessedCoverageIds() {
        return ids(S10CoverageDisposition.OBSERVED_UNASSESSED);
    }

    public synchronized List<String> inconclusiveCoverageIds() {
        return ids(S10CoverageDisposition.INCONCLUSIVE);
    }

    private S10AuthorizationCoverageEntry merge(S10AuthorizationCoverageEntry fresh) {
        S10AuthorizationCoverageEntry merged = Optional.ofNullable(entries.get(fresh.coverageId()))
                .map(fresh::carryLifecycleFrom)
                .orElse(fresh);
        entries.put(merged.coverageId(), merged);
        return merged;
    }

    private S10AuthorizationCoverageEntry existing(String coverageId) {
        return Optional.ofNullable(entries.get(coverageId))
                .orElseThrow(() -> new IllegalArgumentException("S10 policy coverage was not registered"));
    }

    private List<String> ids(S10CoverageDisposition disposition) {
        return entries.values().stream()
                .filter(entry -> entry.disposition() == disposition)
                .map(S10AuthorizationCoverageEntry::coverageId)
                .toList();
    }
}
