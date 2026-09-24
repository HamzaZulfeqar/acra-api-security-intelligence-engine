package io.acra.core.property;

import io.acra.core.domain.authorization.PropertyAuthorizationAssessment;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.engine.PolicyValidationEvaluator;
import java.util.List;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

public final class S9PropertyCoverageTracker {
    private final NavigableMap<String, PropertyAuthorizationCoverageEntry> entries = new TreeMap<>();

    public synchronized PropertyAuthorizationCoverageEntry recordPolicy(
            PolicyValidationEvaluator.PropertyPolicy policy) {
        PropertyAuthorizationCoverageEntry fresh = PropertyAuthorizationCoverageEntry.from(policy);
        PropertyAuthorizationCoverageEntry merged = Optional.ofNullable(entries.get(fresh.coverageId()))
                .map(fresh::carryLifecycleFrom)
                .orElse(fresh);
        entries.put(merged.coverageId(), merged);
        return merged;
    }

    public synchronized PropertyAuthorizationCoverageEntry recordObservation(
            PolicyValidationEvaluator.PropertyPolicy policy,
            PropertyAccessObservation observation) {
        PropertyAuthorizationCoverageEntry current = recordPolicy(policy);
        PropertyAuthorizationCoverageEntry updated = current.withObservation(observation);
        entries.put(updated.coverageId(), updated);
        return updated;
    }

    public synchronized PropertyAuthorizationCoverageEntry recordAssessment(
            PolicyValidationEvaluator.PropertyPolicy policy,
            PropertyAccessObservation observation,
            PropertyAuthorizationAssessment assessment,
            FindingCandidate finding) {
        PropertyAuthorizationCoverageEntry current = PropertyAuthorizationCoverageEntry.from(policy);
        current = Optional.ofNullable(entries.get(current.coverageId()))
                .orElseThrow(() -> new IllegalArgumentException("property policy coverage was not registered"));
        PropertyAuthorizationCoverageEntry updated = current.withAssessment(observation, assessment, finding);
        entries.put(updated.coverageId(), updated);
        return updated;
    }

    public synchronized List<PropertyAuthorizationCoverageEntry> entries() {
        return List.copyOf(entries.values());
    }

    public synchronized int size() {
        return entries.size();
    }

    public synchronized PropertyAuthorizationCoverageSummary summary() {
        return PropertyAuthorizationCoverageSummary.fromEntries(entries());
    }

    public synchronized List<String> unobservedCoverageIds() {
        return entries.values().stream()
                .filter(entry -> entry.disposition() == PropertyCoverageDisposition.UNOBSERVED)
                .map(PropertyAuthorizationCoverageEntry::coverageId)
                .toList();
    }

    public synchronized List<String> observedUnassessedCoverageIds() {
        return entries.values().stream()
                .filter(entry -> entry.disposition() == PropertyCoverageDisposition.OBSERVED_UNASSESSED)
                .map(PropertyAuthorizationCoverageEntry::coverageId)
                .toList();
    }

    public synchronized List<String> inconclusiveCoverageIds() {
        return entries.values().stream()
                .filter(entry -> entry.disposition() == PropertyCoverageDisposition.INCONCLUSIVE)
                .map(PropertyAuthorizationCoverageEntry::coverageId)
                .toList();
    }
}
