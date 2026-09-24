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
import io.acra.core.tests.TestSupport;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public final class Sprint11FindingReviewWorkspaceTestSuite {
    private static final Instant AT = Instant.parse("2026-09-25T01:30:00Z");

    private Sprint11FindingReviewWorkspaceTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT11_FINDING_REVIEW_WORKSPACE PASS assertions=" + assertions);
    }

    public static int run() {
        FindingReviewWorkspace workspace = new FindingReviewWorkspace("project-s11");
        FindingCandidate candidateA = candidate(
                "candidate-a", "resource-a", "BATCH_AUTHORIZATION", FindingCandidateState.CANDIDATE,
                "project-s11");
        FindingCandidate candidateB = candidate(
                "candidate-b", "resource-b", "INDIRECT_REFERENCE_AUTHORIZATION",
                FindingCandidateState.CANDIDATE, "project-s11");
        AuthorizationRiskAssessment riskA = risk(
                candidateA.candidateId(), FindingSeverity.HIGH, FindingConfidence.HIGH);
        AuthorizationRiskAssessment riskB = risk(
                candidateB.candidateId(), FindingSeverity.MEDIUM, FindingConfidence.MEDIUM);

        var findingA = workspace.open(candidateA, riskA, AT);
        var findingB = workspace.open(candidateB, riskB, AT.plusSeconds(1));
        int assertions = 0;

        TestSupport.assertEquals("project-s11", workspace.projectId(),
                "workspace exposes immutable project boundary");
        assertions++;
        TestSupport.assertEquals(FindingLifecycleState.NEEDS_REVIEW, findingA.state(),
                "first candidate opens in needs-review state");
        assertions++;
        TestSupport.assertEquals(FindingLifecycleState.NEEDS_REVIEW, findingB.state(),
                "second candidate opens in needs-review state");
        assertions++;

        var initial = workspace.snapshot();
        TestSupport.assertEquals(2, initial.totalCount(),
                "workspace retains both review cases");
        assertions++;
        TestSupport.assertEquals(2L, initial.count(FindingLifecycleState.NEEDS_REVIEW),
                "snapshot counts needs-review findings");
        assertions++;
        TestSupport.assertEquals(2L, initial.openReviewCount(),
                "both initial findings remain open for review");
        assertions++;
        TestSupport.assertEquals(0L, initial.terminalCount(),
                "no terminal findings are invented");
        assertions++;
        TestSupport.assertTrue(initial.cases().stream()
                        .map(value -> value.finding().findingId())
                        .toList()
                        .equals(initial.cases().stream()
                                .map(value -> value.finding().findingId())
                                .sorted(Comparator.naturalOrder())
                                .toList()),
                "snapshot ordering is deterministic by finding identity");
        assertions++;

        var repeated = workspace.open(candidateA, riskA, AT.plusSeconds(30));
        TestSupport.assertEquals(findingA, repeated,
                "reopening identical source state is idempotent");
        assertions++;
        TestSupport.assertEquals(2, workspace.snapshot().totalCount(),
                "idempotent reopen does not duplicate review case");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> workspace.open(candidate(
                                "foreign-candidate", "resource-x", "BATCH_AUTHORIZATION",
                                FindingCandidateState.CANDIDATE, "project-other"),
                        risk("foreign-candidate", FindingSeverity.LOW, FindingConfidence.LOW), AT),
                "cross-project candidate is rejected");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> workspace.open(candidateA,
                        risk(candidateA.candidateId(), FindingSeverity.CRITICAL, FindingConfidence.HIGH),
                        AT.plusSeconds(31)),
                "same finding identity cannot silently drift risk source state");
        assertions++;

        TestSupport.assertEquals(candidateA, workspace.reviewCase(findingA.findingId()).candidate(),
                "review case preserves original candidate");
        assertions++;
        TestSupport.assertEquals(riskA, workspace.reviewCase(findingA.findingId()).risk(),
                "review case preserves original independent risk assessment");
        assertions++;

        var validatedA = workspace.transition(
                findingA.findingId(),
                FindingLifecycleState.VALIDATED,
                AT.plusSeconds(60),
                "reviewer-a",
                "Candidate evidence validated",
                List.of("review-a"));
        TestSupport.assertEquals(FindingLifecycleState.VALIDATED, validatedA.state(),
                "workspace delegates explicit validation transition");
        assertions++;
        TestSupport.assertEquals(1L, workspace.snapshot().count(FindingLifecycleState.VALIDATED),
                "snapshot reflects validated state");
        assertions++;

        var confirmedA = workspace.transition(
                findingA.findingId(),
                FindingLifecycleState.CONFIRMED,
                AT.plusSeconds(120),
                "reviewer-b",
                "Independent evidence confirms authorization violation",
                List.of("confirm-a"));
        TestSupport.assertEquals(FindingLifecycleState.CONFIRMED, confirmedA.state(),
                "workspace delegates explicit confirmation");
        assertions++;
        TestSupport.assertEquals(FindingSeverity.HIGH, confirmedA.severity(),
                "workspace preserves source severity through review");
        assertions++;
        TestSupport.assertEquals(FindingConfidence.HIGH, confirmedA.confidence(),
                "workspace preserves source confidence through review");
        assertions++;

        var falsePositiveB = workspace.transition(
                findingB.findingId(),
                FindingLifecycleState.FALSE_POSITIVE,
                AT.plusSeconds(90),
                "reviewer-c",
                "Control evidence disproves candidate",
                List.of("fp-b"));
        TestSupport.assertEquals(FindingLifecycleState.FALSE_POSITIVE, falsePositiveB.state(),
                "workspace supports explicit false-positive resolution");
        assertions++;

        var mixed = workspace.snapshot();
        TestSupport.assertEquals(1L, mixed.count(FindingLifecycleState.CONFIRMED),
                "snapshot counts confirmed findings explicitly");
        assertions++;
        TestSupport.assertEquals(1L, mixed.count(FindingLifecycleState.FALSE_POSITIVE),
                "snapshot counts false positives explicitly");
        assertions++;
        TestSupport.assertEquals(1L, mixed.openReviewCount(),
                "confirmed finding remains non-terminal pending disposition");
        assertions++;
        TestSupport.assertEquals(1L, mixed.terminalCount(),
                "false-positive finding is terminal");
        assertions++;

        var acceptedA = workspace.transition(
                findingA.findingId(),
                FindingLifecycleState.ACCEPTED_RISK,
                AT.plusSeconds(180),
                "risk-owner",
                "Explicit risk acceptance",
                List.of("risk-a"));
        TestSupport.assertTrue(acceptedA.terminal(),
                "accepted-risk finding becomes terminal");
        assertions++;
        TestSupport.assertEquals(0L, workspace.snapshot().openReviewCount(),
                "all cases are terminal after explicit dispositions");
        assertions++;
        TestSupport.assertEquals(2L, workspace.snapshot().terminalCount(),
                "terminal count reflects accepted-risk plus false-positive");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> workspace.transition(
                        "missing-finding",
                        FindingLifecycleState.VALIDATED,
                        AT.plusSeconds(200),
                        "reviewer",
                        "unknown id",
                        List.of("e")),
                "unknown finding identity fails closed");
        assertions++;

        TestSupport.assertThrows(UnsupportedOperationException.class,
                () -> initial.cases().add(initial.cases().get(0)),
                "workspace snapshot case list is immutable");
        assertions++;

        return assertions;
    }

    private static FindingCandidate candidate(
            String id,
            String resourceId,
            String dimension,
            FindingCandidateState state,
            String projectId) {
        return new FindingCandidate(
                id,
                state,
                projectId,
                List.of("test-" + id),
                List.of("execution-" + id),
                List.of("observation-" + id),
                List.of("assessment-" + id),
                List.of(dimension),
                "/api/v1/s11/review",
                resourceId,
                "user-a",
                "CROSS_TENANT",
                AuthorizationDecision.DENY,
                AuthorizationDecision.ALLOW,
                List.of("evidence-" + id),
                List.of(),
                List.of("policy-" + id),
                "HIGH",
                "review-only candidate",
                FindingFingerprint.of(
                        "/api/v1/s11/review",
                        resourceId,
                        "user-a",
                        "CROSS_TENANT",
                        dimension,
                        state.name()));
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
                70,
                "explicit Sprint 11 workspace fixture",
                List.of("authorization-impact"));
    }
}
