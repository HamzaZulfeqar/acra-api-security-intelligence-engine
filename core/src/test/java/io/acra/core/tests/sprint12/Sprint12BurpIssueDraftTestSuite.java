package io.acra.core.tests.sprint12;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.reporting.reproduction.BurpIssueDraftConfidence;
import io.acra.core.reporting.reproduction.BurpIssueDraftFactory;
import io.acra.core.reporting.reproduction.BurpIssueDraftSeverity;
import io.acra.core.reporting.reproduction.BurpIssueSubmissionState;
import io.acra.core.reporting.reproduction.ReproductionPackageFactory;
import io.acra.core.tests.TestSupport;
import java.util.List;

public final class Sprint12BurpIssueDraftTestSuite {
    private Sprint12BurpIssueDraftTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT12_BURP_ISSUE_DRAFT PASS assertions=" + assertions);
    }

    public static int run() {
        var reproduction = new ReproductionPackageFactory().from(candidate());
        BurpIssueDraftFactory factory = new BurpIssueDraftFactory();
        var draft = factory.from(reproduction);
        int assertions = 0;

        TestSupport.assertEquals("acra-burp-issue-draft-v1", draft.draftVersion(),
                "Burp issue draft version is explicit");
        assertions++;
        TestSupport.assertEquals(reproduction.packageId(), draft.reproductionPackageId(),
                "Burp issue draft retains reproduction package identity");
        assertions++;
        TestSupport.assertEquals(reproduction.candidateId(), draft.candidateId(),
                "Burp issue draft retains candidate identity");
        assertions++;
        TestSupport.assertEquals(BurpIssueDraftSeverity.INFORMATION, draft.severity(),
                "review-only draft uses information severity");
        assertions++;
        TestSupport.assertEquals(BurpIssueDraftConfidence.TENTATIVE, draft.confidence(),
                "review-only draft uses tentative confidence");
        assertions++;
        TestSupport.assertEquals(BurpIssueSubmissionState.NOT_SUBMITTED, draft.submissionState(),
                "draft is explicitly not submitted");
        assertions++;
        TestSupport.assertTrue(!draft.confirmed(),
                "Burp issue draft cannot claim confirmation");
        assertions++;
        TestSupport.assertEquals(reproduction.endpoint(), draft.path(),
                "draft retains endpoint path for later runtime URL resolution");
        assertions++;
        TestSupport.assertEquals(reproduction.evidenceIds(), draft.evidenceIds(),
                "draft retains minimized evidence references");
        assertions++;

        TestSupport.assertContains(draft.detail(), "Expected decision: DENY",
                "draft detail retains expected decision");
        assertions++;
        TestSupport.assertContains(draft.detail(), "Observed decision: ALLOW",
                "draft detail retains observed decision");
        assertions++;
        TestSupport.assertContains(draft.background(), "not a confirmed vulnerability",
                "draft background preserves non-confirmation boundary");
        assertions++;
        TestSupport.assertContains(draft.background(), "not been submitted to Burp",
                "draft background preserves non-submission boundary");
        assertions++;

        TestSupport.assertNotContains(draft.detail(), "DummyPassword",
                "draft detail excludes candidate rationale secret");
        assertions++;
        TestSupport.assertNotContains(draft.detail(), "user-a",
                "draft detail excludes raw principal");
        assertions++;
        TestSupport.assertNotContains(draft.detail(), "tenant-a",
                "draft detail excludes raw tenant");
        assertions++;

        var repeated = factory.from(reproduction);
        TestSupport.assertEquals(draft.draftId(), repeated.draftId(),
                "Burp issue draft identity is deterministic");
        assertions++;

        var escapedReproduction = new io.acra.core.reporting.reproduction.ReproductionPackage(
                "",
                "acra-reproduction-v1",
                "candidate-html",
                FindingCandidateState.CANDIDATE,
                "/api/<script>alert(1)</script>",
                "resource<&>",
                AuthorizationDecision.DENY,
                AuthorizationDecision.ALLOW,
                List.of("AUTHORIZATION"),
                List.of("evidence-html"),
                List.of("policy-html"),
                "ff-html",
                List.of("review only"));
        var escaped = factory.from(escapedReproduction);
        TestSupport.assertNotContains(escaped.detail(), "<script>",
                "dynamic Burp detail values are HTML encoded");
        assertions++;
        TestSupport.assertContains(escaped.detail(), "&lt;script&gt;",
                "HTML encoding preserves readable escaped endpoint");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> factory.from(null),
                "null reproduction package is rejected");
        assertions++;

        return assertions;
    }

    private static FindingCandidate candidate() {
        return new FindingCandidate(
                "s12-burp-candidate",
                FindingCandidateState.CANDIDATE,
                "s12-project",
                List.of("s12-test"),
                List.of("s12-execution"),
                List.of("s12-observation"),
                List.of("s12-assessment"),
                List.of("BATCH_AUTHORIZATION"),
                "/api/v1/documents/resource-b",
                "resource-b",
                "user-a",
                "tenant-a",
                AuthorizationDecision.DENY,
                AuthorizationDecision.ALLOW,
                List.of("evidence-s12-1"),
                List.of(),
                List.of("policy-s12"),
                "HIGH",
                "password=DummyPassword",
                FindingFingerprint.of(
                        "/api/v1/documents/resource-b",
                        "resource-b",
                        "user-a",
                        "tenant-a",
                        "BATCH_AUTHORIZATION",
                        "CANDIDATE"));
    }
}
