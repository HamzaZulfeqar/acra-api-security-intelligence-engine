package io.acra.burp.tests.sprint12;

import io.acra.burp.scanner.S12BurpIssuePublicationApproval;
import io.acra.burp.scanner.S12BurpIssuePublicationReceipt;
import java.util.Objects;

public final class Sprint12PublicationSecurityHardeningTestSuite {
    private Sprint12PublicationSecurityHardeningTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT12_PUBLICATION_SECURITY_HARDENING PASS assertions=" + assertions);
    }

    public static int run() {
        int assertions = 0;

        var valid = new S12BurpIssuePublicationApproval(
                "candidate-safe",
                true,
                "approval-safe",
                "https://acra-lab.invalid/api/v1/documents/1002");
        assertEquals("https://acra-lab.invalid/api/v1/documents/1002", valid.baseUrl(),
                "valid absolute HTTP(S) base URL remains accepted");
        assertions++;

        assertThrows(IllegalArgumentException.class,
                () -> new S12BurpIssuePublicationApproval(
                        "candidate-safe", true, "approval-safe",
                        "https://user:password@acra-lab.invalid/api/v1/documents/1002"),
                "publication URL userinfo fails closed");
        assertions++;

        assertThrows(IllegalArgumentException.class,
                () -> new S12BurpIssuePublicationApproval(
                        "candidate-safe", true, "approval-safe",
                        "https://acra-lab.invalid/api/v1/documents/1002?mode=review"),
                "publication URL query fails closed");
        assertions++;

        assertThrows(IllegalArgumentException.class,
                () -> new S12BurpIssuePublicationApproval(
                        "candidate-safe", true, "approval-safe",
                        "https://acra-lab.invalid/api/v1/documents/1002#fragment"),
                "publication URL fragment fails closed");
        assertions++;

        assertThrows(IllegalArgumentException.class,
                () -> new S12BurpIssuePublicationApproval(
                        "candidate-safe", true, "approval-safe",
                        "ftp://acra-lab.invalid/api/v1/documents/1002"),
                "non-HTTP publication URL fails closed");
        assertions++;

        assertThrows(IllegalArgumentException.class,
                () -> new S12BurpIssuePublicationApproval(
                        "candidate-safe", true,
                        "Authorization: Bearer abcdefghijklmnopqrstuvwxyz",
                        "https://acra-lab.invalid/api/v1/documents/1002"),
                "secret-bearing approval reference fails closed");
        assertions++;

        assertThrows(IllegalArgumentException.class,
                () -> new S12BurpIssuePublicationApproval(
                        "token=supersecret", true, "approval-safe",
                        "https://acra-lab.invalid/api/v1/documents/1002"),
                "secret-bearing candidate ID fails closed");
        assertions++;

        S12BurpIssuePublicationReceipt receipt = new S12BurpIssuePublicationReceipt(
                "",
                "candidate-safe",
                "approval-safe",
                "https://acra-lab.invalid/api/v1/documents/1002",
                "ACRA authorization review candidate",
                S12BurpIssuePublicationReceipt.IMPORTED_REVIEW_CANDIDATE,
                "");
        assertTrue(receipt.receiptId().startsWith("s12-burp-receipt-"),
                "valid publication receipt gets deterministic identity");
        assertions++;

        S12BurpIssuePublicationReceipt repeated = new S12BurpIssuePublicationReceipt(
                "",
                "candidate-safe",
                "approval-safe",
                "https://acra-lab.invalid/api/v1/documents/1002",
                "ACRA authorization review candidate",
                S12BurpIssuePublicationReceipt.IMPORTED_REVIEW_CANDIDATE,
                "");
        assertEquals(receipt.receiptId(), repeated.receiptId(),
                "publication receipt ID is deterministic");
        assertions++;
        assertEquals(receipt.fingerprint(), repeated.fingerprint(),
                "publication receipt fingerprint is deterministic");
        assertions++;

        assertThrows(IllegalArgumentException.class,
                () -> new S12BurpIssuePublicationReceipt(
                        "tampered-id",
                        receipt.candidateId(),
                        receipt.approvalReference(),
                        receipt.baseUrl(),
                        receipt.issueName(),
                        receipt.state(),
                        ""),
                "publication receipt ID tampering fails closed");
        assertions++;

        assertThrows(IllegalArgumentException.class,
                () -> new S12BurpIssuePublicationReceipt(
                        "",
                        receipt.candidateId(),
                        receipt.approvalReference(),
                        receipt.baseUrl(),
                        receipt.issueName(),
                        receipt.state(),
                        "tampered-fingerprint"),
                "publication receipt fingerprint tampering fails closed");
        assertions++;

        assertThrows(IllegalArgumentException.class,
                () -> new S12BurpIssuePublicationReceipt(
                        "",
                        receipt.candidateId(),
                        receipt.approvalReference(),
                        "https://acra-lab.invalid/api/v1/documents/1002?token=supersecret",
                        receipt.issueName(),
                        receipt.state(),
                        ""),
                "receipt URL query/secret material fails closed");
        assertions++;

        assertThrows(IllegalArgumentException.class,
                () -> new S12BurpIssuePublicationReceipt(
                        "",
                        receipt.candidateId(),
                        receipt.approvalReference(),
                        receipt.baseUrl(),
                        receipt.issueName(),
                        "CONFIRMED",
                        ""),
                "unsupported confirmed receipt state fails closed");
        assertions++;

        return assertions;
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertThrows(
            Class<? extends Throwable> expected,
            Runnable action,
            String message) {
        try {
            action.run();
        } catch (Throwable failure) {
            if (expected.isInstance(failure)) return;
            throw new AssertionError(message + " wrong exception=" + failure, failure);
        }
        throw new AssertionError(message + " expected exception=" + expected.getSimpleName());
    }
}
