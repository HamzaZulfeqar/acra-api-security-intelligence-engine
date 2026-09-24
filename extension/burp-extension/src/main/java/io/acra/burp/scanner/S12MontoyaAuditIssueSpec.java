package io.acra.burp.scanner;

import burp.api.montoya.scanner.audit.issues.AuditIssueConfidence;
import burp.api.montoya.scanner.audit.issues.AuditIssueSeverity;

public record S12MontoyaAuditIssueSpec(
        String name,
        String detail,
        String remediation,
        String baseUrl,
        AuditIssueSeverity severity,
        AuditIssueConfidence confidence,
        String background,
        String remediationBackground,
        AuditIssueSeverity typicalSeverity) {

    public S12MontoyaAuditIssueSpec {
        name = required(name, "name");
        detail = required(detail, "detail");
        remediation = required(remediation, "remediation");
        baseUrl = required(baseUrl, "baseUrl");
        if (severity == null || confidence == null || typicalSeverity == null) {
            throw new IllegalArgumentException("Montoya issue severity/confidence required");
        }
        background = required(background, "background");
        remediationBackground = required(remediationBackground, "remediationBackground");
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " required");
        return value.strip();
    }
}
