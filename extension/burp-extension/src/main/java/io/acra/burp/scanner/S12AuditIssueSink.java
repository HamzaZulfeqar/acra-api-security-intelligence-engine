package io.acra.burp.scanner;

import burp.api.montoya.scanner.audit.issues.AuditIssue;

public interface S12AuditIssueSink {
    void add(AuditIssue issue);
}
