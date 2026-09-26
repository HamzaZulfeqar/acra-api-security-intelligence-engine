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
import io.acra.core.reporting.finding.FindingReproductionPackageGenerator;
import io.acra.core.serialization.DomainSerializer;
import io.acra.core.tests.TestSupport;
import java.time.Instant;
import java.util.List;

public final class Sprint11FindingReproductionPackageTestSuite {
    private static final Instant AT = Instant.parse("2026-09-25T02:00:00Z");

    private Sprint11FindingReproductionPackageTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT11_FINDING_REPRODUCTION_PACKAGE PASS assertions=" + assertions);
    }

    public static int run() {
        FindingReviewWorkspace workspace = new FindingReviewWorkspace("project-s11");
        FindingCandidate candidate = candidate("candidate-repro");
        AuthorizationRiskAssessment risk = risk(candidate.candidateId());
        var opened = workspace.open(candidate, risk, AT);
        FindingReproductionPackageGenerator generator = new FindingReproductionPackageGenerator();

        var initial = generator.generate(workspace.reviewCase(opened.findingId()), AT.plusSeconds(1));
        int assertions = 0;

        TestSupport.assertEquals("s11-finding-reproduction-v1", initial.schemaVersion(),
                "reproduction package declares stable schema version");
        assertions++;
        TestSupport.assertTrue(initial.reproductionId().startsWith("reproduction-"),
                "reproduction package has deterministic hash identity");
        assertions++;
        TestSupport.assertEquals(FindingLifecycleState.NEEDS_REVIEW, initial.state(),
                "initial package preserves needs-review state");
        assertions++;
        TestSupport.assertFalse(initial.confirmedFinding(),
                "needs-review package cannot claim confirmed finding");
        assertions++;
        TestSupport.assertEquals(FindingSeverity.HIGH, initial.severity(),
                "package preserves independent severity");
        assertions++;
        TestSupport.assertEquals(FindingConfidence.MEDIUM, initial.confidence(),
                "package preserves independent confidence");
        assertions++;
        TestSupport.assertEquals(candidate.fingerprint().fingerprint(), initial.findingFingerprint(),
                "package preserves canonical finding fingerprint");
        assertions++;
        TestSupport.assertEquals(candidate.endpoint(), initial.endpoint(),
                "package preserves target endpoint");
        assertions++;
        TestSupport.assertEquals(candidate.resourceId(), initial.resourceId(),
                "package preserves target resource identity");
        assertions++;
        TestSupport.assertEquals(candidate.expectedDecision(), initial.expectedDecision(),
                "package preserves expected authorization decision");
        assertions++;
        TestSupport.assertEquals(candidate.observedDecision(), initial.observedDecision(),
                "package preserves observed authorization decision");
        assertions++;
        TestSupport.assertEquals(1, initial.evidenceIds().size(),
                "initial package retains candidate evidence references");
        assertions++;
        TestSupport.assertEquals(0, initial.reviewTrail().size(),
                "initial package does not fabricate review history");
        assertions++;
        TestSupport.assertTrue(initial.limitations().stream()
                        .anyMatch(value -> value.contains("No raw credentials")),
                "package declares credential minimization boundary");
        assertions++;
        TestSupport.assertTrue(initial.limitations().stream()
                        .anyMatch(value -> value.contains("not scanner auto-confirmation")),
                "package declares human-review confirmation boundary");
        assertions++;

        var sameStateLater = generator.generate(
                workspace.reviewCase(opened.findingId()), AT.plusSeconds(120));
        TestSupport.assertEquals(initial.reproductionId(), sameStateLater.reproductionId(),
                "reproduction identity is independent of render timestamp");
        assertions++;

        workspace.transition(
                opened.findingId(),
                FindingLifecycleState.VALIDATED,
                AT.plusSeconds(10),
                "reviewer-a",
                "Authorization: Bearer s11-review-secret",
                List.of("review-evidence"));
        var validated = generator.generate(
                workspace.reviewCase(opened.findingId()), AT.plusSeconds(20));
        TestSupport.assertEquals(FindingLifecycleState.VALIDATED, validated.state(),
                "package reflects validated lifecycle state");
        assertions++;
        TestSupport.assertFalse(validated.confirmedFinding(),
                "validated package still cannot claim confirmation");
        assertions++;
        TestSupport.assertEquals(1, validated.reviewTrail().size(),
                "validated package carries minimized review trail");
        assertions++;
        TestSupport.assertEquals(FindingLifecycleState.NEEDS_REVIEW,
                validated.reviewTrail().get(0).fromState(),
                "review trail records prior state");
        assertions++;
        TestSupport.assertEquals(FindingLifecycleState.VALIDATED,
                validated.reviewTrail().get(0).toState(),
                "review trail records target state");
        assertions++;
        TestSupport.assertEquals(2, validated.evidenceIds().size(),
                "review evidence is included without duplicating source evidence");
        assertions++;
        TestSupport.assertFalse(initial.reproductionId().equals(validated.reproductionId()),
                "state/evidence change produces new reproduction identity");
        assertions++;

        workspace.transition(
                opened.findingId(),
                FindingLifecycleState.CONFIRMED,
                AT.plusSeconds(30),
                "reviewer-b",
                "Independent review confirmed",
                List.of("confirmation-evidence"));
        var confirmed = generator.generate(
                workspace.reviewCase(opened.findingId()), AT.plusSeconds(40));
        TestSupport.assertEquals(FindingLifecycleState.CONFIRMED, confirmed.state(),
                "package reflects explicit confirmed state");
        assertions++;
        TestSupport.assertTrue(confirmed.confirmedFinding(),
                "confirmedFinding becomes true only after explicit confirmation");
        assertions++;
        TestSupport.assertEquals(2, confirmed.reviewTrail().size(),
                "confirmed package preserves complete minimized state trail");
        assertions++;

        workspace.transition(
                opened.findingId(),
                FindingLifecycleState.ACCEPTED_RISK,
                AT.plusSeconds(50),
                "risk-owner",
                "Explicit risk acceptance",
                List.of("risk-evidence"));
        var accepted = generator.generate(
                workspace.reviewCase(opened.findingId()), AT.plusSeconds(60));
        TestSupport.assertTrue(accepted.confirmedFinding(),
                "accepted-risk disposition preserves prior confirmation semantics");
        assertions++;
        TestSupport.assertEquals(FindingLifecycleState.ACCEPTED_RISK, accepted.state(),
                "package preserves accepted-risk terminal disposition");
        assertions++;

        FindingReviewWorkspace fpWorkspace = new FindingReviewWorkspace("project-s11");
        var fpOpened = fpWorkspace.open(candidate("candidate-fp"), risk("candidate-fp"), AT);
        fpWorkspace.transition(
                fpOpened.findingId(),
                FindingLifecycleState.FALSE_POSITIVE,
                AT.plusSeconds(5),
                "reviewer-fp",
                "Reviewed controls disprove candidate",
                List.of("fp-evidence"));
        var falsePositive = generator.generate(
                fpWorkspace.reviewCase(fpOpened.findingId()), AT.plusSeconds(6));
        TestSupport.assertFalse(falsePositive.confirmedFinding(),
                "false-positive reproduction package never claims confirmation");
        assertions++;

        String serialized = new DomainSerializer().serialize(validated);
        TestSupport.assertNotContains(serialized, "DummyPassword",
                "candidate rationale secret is structurally excluded");
        assertions++;
        TestSupport.assertNotContains(serialized, "s11-review-secret",
                "review transition reason secret is structurally excluded");
        assertions++;
        TestSupport.assertNotContains(serialized, "reviewer-a",
                "reviewer reference is excluded from reproduction package");
        assertions++;
        TestSupport.assertNotContains(serialized, "\"rationale\"",
                "reproduction schema contains no rationale field");
        assertions++;
        TestSupport.assertNotContains(serialized, "\"reviewerReference\"",
                "reproduction schema contains no reviewerReference field");
        assertions++;
        TestSupport.assertContains(serialized, "\"confirmedFinding\":false",
                "serialized validated package preserves non-confirmed state");
        assertions++;

        TestSupport.assertThrows(UnsupportedOperationException.class,
                () -> validated.evidenceIds().add("mutate"),
                "reproduction evidence list is immutable");
        assertions++;
        TestSupport.assertThrows(UnsupportedOperationException.class,
                () -> validated.reviewTrail().add(validated.reviewTrail().get(0)),
                "reproduction review trail is immutable");
        assertions++;
        TestSupport.assertThrows(UnsupportedOperationException.class,
                () -> validated.limitations().add("mutate"),
                "reproduction limitations are immutable");
        assertions++;

        return assertions;
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
                List.of("candidate-evidence"),
                List.of(),
                List.of("policy-s11"),
                "MEDIUM",
                "password=DummyPassword; review-only source rationale",
                FindingFingerprint.of(
                        "/api/v1/s11/review",
                        "resource-b",
                        "user-a",
                        "CROSS_TENANT",
                        "BATCH_AUTHORIZATION",
                        "CANDIDATE"));
    }

    private static AuthorizationRiskAssessment risk(String candidateId) {
        return new AuthorizationRiskAssessment(
                "risk-" + candidateId,
                candidateId,
                FindingSeverity.HIGH,
                FindingConfidence.MEDIUM,
                75,
                "risk rationale is not part of reproduction projection",
                List.of("authorization-impact"));
    }
}
