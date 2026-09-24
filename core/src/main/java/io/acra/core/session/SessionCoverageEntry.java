package io.acra.core.session;

import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

public record SessionCoverageEntry(
        SessionCoverageTarget target,
        List<String> observationIds,
        List<String> correlationIds,
        List<String> assessmentIds,
        List<String> findingCandidateIds,
        List<FindingCandidateState> findingStates) {

    public SessionCoverageEntry {
        if (target == null) throw new IllegalArgumentException("target required");
        observationIds = sorted(observationIds);
        correlationIds = sorted(correlationIds);
        assessmentIds = sorted(assessmentIds);
        findingCandidateIds = sorted(findingCandidateIds);
        findingStates = List.copyOf(findingStates == null ? List.of() : findingStates).stream()
                .distinct()
                .sorted(Comparator.comparing(Enum::name))
                .toList();
        if (assessmentIds.size() != findingCandidateIds.size() && !assessmentIds.isEmpty()) {
            throw new IllegalArgumentException("every assessed coverage context requires a finding projection");
        }
    }

    public static SessionCoverageEntry from(SessionCoverageTarget target) {
        return new SessionCoverageEntry(target, List.of(), List.of(), List.of(), List.of(), List.of());
    }

    public SessionCoverageDisposition disposition() {
        if (observationIds.isEmpty()) return SessionCoverageDisposition.UNOBSERVED;
        if (correlationIds.isEmpty()) return SessionCoverageDisposition.OBSERVED_UNCORRELATED;
        if (assessmentIds.isEmpty()) return SessionCoverageDisposition.CORRELATED_UNASSESSED;
        if (findingStates.contains(FindingCandidateState.CANDIDATE)) return SessionCoverageDisposition.CANDIDATE;
        if (findingStates.contains(FindingCandidateState.INCONCLUSIVE)) return SessionCoverageDisposition.INCONCLUSIVE;
        return SessionCoverageDisposition.REJECTED;
    }

    public SessionCoverageEntry withObservation(AuthenticationSessionObservation observation) {
        requireSession(observation == null ? null : observation.sessionId());
        if (observation == null) throw new IllegalArgumentException("observation required");
        return new SessionCoverageEntry(
                target,
                union(observationIds, List.of(observation.observationId())),
                correlationIds,
                assessmentIds,
                findingCandidateIds,
                findingStates);
    }

    public SessionCoverageEntry withCorrelation(SessionCorrelationResult correlation) {
        if (correlation == null) throw new IllegalArgumentException("correlation required");
        requireSession(correlation.sessionId());
        if (!observationIds.contains(correlation.currentObservationId())) {
            throw new IllegalArgumentException("current correlation observation must be recorded before correlation");
        }
        if (target.objective() == SessionCoverageObjective.ROTATION_CONTEXT_STABILITY
                && !correlation.tokenRotated()) {
            throw new IllegalArgumentException("rotation coverage requires a token-rotation correlation");
        }
        return new SessionCoverageEntry(
                target,
                observationIds,
                union(correlationIds, List.of(correlation.correlationId())),
                assessmentIds,
                findingCandidateIds,
                findingStates);
    }

    public SessionCoverageEntry withAssessment(
            SessionCorrelationResult correlation,
            SessionSecurityAssessment assessment,
            FindingCandidate finding) {

        if (correlation == null || assessment == null || finding == null) {
            throw new IllegalArgumentException("correlation, assessment and finding required");
        }
        requireSession(correlation.sessionId());
        if (!correlationIds.contains(correlation.correlationId())) {
            throw new IllegalArgumentException("correlation must be recorded before assessment");
        }
        if (!assessment.sessionId().equals(target.sessionId())
                || !assessment.currentObservationId().equals(correlation.currentObservationId())
                || !assessment.driftDimensions().equals(correlation.driftDimensions())) {
            throw new IllegalArgumentException("session assessment does not match coverage target");
        }
        if (!finding.assessmentIds().contains(assessment.assessmentId())
                || !finding.dimensions().contains("AUTHENTICATION_SESSION")
                || !finding.resourceId().equals("session:" + target.sessionId())
                || !finding.policyReferences().contains(target.ruleReference())) {
            throw new IllegalArgumentException("session finding does not match coverage assessment");
        }
        return new SessionCoverageEntry(
                target,
                observationIds,
                correlationIds,
                union(assessmentIds, List.of(assessment.assessmentId())),
                union(findingCandidateIds, List.of(finding.candidateId())),
                unionStates(findingStates, finding.state()));
    }

    private void requireSession(String sessionId) {
        if (sessionId == null || !target.sessionId().equals(sessionId)) {
            throw new IllegalArgumentException("session context does not match coverage target");
        }
    }

    private static List<String> union(List<String> left, List<String> right) {
        TreeSet<String> values = new TreeSet<>();
        if (left != null) values.addAll(left);
        if (right != null) values.addAll(right);
        values.removeIf(value -> value == null || value.isBlank());
        return List.copyOf(values);
    }

    private static List<String> sorted(List<String> values) {
        return union(List.of(), values);
    }

    private static List<FindingCandidateState> unionStates(
            List<FindingCandidateState> current,
            FindingCandidateState state) {
        List<FindingCandidateState> values = new ArrayList<>(current == null ? List.of() : current);
        if (state != null) values.add(state);
        return values.stream().distinct().sorted(Comparator.comparing(Enum::name)).toList();
    }
}
