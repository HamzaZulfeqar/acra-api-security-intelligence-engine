package io.acra.burp.scanner;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.scanner.audit.issues.AuditIssue;
import burp.api.montoya.scanner.audit.issues.AuditIssueConfidence;
import burp.api.montoya.scanner.audit.issues.AuditIssueSeverity;
import io.acra.core.reporting.reproduction.BurpIssueDraft;
import io.acra.core.reporting.reproduction.BurpIssueSubmissionState;
import java.net.URI;
import java.net.URISyntaxException;

public final class BurpIssueDraftAdapter {

    public AuditIssue toAuditIssue(
            BurpIssueDraft draft,
            String absoluteBaseUrl,
            HttpRequestResponse... requestResponses) {
        if (draft == null) throw new IllegalArgumentException("draft required");
        if (draft.confirmed()) throw new IllegalArgumentException("confirmed draft unsupported");
        if (draft.submissionState() != BurpIssueSubmissionState.NOT_SUBMITTED) {
            throw new IllegalArgumentException("only non-submitted drafts can be projected");
        }

        String url = absoluteUrl(absoluteBaseUrl, draft.path());
        return AuditIssue.auditIssue(
                draft.name(),
                draft.detail(),
                draft.remediation(),
                url,
                AuditIssueSeverity.INFORMATION,
                AuditIssueConfidence.TENTATIVE,
                draft.background(),
                draft.remediationBackground(),
                AuditIssueSeverity.INFORMATION,
                requestResponses == null ? new HttpRequestResponse[0] : requestResponses);
    }

    public String status() {
        return "PROJECTION_ONLY_NOT_SUBMITTED";
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
