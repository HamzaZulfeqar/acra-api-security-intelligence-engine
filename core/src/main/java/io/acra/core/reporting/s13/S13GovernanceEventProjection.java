package io.acra.core.reporting.s13;

import io.acra.core.domain.finding.FindingLifecycleAction;
import io.acra.core.domain.finding.FindingLifecycleState;
import io.acra.core.security.UniversalRedactor;
import java.util.List;

public record S13GovernanceEventProjection(
        String findingId,
        String eventId,
        int sequence,
        FindingLifecycleState fromState,
        FindingLifecycleState toState,
        FindingLifecycleAction action,
        String reviewerReference,
        String decisionReference,
        List<String> evidenceIds,
        String eventFingerprint) {

    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public S13GovernanceEventProjection {
        findingId = safe(findingId);
        eventId = safe(eventId);
        if (sequence < 1) throw new IllegalArgumentException("sequence");
        if (fromState == null || toState == null || action == null) {
            throw new IllegalArgumentException("event state/action required");
        }
        reviewerReference = safe(reviewerReference);
        decisionReference = safe(decisionReference);
        evidenceIds = List.copyOf(evidenceIds == null ? List.<String>of() : evidenceIds).stream()
                .map(S13GovernanceEventProjection::safe)
                .filter(value -> !value.isBlank())
                .distinct()
                .sorted()
                .toList();
        eventFingerprint = safe(eventFingerprint);
    }

    private static String safe(String value) {
        return REDACTOR.redactText(value == null ? "" : value);
    }
}
