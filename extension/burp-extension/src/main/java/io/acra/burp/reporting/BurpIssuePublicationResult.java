package io.acra.burp.reporting;

import burp.api.montoya.scanner.audit.issues.AuditIssue;

public record BurpIssuePublicationResult(
        BurpIssuePublicationStatus status,
        String reproductionFingerprint,
        AuditIssue issue) {

    public BurpIssuePublicationResult {
        if (status == null) throw new IllegalArgumentException("status required");
        if (reproductionFingerprint == null || reproductionFingerprint.isBlank()) {
            throw new IllegalArgumentException("reproductionFingerprint required");
        }
        if (issue == null) throw new IllegalArgumentException("issue required");
    }
}
