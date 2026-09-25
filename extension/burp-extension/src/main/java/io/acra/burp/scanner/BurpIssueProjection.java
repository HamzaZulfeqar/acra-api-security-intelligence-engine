package io.acra.burp.scanner;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.scanner.audit.issues.AuditIssueConfidence;
import burp.api.montoya.scanner.audit.issues.AuditIssueSeverity;
import java.util.List;

public record BurpIssueProjection(
        String name,
        String detail,
        String remediation,
        String baseUrl,
        AuditIssueSeverity severity,
        AuditIssueConfidence confidence,
        String background,
        String remediationBackground,
        AuditIssueSeverity typicalSeverity,
        List<HttpRequestResponse> requestResponses) {

    public BurpIssueProjection {
        if (name == null || name.isBlank()
                || detail == null || detail.isBlank()
                || remediation == null || remediation.isBlank()
                || baseUrl == null || baseUrl.isBlank()
                || severity == null
                || confidence == null
                || background == null || background.isBlank()
                || typicalSeverity == null) {
            throw new IllegalArgumentException("complete Burp issue projection required");
        }
        remediationBackground = remediationBackground == null ? "" : remediationBackground;
        requestResponses = List.copyOf(requestResponses == null ? List.of() : requestResponses);
    }
}
