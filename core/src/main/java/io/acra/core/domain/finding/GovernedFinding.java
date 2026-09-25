package io.acra.core.domain.finding;

import io.acra.core.security.TokenFingerprint;
import io.acra.core.security.UniversalRedactor;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public record GovernedFinding(
        String findingId,
        String sourceCandidateId,
        String sourceCandidateFingerprint,
        String riskAssessmentId,
        FindingSeverity severity,
        FindingConfidence confidence,
        FindingLifecycleState state,
        List<String> evidenceIds,
        List<FindingLifecycleEvent> events,
        String fingerprint) {

    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public GovernedFinding {
        sourceCandidateId = safeRequired(sourceCandidateId, "sourceCandidateId");
        sourceCandidateFingerprint = safeRequired(
                sourceCandidateFingerprint, "sourceCandidateFingerprint");
        riskAssessmentId = safeRequired(riskAssessmentId, "riskAssessmentId");
        if (severity == null || confidence == null || state == null) {
            throw new IllegalArgumentException("severity/confidence/state required");
        }

        evidenceIds = List.copyOf(evidenceIds == null ? List.<String>of() : evidenceIds).stream()
                .map(value -> safeRequired(value, "evidenceId"))
                .distinct()
                .sorted()
                .toList();
        if (evidenceIds.isEmpty()) throw new IllegalArgumentException("finding evidence required");

        events = List.copyOf(events == null ? List.<FindingLifecycleEvent>of() : events);
        validateHistory(state, events);

        TreeSet<String> eventEvidence = new TreeSet<>();
        events.forEach(event -> eventEvidence.addAll(event.evidenceIds()));
        if (!evidenceIds.containsAll(eventEvidence)) {
            throw new IllegalArgumentException("finding evidence must include lifecycle event evidence");
        }

        String identityMaterial = sourceCandidateId + "|" + sourceCandidateFingerprint + "|" + riskAssessmentId;
        String expectedFindingId = "finding-" + TokenFingerprint.sha256(identityMaterial).substring(0, 24);
        findingId = findingId == null || findingId.isBlank() ? expectedFindingId : findingId;
        if (!findingId.equals(expectedFindingId)) {
            throw new IllegalArgumentException("governed finding identity mismatch");
        }

        String calculated = TokenFingerprint.sha256(canonical(
                findingId, sourceCandidateId, sourceCandidateFingerprint, riskAssessmentId,
                severity, confidence, state, evidenceIds, events));
        fingerprint = fingerprint == null || fingerprint.isBlank() ? calculated : fingerprint;
        if (!fingerprint.equals(calculated)) {
            throw new IllegalArgumentException("governed finding fingerprint mismatch");
        }
    }

    public boolean confirmed() {
        return state == FindingLifecycleState.CONFIRMED
                || state == FindingLifecycleState.ACCEPTED_RISK
                || state == FindingLifecycleState.REMEDIATION_IN_PROGRESS
                || state == FindingLifecycleState.RETEST_REQUIRED
                || state == FindingLifecycleState.RESOLVED
                || (state == FindingLifecycleState.CLOSED && events.stream()
                        .anyMatch(event -> event.action() == FindingLifecycleAction.CONFIRM));
    }

    private static void validateHistory(
            FindingLifecycleState state,
            List<FindingLifecycleEvent> events) {
        if (events.isEmpty()) {
            if (state != FindingLifecycleState.REVIEW_REQUIRED) {
                throw new IllegalArgumentException(
                        "finding without lifecycle events must be REVIEW_REQUIRED");
            }
            return;
        }

        FindingLifecycleState expectedFrom = FindingLifecycleState.REVIEW_REQUIRED;
        for (int index = 0; index < events.size(); index++) {
            FindingLifecycleEvent event = events.get(index);
            if (event.sequence() != index + 1) {
                throw new IllegalArgumentException("finding lifecycle event sequence gap");
            }
            if (event.fromState() != expectedFrom) {
                throw new IllegalArgumentException("finding lifecycle event chain mismatch");
            }
            expectedFrom = event.toState();
        }
        if (state != expectedFrom) {
            throw new IllegalArgumentException("finding state does not match lifecycle history");
        }
    }

    private static String canonical(
            String findingId,
            String sourceCandidateId,
            String sourceCandidateFingerprint,
            String riskAssessmentId,
            FindingSeverity severity,
            FindingConfidence confidence,
            FindingLifecycleState state,
            List<String> evidenceIds,
            List<FindingLifecycleEvent> events) {
        StringBuilder out = new StringBuilder()
                .append(findingId).append('|')
                .append(sourceCandidateId).append('|')
                .append(sourceCandidateFingerprint).append('|')
                .append(riskAssessmentId).append('|')
                .append(severity).append('|')
                .append(confidence).append('|')
                .append(state).append('|')
                .append(evidenceIds).append('\n');
        for (FindingLifecycleEvent event : events) {
            out.append(event.eventId()).append('|').append(event.fingerprint()).append('\n');
        }
        return out.toString();
    }

    private static String safeRequired(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " required");
        String stripped = value.strip();
        if (!stripped.equals(REDACTOR.redactText(stripped))) {
            throw new IllegalArgumentException(name + " contains secret-bearing material");
        }
        return stripped;
    }
}
