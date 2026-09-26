package io.acra.core.domain.finding;

import io.acra.core.security.UniversalRedactor;
import java.time.Instant;
import java.util.List;

public record ReviewedFinding(
        String findingId,
        String candidateId,
        String projectId,
        FindingFingerprint fingerprint,
        FindingSeverity severity,
        FindingConfidence confidence,
        FindingLifecycleState state,
        List<String> supportingEvidenceIds,
        List<FindingReviewTransition> history,
        Instant openedAt,
        Instant updatedAt) {

    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public ReviewedFinding {
        findingId = requiredSafe(findingId, "findingId");
        candidateId = requiredSafe(candidateId, "candidateId");
        projectId = requiredSafe(projectId, "projectId");
        if (fingerprint == null) throw new IllegalArgumentException("fingerprint required");
        if (severity == null) throw new IllegalArgumentException("severity required");
        if (confidence == null) throw new IllegalArgumentException("confidence required");
        if (state == null) throw new IllegalArgumentException("state required");
        supportingEvidenceIds = List.copyOf(
                supportingEvidenceIds == null ? List.<String>of() : supportingEvidenceIds).stream()
                .map(value -> REDACTOR.redactText(value == null ? "" : value).trim())
                .filter(value -> !value.isBlank())
                .distinct()
                .sorted()
                .toList();
        if (supportingEvidenceIds.isEmpty()) {
            throw new IllegalArgumentException("supportingEvidenceIds required");
        }
        history = List.copyOf(history == null ? List.of() : history);
        if (history.stream().anyMatch(value -> value == null)) {
            throw new IllegalArgumentException("history contains null transition");
        }
        if (openedAt == null) throw new IllegalArgumentException("openedAt required");
        if (updatedAt == null) throw new IllegalArgumentException("updatedAt required");
        if (updatedAt.isBefore(openedAt)) throw new IllegalArgumentException("updatedAt before openedAt");
    }

    public boolean terminal() {
        return state == FindingLifecycleState.FALSE_POSITIVE
                || state == FindingLifecycleState.ACCEPTED_RISK;
    }

    private static String requiredSafe(String value, String field) {
        String safe = REDACTOR.redactText(value == null ? "" : value).trim();
        if (safe.isBlank()) throw new IllegalArgumentException(field + " required");
        return safe;
    }
}
