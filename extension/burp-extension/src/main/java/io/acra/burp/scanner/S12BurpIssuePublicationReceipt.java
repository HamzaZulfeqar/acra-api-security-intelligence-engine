package io.acra.burp.scanner;

import io.acra.core.security.TokenFingerprint;
import io.acra.core.security.UniversalRedactor;
import java.net.URI;

public record S12BurpIssuePublicationReceipt(
        String receiptId,
        String candidateId,
        String approvalReference,
        String baseUrl,
        String issueName,
        String state,
        String fingerprint) {

    public static final String IMPORTED_REVIEW_CANDIDATE = "IMPORTED_REVIEW_CANDIDATE";
    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public S12BurpIssuePublicationReceipt {
        candidateId = safeRequired(candidateId, "candidateId");
        approvalReference = safeRequired(approvalReference, "approvalReference");
        baseUrl = safeRequired(baseUrl, "baseUrl");
        validateBaseUrl(baseUrl);
        issueName = safeRequired(issueName, "issueName");
        state = safeRequired(state, "state");
        if (!IMPORTED_REVIEW_CANDIDATE.equals(state)) {
            throw new IllegalArgumentException("unsupported publication receipt state");
        }

        String material = candidateId + "|" + approvalReference + "|" + baseUrl + "|" + issueName + "|" + state;
        String expectedReceiptId = "s12-burp-receipt-"
                + TokenFingerprint.sha256(material).substring(0, 24);
        receiptId = receiptId == null || receiptId.isBlank() ? expectedReceiptId : receiptId;
        if (!receiptId.equals(expectedReceiptId)) {
            throw new IllegalArgumentException("publication receipt identity mismatch");
        }

        String calculated = TokenFingerprint.sha256(receiptId + "|" + material);
        fingerprint = fingerprint == null || fingerprint.isBlank() ? calculated : fingerprint;
        if (!fingerprint.equals(calculated)) {
            throw new IllegalArgumentException("publication receipt fingerprint mismatch");
        }
    }

    private static void validateBaseUrl(String baseUrl) {
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
