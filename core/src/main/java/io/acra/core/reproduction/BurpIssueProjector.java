package io.acra.core.reproduction;

import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingSeverity;
import java.net.URI;

public final class BurpIssueProjector {

    public BurpIssueProjection project(ReproductionPackage reproductionPackage, String origin) {
        if (reproductionPackage == null) throw new IllegalArgumentException("reproductionPackage required");
        if (reproductionPackage.candidateState() != FindingCandidateState.CANDIDATE) {
            throw new IllegalArgumentException("only CANDIDATE packages may project to Burp Issue");
        }
        if (!reproductionPackage.reviewOnly()) {
            throw new IllegalArgumentException("reproductionPackage must be review-only");
        }

        String baseUrl = issueUrl(origin, reproductionPackage.endpoint());
        BurpIssueSeverity severity = severity(reproductionPackage.severity());
        BurpIssueConfidence confidence = confidence(reproductionPackage.confidence());

        String detail = "Review-only ACRA authorization candidate; not automatically confirmed. "
                + "candidate=" + reproductionPackage.candidateId()
                + " expected=" + reproductionPackage.expectedDecision()
                + " observed=" + reproductionPackage.observedDecision()
                + " resource=" + reproductionPackage.resourceId()
                + " package=" + reproductionPackage.packageId()
                + " evidence=" + reproductionPackage.evidenceIds()
                + " summary=" + reproductionPackage.summary();

        return new BurpIssueProjection(
                "",
                "ACRA Authorization Candidate (Review Required)",
                detail,
                "Review the authorization policy and supporting evidence, reproduce in an authorized environment, "
                        + "and confirm impact before treating this candidate as a vulnerability.",
                baseUrl,
                severity,
                confidence,
                "ACRA exported an evidence-backed authorization candidate for human review. "
                        + "Candidate state does not equal confirmed vulnerability.",
                "Apply remediation only after authorized reproduction confirms the policy violation and impact.",
                severity,
                reproductionPackage.packageId(),
                reproductionPackage.candidateState(),
                true,
                reproductionPackage.evidenceIds(),
                "");
    }

    private static String issueUrl(String origin, String endpoint) {
        if (origin == null || origin.isBlank()) throw new IllegalArgumentException("origin required");
        URI parsed = URI.create(origin.strip());
        if (!("http".equalsIgnoreCase(parsed.getScheme()) || "https".equalsIgnoreCase(parsed.getScheme()))
                || parsed.getHost() == null || parsed.getUserInfo() != null
                || parsed.getQuery() != null || parsed.getFragment() != null) {
            throw new IllegalArgumentException("origin must be an absolute http(s) origin");
        }
        String endpointPath = endpoint == null ? "" : endpoint.strip();
        int query = endpointPath.indexOf('?');
        if (query >= 0) endpointPath = endpointPath.substring(0, query);
        int fragment = endpointPath.indexOf('#');
        if (fragment >= 0) endpointPath = endpointPath.substring(0, fragment);
        if (!endpointPath.startsWith("/")) throw new IllegalArgumentException("endpoint must be an absolute path");

        String authority = parsed.getScheme().toLowerCase() + "://" + parsed.getAuthority();
        return authority + endpointPath;
    }

    private static BurpIssueSeverity severity(FindingSeverity severity) {
        return switch (severity == null ? FindingSeverity.INFO : severity) {
            case INFO -> BurpIssueSeverity.INFORMATION;
            case LOW -> BurpIssueSeverity.LOW;
            case MEDIUM -> BurpIssueSeverity.MEDIUM;
            case HIGH, CRITICAL -> BurpIssueSeverity.HIGH;
        };
    }

    private static BurpIssueConfidence confidence(String confidence) {
        String value = confidence == null ? "" : confidence.strip().toUpperCase(java.util.Locale.ROOT);
        return "HIGH".equals(value) ? BurpIssueConfidence.FIRM : BurpIssueConfidence.TENTATIVE;
    }
}
