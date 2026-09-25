package burp.api.montoya.scanner.audit.issues;

import burp.api.montoya.http.message.HttpRequestResponse;
import java.util.List;

public interface AuditIssue {
    String name();
    String detail();
    String remediation();
    String baseUrl();
    AuditIssueSeverity severity();
    AuditIssueConfidence confidence();
    List<HttpRequestResponse> requestResponses();

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
            HttpRequestResponse... requestResponses) {
        List<HttpRequestResponse> evidence = requestResponses == null
                ? List.of()
                : List.of(requestResponses);
        return new AuditIssue() {
            @Override public String name() { return name; }
            @Override public String detail() { return detail; }
            @Override public String remediation() { return remediation; }
            @Override public String baseUrl() { return baseUrl; }
            @Override public AuditIssueSeverity severity() { return severity; }
            @Override public AuditIssueConfidence confidence() { return confidence; }
            @Override public List<HttpRequestResponse> requestResponses() { return evidence; }
        };
    }
}
