package io.acra.core.session;

import io.acra.core.domain.finding.FindingCandidate;
import java.util.List;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

public final class S10SessionCoverageTracker {
    private final NavigableMap<String, SessionCoverageEntry> entries = new TreeMap<>();

    public synchronized SessionCoverageEntry recordTarget(SessionCoverageTarget target) {
        if (target == null) throw new IllegalArgumentException("coverage target required");
        SessionCoverageEntry existing = entries.get(target.coverageId());
        if (existing != null && !existing.target().equals(target)) {
            throw new IllegalArgumentException("session coverage target drift for existing coverage identity");
        }
        SessionCoverageEntry value = existing == null ? SessionCoverageEntry.from(target) : existing;
        entries.put(target.coverageId(), value);
        return value;
    }

    public synchronized SessionCoverageEntry recordObservation(
            SessionCoverageTarget target,
            AuthenticationSessionObservation observation) {
        SessionCoverageEntry current = recordTarget(target);
        SessionCoverageEntry updated = current.withObservation(observation);
        entries.put(target.coverageId(), updated);
        return updated;
    }

    public synchronized SessionCoverageEntry recordCorrelation(
            SessionCoverageTarget target,
            AuthenticationSessionObservation observation,
            SessionCorrelationResult correlation) {
        SessionCoverageEntry current = recordObservation(target, observation);
        SessionCoverageEntry updated = current.withCorrelation(correlation);
        entries.put(target.coverageId(), updated);
        return updated;
    }

    public synchronized SessionCoverageEntry recordAssessment(
            SessionCoverageTarget target,
            SessionCorrelationResult correlation,
            SessionSecurityAssessment assessment,
            FindingCandidate finding) {
        SessionCoverageEntry current = Optional.ofNullable(entries.get(target.coverageId()))
                .orElseThrow(() -> new IllegalArgumentException("session coverage target was not registered"));
        SessionCoverageEntry updated = current.withAssessment(correlation, assessment, finding);
        entries.put(target.coverageId(), updated);
        return updated;
    }

    public synchronized List<SessionCoverageEntry> entries() {
        return List.copyOf(entries.values());
    }

    public synchronized SessionCoverageSummary summary() {
        return SessionCoverageSummary.fromEntries(entries());
    }

    public synchronized int size() {
        return entries.size();
    }

    public synchronized List<String> unobservedCoverageIds() {
        return ids(SessionCoverageDisposition.UNOBSERVED);
    }

    public synchronized List<String> observedUncorrelatedCoverageIds() {
        return ids(SessionCoverageDisposition.OBSERVED_UNCORRELATED);
    }

    public synchronized List<String> correlatedUnassessedCoverageIds() {
        return ids(SessionCoverageDisposition.CORRELATED_UNASSESSED);
    }

    public synchronized List<String> inconclusiveCoverageIds() {
        return ids(SessionCoverageDisposition.INCONCLUSIVE);
    }

    private List<String> ids(SessionCoverageDisposition disposition) {
        return entries.values().stream()
                .filter(entry -> entry.disposition() == disposition)
                .map(entry -> entry.target().coverageId())
                .toList();
    }
}
