package io.acra.core.tests.sprint12;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.domain.finding.FindingSeverity;
import io.acra.core.reproduction.BurpIssueConfidence;
import io.acra.core.reproduction.BurpIssueProjector;
import io.acra.core.reproduction.BurpIssueSeverity;
import io.acra.core.reproduction.ReproductionPackageProjector;
import io.acra.core.tests.TestSupport;
import java.util.List;

public final class Sprint12BurpIssueProjectionTestSuite {
    private Sprint12BurpIssueProjectionTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT12_BURP_ISSUE_PROJECTION PASS assertions=" + assertions);
    }

    public static int run() {
        int assertions = 0;
        BurpIssueProjector projector = new BurpIssueProjector();
        var pkg = new ReproductionPackageProjector().project(
                candidate(FindingCandidateState.CANDIDATE, "HIGH"),
                FindingSeverity.CRITICAL,
                "Authorization mismatch under review");
        var issue = projector.project(pkg, "https://api.example.test");

        TestSupport.assertEquals("ACRA Authorization Candidate (Review Required)", issue.name(),
                "Burp issue name preserves review boundary");
        assertions++;
        TestSupport.assertTrue(issue.reviewOnly(),
                "Burp issue projection is review-only");
        assertions++;
        TestSupport.assertEquals(FindingCandidateState.CANDIDATE, issue.candidateState(),
                "Burp issue projection only represents candidate state");
        assertions++;
        TestSupport.assertEquals(BurpIssueSeverity.HIGH, issue.severity(),
                "CRITICAL ACRA severity is bounded to Burp HIGH");
        assertions++;
        TestSupport.assertEquals(BurpIssueConfidence.FIRM, issue.confidence(),
                "HIGH ACRA confidence maps to FIRM, never CERTAIN");
        assertions++;
        TestSupport.assertEquals("https://api.example.test/api/v1/documents/42", issue.baseUrl(),
                "issue URL combines origin with sanitized endpoint path");
        assertions++;
        TestSupport.assertContains(issue.detail(), "not automatically confirmed",
                "issue detail explicitly prevents automatic confirmation");
        assertions++;
        TestSupport.assertContains(issue.detail(), "expected=DENY",
                "issue detail retains expected decision");
        assertions++;
        TestSupport.assertContains(issue.detail(), "observed=ALLOW",
                "issue detail retains observed decision");
        assertions++;
        TestSupport.assertContains(issue.detail(), "evidence-burp-1",
                "issue detail retains evidence lineage");
        assertions++;
        TestSupport.assertEquals(64, issue.fingerprint().length(),
                "Burp issue projection fingerprint is SHA-256");
        assertions++;

        var repeated = projector.project(pkg, "https://api.example.test");
        TestSupport.assertEquals(issue.projectionId(), repeated.projectionId(),
                "Burp issue projection ID is deterministic");
        assertions++;
        TestSupport.assertEquals(issue.fingerprint(), repeated.fingerprint(),
                "Burp issue projection fingerprint is deterministic");
        assertions++;

        var tentative = projector.project(
                new ReproductionPackageProjector().project(
                        candidate(FindingCandidateState.CANDIDATE, "MEDIUM"),
                        FindingSeverity.MEDIUM,
                        "review"),
                "http://127.0.0.1:8080");
        TestSupport.assertEquals(BurpIssueConfidence.TENTATIVE, tentative.confidence(),
                "non-HIGH candidate confidence stays TENTATIVE");
        assertions++;
        TestSupport.assertEquals(BurpIssueSeverity.MEDIUM, tentative.severity(),
                "MEDIUM severity maps directly");
        assertions++;

        var secret = projector.project(
                new ReproductionPackageProjector().project(
                        secretCandidate(), FindingSeverity.HIGH,
                        "Bearer aaa.bbb.ccc token=topsecret"),
                "https://api.example.test");
        TestSupport.assertNotContains(secret.baseUrl(), "topsecret",
                "Burp issue URL strips query secret material");
        assertions++;
        TestSupport.assertNotContains(secret.detail(), "aaa.bbb.ccc",
                "Burp issue detail redacts bearer-like secret");
        assertions++;
        TestSupport.assertNotContains(secret.detail(), "topsecret",
                "Burp issue detail redacts token value");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> projector.project(
                        new ReproductionPackageProjector().project(
                                candidate(FindingCandidateState.REJECTED, "HIGH"),
                                FindingSeverity.LOW,
                                "rejected"),
                        "https://api.example.test"),
                "REJECTED package cannot become Burp audit issue");
        assertions++;
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> projector.project(
                        new ReproductionPackageProjector().project(
                                candidate(FindingCandidateState.INCONCLUSIVE, "HIGH"),
                                FindingSeverity.LOW,
                                "inconclusive"),
                        "https://api.example.test"),
                "INCONCLUSIVE package cannot become Burp audit issue");
        assertions++;
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> projector.project(pkg, "file:///tmp/acra"),
                "non-http origin fails closed");
        assertions++;
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> projector.project(pkg, "https://user:pass@api.example.test"),
                "origin user-info fails closed");
        assertions++;

        return assertions;
    }

    private static FindingCandidate candidate(FindingCandidateState state, String confidence) {
        return new FindingCandidate(
                "candidate-burp-" + state.name().toLowerCase(),
                state,
                "project-s12",
                List.of("test-1"),
                List.of("exec-1"),
                List.of("obs-1"),
                List.of("assessment-1"),
                List.of("OBJECT", "TENANT"),
                "/api/v1/documents/42",
                "document:42",
                "user-a",
                "CROSS_TENANT",
                AuthorizationDecision.DENY,
                AuthorizationDecision.ALLOW,
                List.of("evidence-burp-1", "evidence-burp-2"),
                List.of(),
                List.of("policy-burp-1"),
                confidence,
                "Expected DENY but observed ALLOW",
                FindingFingerprint.of(
                        "/api/v1/documents/{id}", "document:42", "user-a", "CROSS_TENANT",
                        "DENY_TO_ALLOW", "OBJECT_AUTHORIZATION"));
    }

    private static FindingCandidate secretCandidate() {
        return new FindingCandidate(
                "candidate-burp-secret",
                FindingCandidateState.CANDIDATE,
                "project-secret",
                List.of("test-secret"),
                List.of("exec-secret"),
                List.of("obs-secret"),
                List.of("assessment-secret"),
                List.of("OBJECT"),
                "/api/v1/resource?token=topsecret",
                "resource-secret",
                "user-a",
                "SAME_TENANT",
                AuthorizationDecision.DENY,
                AuthorizationDecision.ALLOW,
                List.of("evidence-token=topsecret"),
                List.of(),
                List.of("policy-token=topsecret"),
                "HIGH",
                "Bearer aaa.bbb.ccc",
                FindingFingerprint.of(
                        "/api/v1/resource", "resource-secret", "user-a", "SAME_TENANT",
                        "DENY_TO_ALLOW", "OBJECT_AUTHORIZATION"));
    }
}
