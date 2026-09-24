package io.acra.burp.scanner;

import io.acra.core.security.TokenFingerprint;

public record S12BurpIssuePublicationReceipt(
        String receiptId,
        String candidateId,
        String approvalReference,
        String baseUrl,
        String issueName,
        String state,
        String fingerprint) {

    public static final String IMPORTED_REVIEW_CANDIDATE = "IMPORTED_REVIEW_CANDIDATE";

    public S12BurpIssuePublicationReceipt {
        candidateId = required(candidateId, "candidateId");
        approvalReference = required(approvalReference, "approvalReference");
        baseUrl = required(baseUrl, "baseUrl");
        issueName = required(issueName, "issueName");
        state = required(state, "state");
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

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " required");
        return value.strip();
    }
}
