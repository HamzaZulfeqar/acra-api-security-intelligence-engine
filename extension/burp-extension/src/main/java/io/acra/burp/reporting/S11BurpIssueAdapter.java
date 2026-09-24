package io.acra.burp.reporting;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.scanner.audit.issues.AuditIssue;
import burp.api.montoya.scanner.audit.issues.AuditIssueConfidence;
import burp.api.montoya.scanner.audit.issues.AuditIssueSeverity;
import io.acra.core.reporting.finding.FindingBurpIssueConfidence;
import io.acra.core.reporting.finding.FindingBurpIssueDraft;
import io.acra.core.reporting.finding.FindingBurpIssueSeverity;
import java.net.URI;
import java.util.List;

public final class S11BurpIssueAdapter {

    public AuditIssue materialize(
            FindingBurpIssueDraft draft,
            String baseUrl,
            List<HttpRequestResponse> requestResponses) {
        if (draft == null) throw new IllegalArgumentException("draft required");
        if (!draft.publicationEligible()) {
            throw new IllegalArgumentException("draft is not publication eligible");
        }
        requireHttpBaseUrl(baseUrl);
        List<HttpRequestResponse> evidence = List.copyOf(
                requestResponses == null ? List.of() : requestResponses);
        if (evidence.stream().anyMatch(value -> value == null)) {
            throw new IllegalArgumentException("requestResponses contains null");
        }

        return AuditIssue.auditIssue(
                draft.name(),
                draft.detail(),
                draft.remediation(),
                baseUrl,
                severity(draft.severity()),
                confidence(draft.confidence()),
                draft.background(),
                draft.remediationBackground(),
                severity(draft.typicalSeverity()),
                evidence);
    }

    public AuditIssueSeverity severity(FindingBurpIssueSeverity value) {
        if (value == null) throw new IllegalArgumentException("severity required");
        return switch (value) {
            case HIGH -> AuditIssueSeverity.HIGH;
            case MEDIUM -> AuditIssueSeverity.MEDIUM;
            case LOW -> AuditIssueSeverity.LOW;
            case INFORMATION -> AuditIssueSeverity.INFORMATION;
            case FALSE_POSITIVE -> AuditIssueSeverity.FALSE_POSITIVE;
        };
    }

    public AuditIssueConfidence confidence(FindingBurpIssueConfidence value) {
        if (value == null) throw new IllegalArgumentException("confidence required");
        return switch (value) {
            case CERTAIN -> AuditIssueConfidence.CERTAIN;
            case FIRM -> AuditIssueConfidence.FIRM;
            case TENTATIVE -> AuditIssueConfidence.TENTATIVE;
        };
    }

    private static void requireHttpBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("baseUrl required");
        }
        URI uri;
        try {
            uri = URI.create(baseUrl);
        } catch (IllegalArgumentException invalid) {
            throw new IllegalArgumentException("baseUrl must be a valid absolute HTTP(S) URL", invalid);
        }
        String scheme = uri.getScheme();
        if (!uri.isAbsolute()
                || scheme == null
                || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))
                || uri.getHost() == null) {
            throw new IllegalArgumentException("baseUrl must be a valid absolute HTTP(S) URL");
        }
    }
}
