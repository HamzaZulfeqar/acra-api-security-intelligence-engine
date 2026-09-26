package io.acra.core.domain.finding;

import io.acra.core.security.TokenFingerprint;
import io.acra.core.security.UniversalRedactor;
import java.time.Instant;
import java.util.List;

public record FindingReviewTransition(
        String transitionId,
        FindingLifecycleState fromState,
        FindingLifecycleState toState,
        Instant occurredAt,
        String reviewerReference,
        String reason,
        List<String> evidenceIds) {

    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public FindingReviewTransition {
        if (fromState == null) throw new IllegalArgumentException("fromState required");
        if (toState == null) throw new IllegalArgumentException("toState required");
        if (fromState == toState) throw new IllegalArgumentException("state transition required");
        if (occurredAt == null) throw new IllegalArgumentException("occurredAt required");
        reviewerReference = requiredSafe(reviewerReference, "reviewerReference");
        reason = requiredSafe(reason, "reason");
        evidenceIds = safeEvidence(evidenceIds);
        if (evidenceIds.isEmpty()) throw new IllegalArgumentException("evidenceIds required");
        if (transitionId == null || transitionId.isBlank()) {
            String material = String.join("|",
                    fromState.name(),
                    toState.name(),
                    occurredAt.toString(),
                    reviewerReference,
                    reason,
                    String.join(",", evidenceIds));
            transitionId = "finding-transition-" + TokenFingerprint.sha256(material).substring(0, 24);
        } else {
            transitionId = requiredSafe(transitionId, "transitionId");
        }
    }

    private static String requiredSafe(String value, String field) {
        String safe = REDACTOR.redactText(value == null ? "" : value).trim();
        if (safe.isBlank()) throw new IllegalArgumentException(field + " required");
        return safe;
    }

    private static List<String> safeEvidence(List<String> values) {
        return List.copyOf(values == null ? List.<String>of() : values).stream()
                .map(value -> REDACTOR.redactText(value == null ? "" : value).trim())
                .filter(value -> !value.isBlank())
                .distinct()
                .sorted()
                .toList();
    }
}
