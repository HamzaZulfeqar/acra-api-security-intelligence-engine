package io.acra.burp.scanner;

import burp.api.montoya.scanner.audit.issues.AuditIssue;
import java.util.List;

public final class S12DefaultMontoyaAuditIssueFactory implements S12MontoyaAuditIssueFactory {
    @Override
    public AuditIssue create(S12MontoyaAuditIssueSpec spec) {
        if (spec == null) throw new IllegalArgumentException("spec required");
        return AuditIssue.auditIssue(
                spec.name(),
                spec.detail(),
                spec.remediation(),
                spec.baseUrl(),
                spec.severity(),
                spec.confidence(),
                spec.background(),
                spec.remediationBackground(),
                spec.typicalSeverity(),
                List.of());
    }
}
