package io.acra.burp.scanner;

import burp.api.montoya.scanner.audit.issues.AuditIssue;
import io.acra.core.reporting.s12.S12BurpIssueProjection;

public final class S12BurpIssuePublisher {
    private final S12MontoyaAuditIssueAdapter adapter;
    private final S12MontoyaAuditIssueFactory factory;
    private final S12AuditIssueSink sink;

    public S12BurpIssuePublisher(
            S12MontoyaAuditIssueAdapter adapter,
            S12MontoyaAuditIssueFactory factory,
            S12AuditIssueSink sink) {
        if (adapter == null || factory == null || sink == null) {
            throw new IllegalArgumentException("adapter/factory/sink required");
        }
        this.adapter = adapter;
        this.factory = factory;
        this.sink = sink;
    }

    public S12BurpIssuePublicationReceipt publish(
            S12BurpIssueProjection projection,
            S12BurpIssuePublicationApproval approval) {
        S12MontoyaAuditIssueSpec spec = adapter.spec(projection, approval);
        AuditIssue issue = factory.create(spec);
        if (issue == null) throw new IllegalStateException("issue factory returned null");
        sink.add(issue);

        return new S12BurpIssuePublicationReceipt(
                "",
                projection.candidateId(),
                approval.approvalReference(),
                approval.baseUrl(),
                spec.name(),
                S12BurpIssuePublicationReceipt.IMPORTED_REVIEW_CANDIDATE,
                "");
    }
}
