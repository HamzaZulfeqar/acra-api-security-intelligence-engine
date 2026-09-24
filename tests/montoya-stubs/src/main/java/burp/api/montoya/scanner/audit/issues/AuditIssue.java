package burp.api.montoya.scanner.audit.issues;

import java.util.List;

public interface AuditIssue {

    static AuditIssue auditIssue(
            String name,
            String detail,
            String remediation,
            String baseUrl,
            AuditIssueSeverity severity,
            AuditIssueConfidence confidence,
            String background,
            String remediationBackground,
            AuditIssueSeverity typicalSeverity,
            List<?> requestResponses) {
        return new StubAuditIssue(
                name, detail, remediation, baseUrl, severity, confidence,
                background, remediationBackground, typicalSeverity);
    }

    record StubAuditIssue(
            String name,
            String detail,
            String remediation,
            String baseUrl,
            AuditIssueSeverity severity,
            AuditIssueConfidence confidence,
            String background,
            String remediationBackground,
            AuditIssueSeverity typicalSeverity) implements AuditIssue { }
}
