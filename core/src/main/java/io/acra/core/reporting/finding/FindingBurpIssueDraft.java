package io.acra.core.reporting.finding;

import io.acra.core.domain.finding.FindingLifecycleState;
import java.util.List;

public record FindingBurpIssueDraft(
        String draftId,
        String reproductionId,
        String findingId,
        FindingLifecycleState lifecycleState,
        String name,
        String detail,
        String remediation,
        String targetEndpoint,
        FindingBurpIssueSeverity severity,
        FindingBurpIssueConfidence confidence,
        String background,
        String remediationBackground,
        FindingBurpIssueSeverity typicalSeverity,
        boolean publicationEligible,
        List<String> evidenceIds,
        List<String> limitations) {

    public FindingBurpIssueDraft {
        draftId = required(draftId, "draftId");
        reproductionId = required(reproductionId, "reproductionId");
        findingId = required(findingId, "findingId");
        if (lifecycleState == null) throw new IllegalArgumentException("lifecycleState required");
        name = required(name, "name");
        detail = required(detail, "detail");
        remediation = required(remediation, "remediation");
        targetEndpoint = required(targetEndpoint, "targetEndpoint");
        if (severity == null) throw new IllegalArgumentException("severity required");
        if (confidence == null) throw new IllegalArgumentException("confidence required");
        background = required(background, "background");
        remediationBackground = required(remediationBackground, "remediationBackground");
        if (typicalSeverity == null) throw new IllegalArgumentException("typicalSeverity required");
        evidenceIds = sorted(evidenceIds);
        limitations = sorted(limitations);
        if (evidenceIds.isEmpty()) throw new IllegalArgumentException("evidenceIds required");
        if (limitations.isEmpty()) throw new IllegalArgumentException("limitations required");
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " required");
        return value;
    }

    private static List<String> sorted(List<String> values) {
        return List.copyOf(values == null ? List.<String>of() : values).stream()
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .sorted()
                .toList();
    }
}
