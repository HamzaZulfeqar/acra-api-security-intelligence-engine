package io.acra.burp.tests.sprint12;

import burp.api.montoya.scanner.audit.issues.AuditIssue;
import io.acra.burp.scanner.BurpIssueDraftAdapter;

public final class Sprint12BurpIssueAdapterCompileTest {
    private Sprint12BurpIssueAdapterCompileTest() { }

    public static Class<?> auditIssueType() {
        return AuditIssue.class;
    }

    public static String adapterStatus() {
        return new BurpIssueDraftAdapter().status();
    }
}
