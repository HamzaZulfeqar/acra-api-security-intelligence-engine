package io.acra.burp.reporting;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.scanner.audit.issues.AuditIssue;
import burp.api.montoya.scanner.audit.issues.AuditIssueConfidence;
import burp.api.montoya.scanner.audit.issues.AuditIssueSeverity;
import io.acra.core.reproduction.BurpIssueConfidence;
import io.acra.core.reproduction.BurpIssueProjection;
import io.acra.core.reproduction.BurpIssueSeverity;
import java.util.List;

public final class BurpAuditIssueAdapter {

    public AuditIssue create(
            BurpIssueProjection projection,
            List<HttpRequestResponse> requestResponses) {
        if (projection == null) throw new IllegalArgumentException("projection required");
        List<HttpRequestResponse> messages =
                List.copyOf(requestResponses == null ? List.of() : requestResponses);

        return AuditIssue.auditIssue(
                projection.name(),
                projection.detail(),
                projection.remediation(),
                projection.baseUrl(),
                severity(projection.severity()),
                confidence(projection.confidence()),
                projection.background(),
                projection.remediationBackground(),
                severity(projection.typicalSeverity()),
                messages);
    }

    public void addReviewIssueToSiteMap(
            MontoyaApi api,
            BurpIssueProjection projection,
            List<HttpRequestResponse> requestResponses) {
        if (api == null) throw new IllegalArgumentException("api required");
        api.siteMap().add(create(projection, requestResponses));
    }

    private static AuditIssueSeverity severity(BurpIssueSeverity severity) {
        return switch (severity) {
            case INFORMATION -> AuditIssueSeverity.INFORMATION;
            case LOW -> AuditIssueSeverity.LOW;
            case MEDIUM -> AuditIssueSeverity.MEDIUM;
            case HIGH -> AuditIssueSeverity.HIGH;
        };
    }

    private static AuditIssueConfidence confidence(BurpIssueConfidence confidence) {
        return switch (confidence) {
            case TENTATIVE -> AuditIssueConfidence.TENTATIVE;
            case FIRM -> AuditIssueConfidence.FIRM;
        };
    }
}
