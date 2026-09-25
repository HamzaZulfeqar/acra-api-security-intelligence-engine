package io.acra.core.reporting.s13;

import io.acra.core.domain.finding.FindingConfidence;
import io.acra.core.domain.finding.FindingLifecycleState;
import io.acra.core.domain.finding.FindingSeverity;
import io.acra.core.security.UniversalRedactor;
import java.util.List;

public record S13GovernanceFindingProjection(
        String findingId,
        String sourceCandidateId,
        String sourceCandidateFingerprint,
        String riskAssessmentId,
        FindingSeverity severity,
        FindingConfidence confidence,
        FindingLifecycleState state,
        boolean confirmedHistory,
        List<String> evidenceIds,
        int eventCount,
        String findingFingerprint) {

    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public S13GovernanceFindingProjection {
        findingId = safe(findingId);
        sourceCandidateId = safe(sourceCandidateId);
        sourceCandidateFingerprint = safe(sourceCandidateFingerprint);
        riskAssessmentId = safe(riskAssessmentId);
        if (severity == null || confidence == null || state == null) {
            throw new IllegalArgumentException("severity/confidence/state required");
        }
        evidenceIds = List.copyOf(evidenceIds == null ? List.<String>of() : evidenceIds).stream()
                .map(S13GovernanceFindingProjection::safe)
                .filter(value -> !value.isBlank())
                .distinct()
                .sorted()
                .toList();
        if (eventCount < 0) throw new IllegalArgumentException("eventCount");
        findingFingerprint = safe(findingFingerprint);
    }

    private static String safe(String value) {
        return REDACTOR.redactText(value == null ? "" : value);
    }
}
