package io.acra.burp.scanner;

import burp.api.montoya.scanner.audit.issues.AuditIssue;

public interface S12MontoyaAuditIssueFactory {
    AuditIssue create(S12MontoyaAuditIssueSpec spec);
}
