package io.acra.core.tests.sprint11;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.AuthorizationRiskAssessment;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingConfidence;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.domain.finding.FindingLifecycleState;
import io.acra.core.domain.finding.FindingSeverity;
import io.acra.core.product.finding.FindingReviewWorkspace;
import io.acra.core.reporting.finding.FindingBurpIssueConfidence;
import io.acra.core.reporting.finding.FindingBurpIssueDraftGenerator;
import io.acra.core.reporting.finding.FindingBurpIssueSeverity;
import io.acra.core.reporting.finding.FindingReproductionPackageGenerator;
import io.acra.core.serialization.DomainSerializer;
import io.acra.core.tests.TestSupport;
import java.time.Instant;
import java.util.List;

public final class Sprint11FindingBurpIssueDraftTestSuite {
    private static final Instant AT = Instant.parse("2026-09-25T03:30:00Z");

    private Sprint11FindingBurpIssueDraftTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT11_FINDING_BURP_ISSUE_DRAFT PASS assertions=" + assertions);
    }

    public static int run() {
        FindingBurpIssueDraftGenerator generator = new FindingBurpIssueDraftGenerator();
        int assertions = 0;

        FindingReviewWorkspace reviewWorkspace = workspace(
                "candidate-review", FindingSeverity.HIGH, FindingConfidence.HIGH);
        var reviewFinding = reviewWorkspace.snapshot().cases().get(0).finding();
        var reviewPackage = packageFor(reviewWorkspace, reviewFinding.findingId(), AT.plusSeconds(1));
        var reviewDraft = generator.generate(reviewPackage);

        TestSupport.assertTrue(reviewDraft.draftId().startsWith("burp-issue-draft-"),
                "Burp issue draft has stable hash identity");
        assertions++;
        TestSupport.assertEquals(reviewPackage.reproductionId(), reviewDraft.reproductionId(),
                "draft preserves reproduction identity");
        assertions++;
        TestSupport.assertEquals(reviewPackage.findingId(), reviewDraft.findingId(),
                "draft preserves finding identity");
        assertions++;
        TestSupport.assertEquals(FindingLifecycleState.NEEDS_REVIEW, reviewDraft.lifecycleState(),
                "draft preserves lifecycle state");
        assertions++;
        TestSupport.assertEquals(FindingBurpIssueSeverity.HIGH, reviewDraft.severity(),
                "high ACRA severity maps to high Burp draft severity");
        assertions++;
        TestSupport.assertEquals(FindingBurpIssueConfidence.TENTATIVE, reviewDraft.confidence(),
                "unconfirmed review state is tentative regardless of ACRA confidence");
        assertions++;
        TestSupport.assertFalse(reviewDraft.publicationEligible(),
                "needs-review draft is not publication eligible");
        assertions++;
        TestSupport.assertContains(reviewDraft.detail(), "Expected authorization decision: DENY",
                "draft detail preserves expected decision");
        assertions++;
        TestSupport.assertContains(reviewDraft.detail(), "Observed authorization decision: ALLOW",
                "draft detail preserves observed decision");
        assertions++;
        TestSupport.assertContains(reviewDraft.detail(), reviewPackage.findingFingerprint(),
                "draft detail preserves finding fingerprint");
        assertions++;
        TestSupport.assertContains(reviewDraft.detail(), reviewPackage.reproductionId(),
                "draft detail preserves reproduction identity");
        assertions++;
        TestSupport.assertTrue(reviewDraft.limitations().stream()
                        .anyMatch(value -> value.contains("does not publish")),
                "draft explicitly states non-publication boundary");
        assertions++;
        TestSupport.assertTrue(reviewDraft.limitations().stream()
                        .anyMatch(value -> value.contains("base URL")),
                "draft records base-URL materialization requirement");
        assertions++;

        reviewWorkspace.transition(
                reviewFinding.findingId(),
                FindingLifecycleState.VALIDATED,
                AT.plusSeconds(10),
                "reviewer-a",
                "Authorization: Bearer s11-burp-secret",
                List.of("burp-review-evidence"));
        var validatedDraft = generator.generate(
                packageFor(reviewWorkspace, reviewFinding.findingId(), AT.plusSeconds(11)));
        TestSupport.assertFalse(validatedDraft.publicationEligible(),
                "validated review remains non-publication eligible");
        assertions++;
        TestSupport.assertEquals(FindingBurpIssueConfidence.TENTATIVE, validatedDraft.confidence(),
                "validated-but-unconfirmed review remains tentative");
        assertions++;

        reviewWorkspace.transition(
                reviewFinding.findingId(),
                FindingLifecycleState.CONFIRMED,
                AT.plusSeconds(20),
                "reviewer-b",
                "Independent confirmation",
                List.of("burp-confirm-evidence"));
        var confirmedDraft = generator.generate(
                packageFor(reviewWorkspace, reviewFinding.findingId(), AT.plusSeconds(21)));
        TestSupport.assertTrue(confirmedDraft.publicationEligible(),
                "explicit confirmed finding becomes publication eligible");
        assertions++;
        TestSupport.assertEquals(FindingBurpIssueConfidence.CERTAIN, confirmedDraft.confidence(),
                "confirmed high-confidence ACRA finding maps to CERTAIN");
        assertions++;
        TestSupport.assertEquals(FindingBurpIssueSeverity.HIGH, confirmedDraft.severity(),
                "confirmed high severity remains high");
        assertions++;

        var confirmedDraftAgain = generator.generate(
                packageFor(reviewWorkspace, reviewFinding.findingId(), AT.plusSeconds(99)));
        TestSupport.assertEquals(confirmedDraft.draftId(), confirmedDraftAgain.draftId(),
                "draft identity is independent of render timestamp");
        assertions++;

        reviewWorkspace.transition(
                reviewFinding.findingId(),
                FindingLifecycleState.ACCEPTED_RISK,
                AT.plusSeconds(30),
                "risk-owner",
                "Explicit risk acceptance",
                List.of("burp-risk-evidence"));
        var acceptedDraft = generator.generate(
                packageFor(reviewWorkspace, reviewFinding.findingId(), AT.plusSeconds(31)));
        TestSupport.assertTrue(acceptedDraft.publicationEligible(),
                "accepted-risk state retains confirmed publication eligibility");
        assertions++;
        TestSupport.assertEquals(FindingBurpIssueConfidence.CERTAIN, acceptedDraft.confidence(),
                "accepted risk preserves confirmed confidence mapping");
        assertions++;

        FindingReviewWorkspace mediumWorkspace = workspace(
                "candidate-medium", FindingSeverity.MEDIUM, FindingConfidence.MEDIUM);
        var mediumFinding = mediumWorkspace.snapshot().cases().get(0).finding();
        confirm(mediumWorkspace, mediumFinding.findingId(), AT.plusSeconds(40));
        var mediumDraft = generator.generate(
                packageFor(mediumWorkspace, mediumFinding.findingId(), AT.plusSeconds(42)));
        TestSupport.assertEquals(FindingBurpIssueSeverity.MEDIUM, mediumDraft.severity(),
                "medium ACRA severity maps to Burp medium");
        assertions++;
        TestSupport.assertEquals(FindingBurpIssueConfidence.FIRM, mediumDraft.confidence(),
                "confirmed medium ACRA confidence maps to FIRM");
        assertions++;

        FindingReviewWorkspace lowWorkspace = workspace(
                "candidate-low", FindingSeverity.LOW, FindingConfidence.LOW);
        var lowFinding = lowWorkspace.snapshot().cases().get(0).finding();
        confirm(lowWorkspace, lowFinding.findingId(), AT.plusSeconds(50));
        var lowDraft = generator.generate(
                packageFor(lowWorkspace, lowFinding.findingId(), AT.plusSeconds(52)));
        TestSupport.assertEquals(FindingBurpIssueSeverity.LOW, lowDraft.severity(),
                "low ACRA severity maps to Burp low");
        assertions++;
        TestSupport.assertEquals(FindingBurpIssueConfidence.TENTATIVE, lowDraft.confidence(),
                "confirmed low ACRA confidence remains TENTATIVE");
        assertions++;

        FindingReviewWorkspace infoWorkspace = workspace(
                "candidate-info", FindingSeverity.INFO, FindingConfidence.INSUFFICIENT);
        var infoFinding = infoWorkspace.snapshot().cases().get(0).finding();
        confirm(infoWorkspace, infoFinding.findingId(), AT.plusSeconds(60));
        var infoDraft = generator.generate(
                packageFor(infoWorkspace, infoFinding.findingId(), AT.plusSeconds(62)));
        TestSupport.assertEquals(FindingBurpIssueSeverity.INFORMATION, infoDraft.severity(),
                "informational ACRA severity maps to Burp information");
        assertions++;
        TestSupport.assertEquals(FindingBurpIssueConfidence.TENTATIVE, infoDraft.confidence(),
                "insufficient ACRA confidence maps to Burp tentative");
        assertions++;

        FindingReviewWorkspace fpWorkspace = workspace(
                "candidate-fp", FindingSeverity.HIGH, FindingConfidence.HIGH);
        var fpFinding = fpWorkspace.snapshot().cases().get(0).finding();
        fpWorkspace.transition(
                fpFinding.findingId(),
                FindingLifecycleState.FALSE_POSITIVE,
                AT.plusSeconds(70),
                "reviewer-fp",
                "Control evidence disproves candidate",
                List.of("burp-fp-evidence"));
        var fpDraft = generator.generate(
                packageFor(fpWorkspace, fpFinding.findingId(), AT.plusSeconds(71)));
        TestSupport.assertEquals(FindingBurpIssueSeverity.FALSE_POSITIVE, fpDraft.severity(),
                "false-positive lifecycle maps to Burp false-positive severity");
        assertions++;
        TestSupport.assertEquals(FindingBurpIssueSeverity.INFORMATION, fpDraft.typicalSeverity(),
                "false-positive draft does not advertise false-positive as typical issue severity");
        assertions++;
        TestSupport.assertEquals(FindingBurpIssueConfidence.TENTATIVE, fpDraft.confidence(),
                "false positive remains tentative");
        assertions++;
        TestSupport.assertFalse(fpDraft.publicationEligible(),
                "false-positive draft is not publication eligible");
        assertions++;

        String serialized = new DomainSerializer().serialize(validatedDraft);
        TestSupport.assertNotContains(serialized, "DummyPassword",
                "Burp issue draft excludes candidate source rationale secret");
        assertions++;
        TestSupport.assertNotContains(serialized, "s11-burp-secret",
                "Burp issue draft excludes review reason secret");
        assertions++;
        TestSupport.assertNotContains(serialized, "reviewer-a",
                "Burp issue draft excludes reviewer identity");
        assertions++;
        TestSupport.assertNotContains(serialized, "\"rationale\"",
                "Burp issue draft schema contains no rationale field");
        assertions++;
        TestSupport.assertNotContains(serialized, "\"reviewerReference\"",
                "Burp issue draft schema contains no reviewerReference field");
        assertions++;

        TestSupport.assertThrows(
                UnsupportedOperationException.class,
                () -> confirmedDraft.evidenceIds().add("mutate"),
                "Burp issue draft evidence list is immutable");
        assertions++;
        TestSupport.assertThrows(
                UnsupportedOperationException.class,
                () -> confirmedDraft.limitations().add("mutate"),
                "Burp issue draft limitations are immutable");
        assertions++;
        TestSupport.assertThrows(
                IllegalArgumentException.class,
                () -> generator.generate(null),
                "Burp issue draft generator rejects null reproduction package");
        assertions++;

        return assertions;
    }

    private static FindingReviewWorkspace workspace(
            String candidateId,
            FindingSeverity severity,
            FindingConfidence confidence) {
        FindingReviewWorkspace workspace = new FindingReviewWorkspace("project-s11");
        FindingCandidate candidate = candidate(candidateId);
        workspace.open(candidate, risk(candidateId, severity, confidence), AT);
        return workspace;
    }

    private static void confirm(
            FindingReviewWorkspace workspace,
            String findingId,
            Instant at) {
        workspace.transition(
                findingId,
                FindingLifecycleState.VALIDATED,
                at,
                "reviewer-1",
                "validated",
                List.of("evidence-validate-" + findingId));
        workspace.transition(
                findingId,
                FindingLifecycleState.CONFIRMED,
                at.plusSeconds(1),
                "reviewer-2",
                "confirmed",
                List.of("evidence-confirm-" + findingId));
    }

    private static io.acra.core.reporting.finding.FindingReproductionPackage packageFor(
            FindingReviewWorkspace workspace,
            String findingId,
            Instant generatedAt) {
        return new FindingReproductionPackageGenerator().generate(
                workspace.reviewCase(findingId), generatedAt);
    }

    private static FindingCandidate candidate(String id) {
        return new FindingCandidate(
                id,
                FindingCandidateState.CANDIDATE,
                "project-s11",
                List.of("test-" + id),
                List.of("execution-" + id),
                List.of("observation-" + id),
                List.of("assessment-" + id),
                List.of("BATCH_AUTHORIZATION"),
                "/api/v1/s11/review",
                "resource-b",
                "user-a",
                "CROSS_TENANT",
                AuthorizationDecision.DENY,
                AuthorizationDecision.ALLOW,
                List.of("candidate-" + id + "-evidence"),
                List.of(),
                List.of("policy-" + id),
                "HIGH",
                "password=DummyPassword; excluded source rationale",
                FindingFingerprint.of(
                        "/api/v1/s11/review",
                        "resource-b",
                        "user-a",
                        "CROSS_TENANT",
                        "BATCH_AUTHORIZATION",
                        "CANDIDATE"));
    }

    private static AuthorizationRiskAssessment risk(
            String candidateId,
            FindingSeverity severity,
            FindingConfidence confidence) {
        return new AuthorizationRiskAssessment(
                "risk-" + candidateId,
                candidateId,
                severity,
                confidence,
                severity == FindingSeverity.HIGH ? 80 : 60,
                "risk rationale excluded from reproduction",
                List.of("authorization-impact"));
    }
}
