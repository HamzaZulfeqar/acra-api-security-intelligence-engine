package io.acra.burp.scanner;

import burp.api.montoya.scanner.audit.issues.AuditIssue;
import burp.api.montoya.scanner.audit.issues.AuditIssueConfidence;
import burp.api.montoya.scanner.audit.issues.AuditIssueSeverity;
import io.acra.core.reporting.s12.S12BurpIssueProjection;
import java.util.List;

public final class S12MontoyaAuditIssueAdapter {

    public S12MontoyaAuditIssueSpec spec(
            S12BurpIssueProjection projection,
            S12BurpIssuePublicationApproval approval) {
        requireApproval(projection, approval);
        return new S12MontoyaAuditIssueSpec(
                projection.title(),
                projection.detail(),
                projection.remediation(),
                approval.baseUrl(),
                AuditIssueSeverity.INFORMATION,
                AuditIssueConfidence.TENTATIVE,
                "ACRA authorization review candidate. This imported issue is review-only and does not represent automatic vulnerability confirmation.",
                "Validate the supporting evidence and authorization policy before treating the candidate as a confirmed vulnerability.",
                AuditIssueSeverity.INFORMATION);
    }

    public AuditIssue createAuditIssue(
            S12BurpIssueProjection projection,
            S12BurpIssuePublicationApproval approval) {
        S12MontoyaAuditIssueSpec spec = spec(projection, approval);
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

    private static void requireApproval(
            S12BurpIssueProjection projection,
            S12BurpIssuePublicationApproval approval) {
        if (projection == null || approval == null) {
            throw new IllegalArgumentException("projection/approval required");
        }
        if (projection.publishable()) {
            throw new IllegalArgumentException("core projection must remain non-publishable by default");
        }
        if (!approval.approved()) {
            throw new IllegalArgumentException("explicit Burp issue publication approval required");
        }
        if (!projection.candidateId().equals(approval.candidateId())) {
            throw new IllegalArgumentException("publication approval candidate mismatch");
        }
    }
}
