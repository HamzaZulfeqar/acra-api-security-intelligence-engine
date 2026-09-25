package io.acra.core.domain.finding;

import io.acra.core.security.UniversalRedactor;
import java.util.List;

public record FindingLifecycleTransitionRequest(
        FindingLifecycleAction action,
        String reviewerReference,
        String decisionReference,
        List<String> evidenceIds) {

    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public FindingLifecycleTransitionRequest {
        if (action == null) throw new IllegalArgumentException("action required");
        reviewerReference = safeRequired(reviewerReference, "reviewerReference");
        decisionReference = safeRequired(decisionReference, "decisionReference");
        evidenceIds = List.copyOf(evidenceIds == null ? List.<String>of() : evidenceIds).stream()
                .map(value -> safeRequired(value, "evidenceId"))
                .distinct()
                .sorted()
                .toList();
        if (evidenceIds.isEmpty()) {
            throw new IllegalArgumentException("lifecycle transition requires evidence");
        }
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
