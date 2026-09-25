package io.acra.burp.scanner;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.scanner.audit.issues.AuditIssue;
import burp.api.montoya.scanner.audit.issues.AuditIssueConfidence;
import burp.api.montoya.scanner.audit.issues.AuditIssueSeverity;
import io.acra.core.reporting.reproduction.BurpIssueDraft;
import io.acra.core.reporting.reproduction.BurpIssueSubmissionState;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public final class BurpIssueDraftAdapter {

    public BurpIssueProjection project(
            BurpIssueDraft draft,
            String absoluteBaseUrl,
            HttpRequestResponse... requestResponses) {
        validate(draft);
        String url = absoluteUrl(absoluteBaseUrl, draft.path());
        return new BurpIssueProjection(
                draft.name(),
                draft.detail(),
                draft.remediation(),
                url,
                AuditIssueSeverity.INFORMATION,
                AuditIssueConfidence.TENTATIVE,
                draft.background(),
                draft.remediationBackground(),
                AuditIssueSeverity.INFORMATION,
                requestResponses == null ? List.of() : List.of(requestResponses));
    }

    public AuditIssue toAuditIssue(
            BurpIssueDraft draft,
            String absoluteBaseUrl,
            HttpRequestResponse... requestResponses) {
        BurpIssueProjection projection = project(draft, absoluteBaseUrl, requestResponses);
        try {
            return AuditIssue.auditIssue(
                    projection.name(),
                    projection.detail(),
                    projection.remediation(),
                    projection.baseUrl(),
                    projection.severity(),
                    projection.confidence(),
                    projection.background(),
                    projection.remediationBackground(),
                    projection.typicalSeverity(),
                    projection.requestResponses());
        } catch (RuntimeException failure) {
            throw new IllegalStateException(
                    "Montoya AuditIssue factory unavailable; real Burp runtime required",
                    failure);
        }
    }

    public String status() {
        return "PROJECTION_ONLY_NOT_SUBMITTED";
    }

    private static void validate(BurpIssueDraft draft) {
        if (draft == null) throw new IllegalArgumentException("draft required");
        if (draft.confirmed()) throw new IllegalArgumentException("confirmed draft unsupported");
        if (draft.submissionState() != BurpIssueSubmissionState.NOT_SUBMITTED) {
            throw new IllegalArgumentException("only non-submitted drafts can be projected");
        }
    }

    private static String absoluteUrl(String baseUrl, String path) {
        try {
            URI base = new URI(baseUrl == null ? "" : baseUrl);
            if (!base.isAbsolute()
                    || (!"http".equalsIgnoreCase(base.getScheme())
                    && !"https".equalsIgnoreCase(base.getScheme()))) {
                throw new IllegalArgumentException("absolute HTTP(S) base URL required");
            }
            String normalizedBase = base.toString().endsWith("/") ? base.toString() : base + "/";
            String relative = path == null ? "" : path.strip();
            if (relative.startsWith("/")) relative = relative.substring(1);
            return new URI(normalizedBase).resolve(relative).toString();
        } catch (URISyntaxException failure) {
            throw new IllegalArgumentException("valid base URL required", failure);
        }
    }
}
