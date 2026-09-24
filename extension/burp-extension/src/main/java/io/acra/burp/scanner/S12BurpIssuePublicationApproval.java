package io.acra.burp.scanner;

import java.net.URI;

public record S12BurpIssuePublicationApproval(
        String candidateId,
        boolean approved,
        String approvalReference,
        String baseUrl) {

    public S12BurpIssuePublicationApproval {
        candidateId = required(candidateId, "candidateId");
        approvalReference = required(approvalReference, "approvalReference");
        baseUrl = required(baseUrl, "baseUrl");
        URI parsed = URI.create(baseUrl);
        if (!parsed.isAbsolute()
                || (!"http".equalsIgnoreCase(parsed.getScheme())
                && !"https".equalsIgnoreCase(parsed.getScheme()))) {
            throw new IllegalArgumentException("baseUrl must be absolute http/https URL");
        }
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " required");
        return value.strip();
    }
}
