package io.acra.core.reproduction;

import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.security.UniversalRedactor;
import java.util.List;

public record BurpIssueProjection(
        String projectionId,
        String name,
        String detail,
        String remediation,
        String baseUrl,
        BurpIssueSeverity severity,
        BurpIssueConfidence confidence,
        String background,
        String remediationBackground,
        BurpIssueSeverity typicalSeverity,
        String packageId,
        FindingCandidateState candidateState,
        boolean reviewOnly,
        List<String> evidenceIds,
        String fingerprint) {

    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public BurpIssueProjection {
        name = required(name, "name");
        detail = required(detail, "detail");
        remediation = required(remediation, "remediation");
        baseUrl = required(baseUrl, "baseUrl");
        if (severity == null || confidence == null || typicalSeverity == null) {
            throw new IllegalArgumentException("severity/confidence required");
        }
        background = required(background, "background");
        remediationBackground = required(remediationBackground, "remediationBackground");
        packageId = required(packageId, "packageId");
        if (candidateState != FindingCandidateState.CANDIDATE) {
            throw new IllegalArgumentException("Burp Issue projection requires CANDIDATE state");
        }
        if (!reviewOnly) throw new IllegalArgumentException("Burp Issue projection must remain review-only");
        evidenceIds = List.copyOf(evidenceIds == null ? List.<String>of() : evidenceIds).stream()
                .map(BurpIssueProjection::safe)
                .filter(value -> !value.isBlank())
                .distinct()
                .sorted()
                .toList();
        if (evidenceIds.isEmpty()) throw new IllegalArgumentException("evidenceIds required");

        String material = String.join("|",
                name, detail, remediation, baseUrl, severity.name(), confidence.name(),
                background, remediationBackground, typicalSeverity.name(),
                packageId, candidateState.name(), Boolean.toString(reviewOnly), evidenceIds.toString());
        String expectedId = "burp-issue-" + TokenFingerprint.sha256(material).substring(0, 24);
        projectionId = projectionId == null || projectionId.isBlank() ? expectedId : projectionId.strip();
        if (!projectionId.equals(expectedId)) throw new IllegalArgumentException("projectionId mismatch");
        String calculated = TokenFingerprint.sha256(projectionId + "|" + material);
        fingerprint = fingerprint == null || fingerprint.isBlank() ? calculated : fingerprint.strip();
        if (!fingerprint.equals(calculated)) throw new IllegalArgumentException("projection fingerprint mismatch");
    }

    private static String required(String value, String name) {
        String safe = safe(value);
        if (safe.isBlank()) throw new IllegalArgumentException(name + " required");
        return safe;
    }

    private static String safe(String value) {
        return REDACTOR.redactText(value == null ? "" : value).strip();
    }
}
