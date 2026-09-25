package burp.api.montoya.scanner.audit.issues;

import burp.api.montoya.http.message.HttpRequestResponse;
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
            List<HttpRequestResponse> requestResponses) {
        return new AuditIssue() { };
    }
}
