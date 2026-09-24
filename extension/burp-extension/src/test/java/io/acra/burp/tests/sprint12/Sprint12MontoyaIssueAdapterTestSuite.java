package io.acra.burp.tests.sprint12;

import burp.api.montoya.scanner.audit.issues.AuditIssue;
import burp.api.montoya.scanner.audit.issues.AuditIssueConfidence;
import burp.api.montoya.scanner.audit.issues.AuditIssueSeverity;
import io.acra.burp.scanner.S12BurpIssuePublicationApproval;
import io.acra.burp.scanner.S12MontoyaAuditIssueAdapter;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.reporting.s12.S12BurpIssueProjector;
import io.acra.core.reporting.s12.S12ReproductionPackageFactory;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public final class Sprint12MontoyaIssueAdapterTestSuite {
    private Sprint12MontoyaIssueAdapterTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT12_MONTOYA_ISSUE_ADAPTER PASS assertions=" + assertions);
    }

    public static int run() {
        var reproduction = new S12ReproductionPackageFactory().from(fixture());
        var projection = new S12BurpIssueProjector().project(reproduction);
        var adapter = new S12MontoyaAuditIssueAdapter();
        int assertions = 0;

        TestSupport.assertTrue(!projection.publishable(),
                "core Burp projection remains non-publishable");
        assertions++;

        var denied = new S12BurpIssuePublicationApproval(
                projection.candidateId(),
                false,
                "approval-denied",
                "https://acra-lab.invalid/api/v1/documents/1002");
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> adapter.spec(projection, denied),
                "adapter rejects missing explicit approval");
        assertions++;

        var mismatch = new S12BurpIssuePublicationApproval(
                "other-candidate",
                true,
                "approval-mismatch",
                "https://acra-lab.invalid/api/v1/documents/1002");
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> adapter.spec(projection, mismatch),
                "adapter rejects approval for another candidate");
        assertions++;

        var approved = new S12BurpIssuePublicationApproval(
                projection.candidateId(),
                true,
                "human-review-approval-001",
                "https://acra-lab.invalid/api/v1/documents/1002");
        var spec = adapter.spec(projection, approved);

        TestSupport.assertEquals(AuditIssueSeverity.INFORMATION, spec.severity(),
                "Montoya issue spec remains informational");
        assertions++;
        TestSupport.assertEquals(AuditIssueConfidence.TENTATIVE, spec.confidence(),
                "Montoya issue spec remains tentative");
        assertions++;
        TestSupport.assertEquals(AuditIssueSeverity.INFORMATION, spec.typicalSeverity(),
                "typical severity remains informational");
        assertions++;
        TestSupport.assertEquals(approved.baseUrl(), spec.baseUrl(),
                "explicit approved absolute URL is used for Montoya issue");
        assertions++;
        TestSupport.assertContains(spec.detail(), "Human verification is required",
                "Montoya issue detail retains human-review boundary");
        assertions++;
        TestSupport.assertContains(spec.background(), "review-only",
                "Montoya issue background explicitly states review-only");
        assertions++;
        TestSupport.assertNotContains(spec.detail(), "DummyPassword",
                "Montoya issue detail excludes candidate rationale secret");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> new S12BurpIssuePublicationApproval(
                        projection.candidateId(),
                        true,
                        "bad-url",
                        "/api/v1/documents/1002"),
                "relative publication URL fails closed");
        assertions++;

        Method create = Arrays.stream(S12MontoyaAuditIssueAdapter.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("createAuditIssue"))
                .findFirst()
                .orElseThrow();
        TestSupport.assertEquals(AuditIssue.class, create.getReturnType(),
                "extension adapter compiles against real Montoya AuditIssue type");
        assertions++;

        boolean hasPublishMethod = Arrays.stream(S12MontoyaAuditIssueAdapter.class.getDeclaredMethods())
                .map(Method::getName)
                .anyMatch(name -> name.equals("publish") || name.equals("addToSiteMap"));
        TestSupport.assertTrue(!hasPublishMethod,
                "Phase 2 adapter exposes no publication method");
        assertions++;

        return assertions;
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertContains(String text, String expected, String message) {
        if (text == null || !text.contains(expected)) {
            throw new AssertionError(message + " missing=" + expected);
        }
    }

    private static void assertNotContains(String text, String forbidden, String message) {
        if (text != null && text.contains(forbidden)) {
            throw new AssertionError(message + " forbidden=" + forbidden);
        }
    }

    private static void assertThrows(
            Class<? extends Throwable> expectedType,
            Runnable action,
            String message) {
        try {
            action.run();
        } catch (Throwable failure) {
            if (expectedType.isInstance(failure)) return;
            throw new AssertionError(message + " wrong exception=" + failure, failure);
        }
        throw new AssertionError(message + " expected exception=" + expectedType.getSimpleName());
    }

    private static FindingCandidate fixture() {
        return new FindingCandidate(
                "s12-montoya-candidate",
                FindingCandidateState.CANDIDATE,
                "acra-s12",
                List.of("test-s12"),
                List.of("execution-s12"),
                List.of("observation-s12"),
                List.of("assessment-s12"),
                List.of("OBJECT_AUTHORIZATION"),
                "/api/v1/documents/1002",
                "document:1002",
                "user-a",
                "FOREIGN_TENANT",
                AuthorizationDecision.DENY,
                AuthorizationDecision.ALLOW,
                List.of("evidence-s12-a"),
                List.of(),
                List.of("policy-s12"),
                "HIGH",
                "Review fixture; password=DummyPassword",
                FindingFingerprint.of(
                        "/api/v1/documents/1002",
                        "document:1002",
                        "user-a",
                        "FOREIGN_TENANT",
                        "DENY_TO_ALLOW",
                        "OBJECT_AUTHORIZATION"));
    }
}
