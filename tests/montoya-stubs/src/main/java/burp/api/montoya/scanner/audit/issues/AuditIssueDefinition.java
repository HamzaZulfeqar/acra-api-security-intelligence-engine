package burp.api.montoya.scanner.audit.issues;

public interface AuditIssueDefinition {
    String name();
    String background();
    String remediation();
    AuditIssueSeverity typicalSeverity();
    int typeIndex();
}
