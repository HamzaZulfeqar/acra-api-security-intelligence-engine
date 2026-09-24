package io.acra.core.reporting.s12;

import java.util.List;

public record S12BurpIssueProjection(
        String projectionVersion,
        String candidateId,
        String title,
        String detail,
        String remediation,
        String url,
        String severity,
        String confidence,
        boolean publishable,
        List<String> evidenceIds) {

    public S12BurpIssueProjection {
        projectionVersion = required(projectionVersion, "projectionVersion");
        candidateId = required(candidateId, "candidateId");
        title = required(title, "title");
        detail = required(detail, "detail");
        remediation = required(remediation, "remediation");
        url = required(url, "url");
        severity = required(severity, "severity");
        confidence = required(confidence, "confidence");
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " required");
        return value.strip();
    }
}
