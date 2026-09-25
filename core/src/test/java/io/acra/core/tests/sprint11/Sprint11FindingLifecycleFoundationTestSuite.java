package io.acra.core.tests.sprint11;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.AuthorizationRiskAssessment;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingConfidence;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.domain.finding.FindingLifecycleService;
import io.acra.core.domain.finding.FindingLifecycleState;
import io.acra.core.domain.finding.FindingSeverity;
import io.acra.core.serialization.DomainSerializer;
import io.acra.core.tests.TestSupport;
import java.time.Instant;
import java.util.List;

public final class Sprint11FindingLifecycleFoundationTestSuite {
    private static final Instant OPENED = Instant.parse("2026-09-25T01:00:00Z");

    private Sprint11FindingLifecycleFoundationTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT11_FINDING_LIFECYCLE_FOUNDATION PASS assertions=" + assertions);
    }

    public static int run() {
        FindingLifecycleService service = new FindingLifecycleService();
        FindingCandidate candidate = candidate("fc-s11", FindingCandidateState.CANDIDATE, List.of("evidence-a"));
        AuthorizationRiskAssessment risk = risk(candidate.candidateId(), FindingSeverity.HIGH, FindingConfidence.MEDIUM);

        var opened = service.open(candidate, risk, OPENED);
        int assertions = 0;

        TestSupport.assertEquals(FindingLifecycleState.NEEDS_REVIEW, opened.state(),
                "candidate enters explicit needs-review state");
        assertions++;
        TestSupport.assertEquals(FindingSeverity.HIGH, opened.severity(),
                "finding preserves independent severity");
        assertions++;
        TestSupport.assertEquals(FindingConfidence.MEDIUM, opened.confidence(),
                "finding preserves independent confidence");
        assertions++;
        TestSupport.assertEquals(candidate.candidateId(), opened.candidateId(),
                "reviewed finding preserves candidate lineage");
        assertions++;
        TestSupport.assertEquals(candidate.projectId(), opened.projectId(),
                "reviewed finding preserves project isolation");
        assertions++;
        TestSupport.assertTrue(opened.findingId().startsWith("finding-"),
                "finding identity uses stable hash prefix");
        assertions++;
        TestSupport.assertEquals(OPENED, opened.openedAt(),
                "opening timestamp is retained");
        assertions++;
        TestSupport.assertEquals(OPENED, opened.updatedAt(),
                "initial updated timestamp equals opening time");
        assertions++;
        TestSupport.assertEquals(0, opened.history().size(),
                "opening does not fabricate a review transition");
        assertions++;
        TestSupport.assertFalse(opened.terminal(),
                "needs-review is not terminal");
        assertions++;

        var repeated = service.open(candidate, risk, OPENED.plusSeconds(60));
        TestSupport.assertEquals(opened.findingId(), repeated.findingId(),
                "finding identity is deterministic and timestamp independent");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> service.open(candidate("fc-rejected", FindingCandidateState.REJECTED, List.of("e1")),
                        risk("fc-rejected", FindingSeverity.LOW, FindingConfidence.HIGH), OPENED),
                "rejected candidate cannot enter finding lifecycle");
        assertions++;
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> service.open(candidate("fc-inconclusive", FindingCandidateState.INCONCLUSIVE, List.of("e1")),
                        risk("fc-inconclusive", FindingSeverity.LOW, FindingConfidence.LOW), OPENED),
                "inconclusive candidate cannot enter finding lifecycle");
        assertions++;
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> service.open(candidate, risk("other-candidate", FindingSeverity.HIGH,
                        FindingConfidence.MEDIUM), OPENED),
                "risk assessment must match candidate identity");
        assertions++;
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> service.open(candidate("fc-no-evidence", FindingCandidateState.CANDIDATE, List.of()),
                        risk("fc-no-evidence", FindingSeverity.MEDIUM, FindingConfidence.LOW), OPENED),
                "candidate without supporting evidence fails closed");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> service.transition(opened, FindingLifecycleState.CONFIRMED, OPENED.plusSeconds(10),
                        "reviewer-a", "attempt to skip validation", List.of("review-evidence-a")),
                "needs-review cannot skip directly to confirmed");
        assertions++;

        var validated = service.transition(
                opened,
                FindingLifecycleState.VALIDATED,
                OPENED.plusSeconds(10),
                "reviewer-a",
                "Evidence and authorization context validated",
                List.of("review-evidence-a"));
        TestSupport.assertEquals(FindingLifecycleState.VALIDATED, validated.state(),
                "explicit review advances to validated");
        assertions++;
        TestSupport.assertEquals(1, validated.history().size(),
                "validated transition is auditable");
        assertions++;
        TestSupport.assertEquals(2, validated.supportingEvidenceIds().size(),
                "review evidence is merged with candidate evidence");
        assertions++;
        TestSupport.assertEquals(FindingSeverity.HIGH, validated.severity(),
                "lifecycle does not rewrite severity");
        assertions++;
        TestSupport.assertEquals(FindingConfidence.MEDIUM, validated.confidence(),
                "lifecycle does not rewrite confidence");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> service.transition(validated, FindingLifecycleState.ACCEPTED_RISK,
                        OPENED.plusSeconds(15), "reviewer-a", "premature acceptance", List.of("e2")),
                "validated finding cannot skip confirmed state");
        assertions++;

        var confirmed = service.transition(
                validated,
                FindingLifecycleState.CONFIRMED,
                OPENED.plusSeconds(20),
                "reviewer-b",
                "Independent review confirms evidence-backed authorization violation",
                List.of("confirmation-evidence"));
        TestSupport.assertEquals(FindingLifecycleState.CONFIRMED, confirmed.state(),
                "validated finding can be explicitly confirmed");
        assertions++;
        TestSupport.assertEquals(2, confirmed.history().size(),
                "confirmation appends audit history");
        assertions++;
        TestSupport.assertFalse(confirmed.terminal(),
                "confirmed remains actionable for accepted-risk decision");
        assertions++;

        var accepted = service.transition(
                confirmed,
                FindingLifecycleState.ACCEPTED_RISK,
                OPENED.plusSeconds(30),
                "risk-owner",
                "Risk acceptance recorded with explicit evidence",
                List.of("risk-acceptance-evidence"));
        TestSupport.assertEquals(FindingLifecycleState.ACCEPTED_RISK, accepted.state(),
                "confirmed finding can enter accepted-risk terminal state");
        assertions++;
        TestSupport.assertTrue(accepted.terminal(),
                "accepted risk is terminal");
        assertions++;
        TestSupport.assertEquals(3, accepted.history().size(),
                "accepted-risk decision remains in audit history");
        assertions++;
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> service.transition(accepted, FindingLifecycleState.FALSE_POSITIVE,
                        OPENED.plusSeconds(40), "reviewer-c", "terminal rewrite", List.of("e3")),
                "terminal lifecycle state cannot be rewritten");
        assertions++;

        var falsePositive = service.transition(
                opened,
                FindingLifecycleState.FALSE_POSITIVE,
                OPENED.plusSeconds(12),
                "reviewer-fp",
                "Candidate disproven by reviewed control evidence",
                List.of("false-positive-evidence"));
        TestSupport.assertEquals(FindingLifecycleState.FALSE_POSITIVE, falsePositive.state(),
                "needs-review candidate can be explicitly rejected as false positive");
        assertions++;
        TestSupport.assertTrue(falsePositive.terminal(),
                "false positive is terminal");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> service.transition(validated, FindingLifecycleState.CONFIRMED,
                        OPENED.minusSeconds(1), "reviewer-a", "time reversal", List.of("e4")),
                "transition cannot move audit time backwards");
        assertions++;
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> service.transition(validated, FindingLifecycleState.CONFIRMED,
                        OPENED.plusSeconds(20), "", "missing reviewer", List.of("e4")),
                "review transition requires reviewer reference");
        assertions++;
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> service.transition(validated, FindingLifecycleState.CONFIRMED,
                        OPENED.plusSeconds(20), "reviewer-a", "", List.of("e4")),
                "review transition requires reason");
        assertions++;
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> service.transition(validated, FindingLifecycleState.CONFIRMED,
                        OPENED.plusSeconds(20), "reviewer-a", "missing evidence", List.of()),
                "review transition requires evidence");
        assertions++;

        var redacted = service.transition(
                opened,
                FindingLifecycleState.VALIDATED,
                OPENED.plusSeconds(50),
                "reviewer-secret",
                "Authorization: Bearer s11-secret-token",
                List.of("review-evidence-secret"));
        String serialized = new DomainSerializer().serialize(redacted);
        TestSupport.assertNotContains(serialized, "s11-secret-token",
                "review audit serialization remains secret-safe");
        assertions++;
        TestSupport.assertContains(serialized, "NEEDS_REVIEW",
                "serialized review history preserves prior state");
        assertions++;
        TestSupport.assertContains(serialized, "VALIDATED",
                "serialized review history preserves target state");
        assertions++;

        TestSupport.assertThrows(UnsupportedOperationException.class,
                () -> opened.supportingEvidenceIds().add("mutate"),
                "reviewed finding evidence list is immutable");
        assertions++;
        TestSupport.assertThrows(UnsupportedOperationException.class,
                () -> validated.history().add(validated.history().get(0)),
                "reviewed finding history is immutable");
        assertions++;

        return assertions;
    }

    private static FindingCandidate candidate(
            String id,
            FindingCandidateState state,
            List<String> evidence) {
        String dimension = "BATCH_AUTHORIZATION";
        return new FindingCandidate(
                id,
                state,
                "project-s11",
                List.of("test-s11"),
                List.of("execution-s11"),
                List.of("observation-s11"),
                List.of("assessment-s11"),
                List.of(dimension),
                "/api/v1/s11/review",
                "resource-b",
                "user-a",
                "CROSS_TENANT",
                AuthorizationDecision.DENY,
                AuthorizationDecision.ALLOW,
                evidence,
                List.of(),
                List.of("policy-s11"),
                "MEDIUM",
                "review-only candidate",
                FindingFingerprint.of(
                        "/api/v1/s11/review",
                        "resource-b",
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
                "explicit Sprint 11 review fixture",
                List.of("authorization-impact"));
    }
}
