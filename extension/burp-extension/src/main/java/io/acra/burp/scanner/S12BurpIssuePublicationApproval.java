package io.acra.burp.scanner;

import io.acra.core.security.UniversalRedactor;
import java.net.URI;

public record S12BurpIssuePublicationApproval(
        String candidateId,
        boolean approved,
        String approvalReference,
        String baseUrl) {

    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public S12BurpIssuePublicationApproval {
        candidateId = safeRequired(candidateId, "candidateId");
        approvalReference = safeRequired(approvalReference, "approvalReference");
        baseUrl = safeRequired(baseUrl, "baseUrl");
        URI parsed = URI.create(baseUrl);
        if (!parsed.isAbsolute()
                || (!"http".equalsIgnoreCase(parsed.getScheme())
                && !"https".equalsIgnoreCase(parsed.getScheme()))
                || parsed.getHost() == null
                || parsed.getHost().isBlank()
                || parsed.getRawUserInfo() != null
                || parsed.getRawQuery() != null
                || parsed.getRawFragment() != null) {
            throw new IllegalArgumentException(
                    "baseUrl must be absolute http/https URL without credentials, query or fragment");
        }
    }

    private static String safeRequired(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " required");
        String stripped = value.strip();
        String redacted = REDACTOR.redactText(stripped);
        if (!stripped.equals(redacted)) {
            throw new IllegalArgumentException(name + " contains secret-bearing material");
        }
        return stripped;
    }
}
