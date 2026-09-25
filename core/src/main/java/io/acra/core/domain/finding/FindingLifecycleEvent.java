package io.acra.core.domain.finding;

import io.acra.core.security.TokenFingerprint;
import io.acra.core.security.UniversalRedactor;
import java.util.List;

public record FindingLifecycleEvent(
        String eventId,
        int sequence,
        FindingLifecycleState fromState,
        FindingLifecycleState toState,
        FindingLifecycleAction action,
        String reviewerReference,
        String decisionReference,
        List<String> evidenceIds,
        String fingerprint) {

    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public FindingLifecycleEvent {
        if (sequence < 1) throw new IllegalArgumentException("event sequence must be positive");
        if (fromState == null || toState == null || action == null) {
            throw new IllegalArgumentException("event states/action required");
        }
        FindingLifecycleState expectedTarget = FindingLifecyclePolicy.target(fromState, action);
        if (expectedTarget != toState) {
            throw new IllegalArgumentException("event transition does not match lifecycle policy");
        }

        reviewerReference = safeRequired(reviewerReference, "reviewerReference");
        decisionReference = safeRequired(decisionReference, "decisionReference");
        evidenceIds = List.copyOf(evidenceIds == null ? List.<String>of() : evidenceIds).stream()
                .map(value -> safeRequired(value, "evidenceId"))
                .distinct()
                .sorted()
                .toList();
        if (evidenceIds.isEmpty()) throw new IllegalArgumentException("event evidence required");

        String material = canonical(
                sequence, fromState, toState, action,
                reviewerReference, decisionReference, evidenceIds);
        String expectedId = "finding-event-" + TokenFingerprint.sha256(material).substring(0, 24);
        eventId = eventId == null || eventId.isBlank() ? expectedId : eventId;
        if (!eventId.equals(expectedId)) {
            throw new IllegalArgumentException("finding lifecycle event identity mismatch");
        }

        String calculated = TokenFingerprint.sha256(eventId + "|" + material);
        fingerprint = fingerprint == null || fingerprint.isBlank() ? calculated : fingerprint;
        if (!fingerprint.equals(calculated)) {
            throw new IllegalArgumentException("finding lifecycle event fingerprint mismatch");
        }
    }

    private static String canonical(
            int sequence,
            FindingLifecycleState fromState,
            FindingLifecycleState toState,
            FindingLifecycleAction action,
            String reviewerReference,
            String decisionReference,
            List<String> evidenceIds) {
        return sequence + "|" + fromState + "|" + toState + "|" + action + "|"
                + reviewerReference + "|" + decisionReference + "|" + evidenceIds;
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
