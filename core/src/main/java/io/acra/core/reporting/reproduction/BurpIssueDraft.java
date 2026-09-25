package io.acra.core.reporting.reproduction;

import io.acra.core.security.TokenFingerprint;
import io.acra.core.security.UniversalRedactor;
import java.util.List;

public record BurpIssueDraft(
        String draftId,
        String draftVersion,
        String reproductionPackageId,
        String candidateId,
        String name,
        String detail,
        String remediation,
        String path,
        BurpIssueDraftSeverity severity,
        BurpIssueDraftConfidence confidence,
        String background,
        String remediationBackground,
        List<String> evidenceIds,
        boolean confirmed,
        BurpIssueSubmissionState submissionState) {

    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public BurpIssueDraft {
        draftVersion = required(draftVersion, "draftVersion");
        reproductionPackageId = required(reproductionPackageId, "reproductionPackageId");
        candidateId = required(candidateId, "candidateId");
        name = required(name, "name");
        detail = required(detail, "detail");
        remediation = required(remediation, "remediation");
        path = required(path, "path");
        severity = severity == null ? BurpIssueDraftSeverity.INFORMATION : severity;
        confidence = confidence == null ? BurpIssueDraftConfidence.TENTATIVE : confidence;
        background = required(background, "background");
        remediationBackground = safe(remediationBackground);
        evidenceIds = List.copyOf(evidenceIds == null ? List.<String>of() : evidenceIds).stream()
                .map(BurpIssueDraft::safe)
                .filter(value -> !value.isBlank())
                .distinct()
                .sorted()
                .toList();
        if (confirmed) throw new IllegalArgumentException("Burp issue draft cannot be confirmed");
        submissionState = submissionState == null ? BurpIssueSubmissionState.NOT_SUBMITTED : submissionState;
        if (submissionState != BurpIssueSubmissionState.NOT_SUBMITTED) {
            throw new IllegalArgumentException("unsupported Burp issue submission state");
        }

        String calculated = deterministicId(
                draftVersion,
                reproductionPackageId,
                candidateId,
                name,
                detail,
                remediation,
                path,
                severity,
                confidence,
                background,
                remediationBackground,
                evidenceIds,
                confirmed,
                submissionState);
        draftId = draftId == null || draftId.isBlank() ? calculated : draftId.strip();
        if (!draftId.equals(calculated)) {
            throw new IllegalArgumentException("Burp issue draft identity mismatch");
        }
    }

    public static String deterministicId(
            String version,
            String packageId,
            String candidateId,
            String name,
            String detail,
            String remediation,
            String path,
            BurpIssueDraftSeverity severity,
            BurpIssueDraftConfidence confidence,
            String background,
            String remediationBackground,
            List<String> evidenceIds,
            boolean confirmed,
            BurpIssueSubmissionState submissionState) {
        String material = version + "|" + packageId + "|" + candidateId + "|"
                + name + "|" + detail + "|" + remediation + "|" + path + "|"
                + severity + "|" + confidence + "|" + background + "|"
                + remediationBackground + "|" + evidenceIds + "|"
                + confirmed + "|" + submissionState;
        return "acra-burp-draft-" + TokenFingerprint.sha256(material).substring(0, 24);
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
