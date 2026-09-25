package io.acra.burp.reporting;

import burp.api.montoya.collaborator.Interaction;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.scanner.audit.issues.AuditIssue;
import burp.api.montoya.scanner.audit.issues.AuditIssueConfidence;
import burp.api.montoya.scanner.audit.issues.AuditIssueDefinition;
import burp.api.montoya.scanner.audit.issues.AuditIssueSeverity;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingConfidence;
import io.acra.core.domain.finding.FindingSeverity;
import io.acra.core.reproduction.ReproductionPackage;
import java.util.List;

public final class ReproductionAuditIssueAdapter {

    public AuditIssue create(
            ReproductionPackage value,
            HttpRequestResponse requestResponse) {
        if (value == null) throw new IllegalArgumentException("reproduction package required");
        if (requestResponse == null
                || requestResponse.request() == null
                || requestResponse.response() == null
                || !requestResponse.hasResponse()) {
            throw new IllegalArgumentException("real request/response evidence required");
        }
        if (value.candidateState() != FindingCandidateState.CANDIDATE || !value.issueEligible()) {
            throw new IllegalArgumentException("only issue-eligible CANDIDATE packages may become Burp issues");
        }

        String baseUrl = requestResponse.request().url();
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("request URL required for Burp issue");
        }

        AuditIssueSeverity severity = severity(value.severity());
        AuditIssueConfidence confidence = confidence(value.confidence());
        String name = "ACRA Authorization Review Candidate";
        String detail = detail(value);
        String remediation = "Validate the authorization policy for this endpoint and reproduce the expected-versus-observed "
                + "decision with an authorized human reviewer before treating this candidate as a confirmed vulnerability.";
        String background = "ACRA correlates API authorization context and evidence. This custom Burp issue represents a "
                + "review-only candidate and does not by itself claim confirmed exploitation.";
        String remediationBackground = "Review resource ownership, tenant boundaries, role/function policy and workflow "
                + "conditions relevant to the supplied evidence before remediation.";

        return new ReviewAuditIssue(
                name,
                detail,
                remediation,
                baseUrl,
                severity,
                confidence,
                background,
                remediationBackground,
                severity,
                requestResponse);
    }

    private static AuditIssueSeverity severity(FindingSeverity severity) {
        return switch (severity) {
            case CRITICAL, HIGH -> AuditIssueSeverity.HIGH;
            case MEDIUM -> AuditIssueSeverity.MEDIUM;
            case LOW -> AuditIssueSeverity.LOW;
            case INFO -> AuditIssueSeverity.INFORMATION;
        };
    }

    private static AuditIssueConfidence confidence(FindingConfidence confidence) {
        return switch (confidence) {
            case HIGH -> AuditIssueConfidence.FIRM;
            case MEDIUM, LOW, INSUFFICIENT -> AuditIssueConfidence.TENTATIVE;
        };
    }

    private static String detail(ReproductionPackage value) {
        return "<b>Review-only authorization candidate.</b> Human validation is required; this is not confirmed exploitation."
                + "<br><br><b>Candidate:</b> " + html(value.candidateId())
                + "<br><b>Endpoint:</b> " + html(value.endpoint())
                + "<br><b>Resource:</b> " + html(value.resourceId())
                + "<br><b>Expected:</b> " + html(value.expectedDecision().name())
                + "<br><b>Observed:</b> " + html(value.observedDecision().name())
                + "<br><b>Dimensions:</b> " + html(value.dimensions().toString())
                + "<br><b>ACRA severity:</b> " + html(value.severity().name())
                + "<br><b>ACRA confidence:</b> " + html(value.confidence().name())
                + "<br><b>Finding fingerprint:</b> " + html(value.findingFingerprint())
                + "<br><b>Reproduction fingerprint:</b> " + html(value.fingerprint())
                + "<br><b>Evidence IDs:</b> " + html(value.evidenceIds().toString())
                + "<br><b>Policy references:</b> " + html(value.policyReferences().toString());
    }

    private static String html(String value) {
        String out = value == null ? "" : value;
        return out.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace(""", "&quot;")
                .replace("'", "&#39;");
    }

    private static final class ReviewAuditIssue implements AuditIssue {
        private final String name;
        private final String detail;
        private final String remediation;
        private final String baseUrl;
        private final AuditIssueSeverity severity;
        private final AuditIssueConfidence confidence;
        private final AuditIssueDefinition definition;
        private final HttpRequestResponse requestResponse;

        private ReviewAuditIssue(
                String name,
                String detail,
                String remediation,
                String baseUrl,
                AuditIssueSeverity severity,
                AuditIssueConfidence confidence,
                String background,
                String remediationBackground,
                AuditIssueSeverity typicalSeverity,
                HttpRequestResponse requestResponse) {
            this.name = name;
            this.detail = detail;
            this.remediation = remediation;
            this.baseUrl = baseUrl;
            this.severity = severity;
            this.confidence = confidence;
            this.definition = new ReviewAuditIssueDefinition(
                    name, background, remediationBackground, typicalSeverity);
            this.requestResponse = requestResponse;
        }

        @Override public String name() { return name; }
        @Override public String detail() { return detail; }
        @Override public String remediation() { return remediation; }
        @Override public HttpService httpService() { return requestResponse.httpService(); }
        @Override public String baseUrl() { return baseUrl; }
        @Override public AuditIssueSeverity severity() { return severity; }
        @Override public AuditIssueConfidence confidence() { return confidence; }
        @Override public List<HttpRequestResponse> requestResponses() { return List.of(requestResponse); }
        @Override public List<Interaction> collaboratorInteractions() { return List.of(); }
        @Override public AuditIssueDefinition definition() { return definition; }
    }

    private record ReviewAuditIssueDefinition(
            String name,
            String background,
            String remediation,
            AuditIssueSeverity typicalSeverity) implements AuditIssueDefinition {
        @Override public int typeIndex() { return 0; }
    }
}
