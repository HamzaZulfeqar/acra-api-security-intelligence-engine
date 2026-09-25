package io.acra.core.tests.sprint13;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.AuthorizationImpactProfile;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingConfidence;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.domain.finding.FindingLifecycleAction;
import io.acra.core.domain.finding.FindingLifecycleState;
import io.acra.core.domain.finding.FindingLifecycleTransitionRequest;
import io.acra.core.domain.finding.FindingSeverity;
import io.acra.core.domain.finding.GovernedFinding;
import io.acra.core.engine.AuthorizationSeverityEvaluator;
import io.acra.core.engine.FindingLifecycleService;
import io.acra.core.tests.TestSupport;
import java.lang.reflect.Method;
import java.util.List;

public final class Sprint13FindingLifecycleFoundationTestSuite {
    private Sprint13FindingLifecycleFoundationTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT13_FINDING_LIFECYCLE_FOUNDATION PASS assertions=" + assertions);
    }

    public static int run() {
        FindingLifecycleService service = new FindingLifecycleService();
        FindingCandidate candidate = candidate(
                "s13-candidate",
                FindingCandidateState.CANDIDATE,
                "HIGH");
        var risk = new AuthorizationSeverityEvaluator().evaluate(
                candidate,
                new AuthorizationImpactProfile(
                        true, true, true, true, true, true,
                        List.of("controlled high-impact review fixture")));

        int assertions = 0;

        TestSupport.assertEquals(FindingSeverity.CRITICAL, risk.severity(),
                "fixture produces critical internal prioritization");
        assertions++;

        GovernedFinding finding = service.create(candidate, risk);
        TestSupport.assertEquals(FindingLifecycleState.REVIEW_REQUIRED, finding.state(),
                "candidate enters lifecycle at REVIEW_REQUIRED");
        assertions++;
        TestSupport.assertTrue(!finding.confirmed(),
                "even critical risk cannot auto-confirm a finding");
        assertions++;
        TestSupport.assertEquals(FindingSeverity.CRITICAL, finding.severity(),
                "finding preserves severity independently from lifecycle state");
        assertions++;
        TestSupport.assertEquals(FindingConfidence.HIGH, finding.confidence(),
                "finding preserves confidence independently from lifecycle state");
        assertions++;
        TestSupport.assertEquals(0, finding.events().size(),
                "initial governed finding has no fabricated human-review event");
        assertions++;
        TestSupport.assertEquals(candidate.supportingEvidenceIds(), finding.evidenceIds(),
                "initial governed finding preserves supporting evidence");
        assertions++;

        GovernedFinding repeated = service.create(candidate, risk);
        TestSupport.assertEquals(finding.findingId(), repeated.findingId(),
                "governed finding identity is deterministic");
        assertions++;
        TestSupport.assertEquals(finding.fingerprint(), repeated.fingerprint(),
                "governed finding fingerprint is deterministic");
        assertions++;

        GovernedFinding confirmed = service.transition(
                finding,
                request(
                        FindingLifecycleAction.CONFIRM,
                        "reviewer-001",
                        "review-decision-001",
                        "review-evidence-001"));
        TestSupport.assertEquals(FindingLifecycleState.CONFIRMED, confirmed.state(),
                "explicit human review can confirm a candidate");
        assertions++;
        TestSupport.assertTrue(confirmed.confirmed(),
                "confirmed lifecycle state reports confirmed");
        assertions++;
        TestSupport.assertEquals(1, confirmed.events().size(),
                "confirmation appends one lifecycle event");
        assertions++;
        TestSupport.assertEquals(FindingLifecycleState.REVIEW_REQUIRED,
                confirmed.events().getFirst().fromState(),
                "confirmation event preserves source state");
        assertions++;
        TestSupport.assertEquals(FindingLifecycleState.CONFIRMED,
                confirmed.events().getFirst().toState(),
                "confirmation event preserves target state");
        assertions++;
        TestSupport.assertEquals(FindingSeverity.CRITICAL, confirmed.severity(),
                "confirmation does not rewrite severity");
        assertions++;
        TestSupport.assertEquals(FindingConfidence.HIGH, confirmed.confidence(),
                "confirmation does not rewrite confidence");
        assertions++;

        GovernedFinding remediation = service.transition(
                confirmed,
                request(
                        FindingLifecycleAction.START_REMEDIATION,
                        "reviewer-002",
                        "remediation-start-001",
                        "remediation-evidence-001"));
        TestSupport.assertEquals(
                FindingLifecycleState.REMEDIATION_IN_PROGRESS,
                remediation.state(),
                "confirmed finding can enter remediation");
        assertions++;

        GovernedFinding retest = service.transition(
                remediation,
                request(
                        FindingLifecycleAction.REQUEST_RETEST,
                        "reviewer-003",
                        "retest-request-001",
                        "retest-evidence-001"));
        TestSupport.assertEquals(FindingLifecycleState.RETEST_REQUIRED, retest.state(),
                "remediation can request retest");
        assertions++;

        GovernedFinding resolved = service.transition(
                retest,
                request(
                        FindingLifecycleAction.PASS_RETEST,
                        "reviewer-004",
                        "retest-pass-001",
                        "retest-pass-evidence-001"));
        TestSupport.assertEquals(FindingLifecycleState.RESOLVED, resolved.state(),
                "passing retest resolves confirmed finding");
        assertions++;

        GovernedFinding closed = service.transition(
                resolved,
                request(
                        FindingLifecycleAction.CLOSE,
                        "reviewer-005",
                        "close-001",
                        "close-evidence-001"));
        TestSupport.assertEquals(FindingLifecycleState.CLOSED, closed.state(),
                "resolved finding can close");
        assertions++;
        TestSupport.assertTrue(closed.confirmed(),
                "closed finding retains confirmed history");
        assertions++;
        TestSupport.assertEquals(5, closed.events().size(),
                "lifecycle history remains append-only");
        assertions++;

        GovernedFinding falsePositive = service.transition(
                finding,
                request(
                        FindingLifecycleAction.MARK_FALSE_POSITIVE,
                        "reviewer-006",
                        "false-positive-001",
                        "false-positive-evidence-001"));
        TestSupport.assertEquals(FindingLifecycleState.FALSE_POSITIVE, falsePositive.state(),
                "human review can mark candidate false positive");
        assertions++;
        TestSupport.assertTrue(!falsePositive.confirmed(),
                "false-positive path is never confirmed");
        assertions++;
        GovernedFinding falsePositiveClosed = service.transition(
                falsePositive,
                request(
                        FindingLifecycleAction.CLOSE,
                        "reviewer-007",
                        "false-positive-close-001",
                        "false-positive-close-evidence-001"));
        TestSupport.assertEquals(FindingLifecycleState.CLOSED, falsePositiveClosed.state(),
                "false-positive finding can close");
        assertions++;
        TestSupport.assertTrue(!falsePositiveClosed.confirmed(),
                "closed false-positive history stays unconfirmed");
        assertions++;

        GovernedFinding acceptedRisk = service.transition(
                confirmed,
                request(
                        FindingLifecycleAction.ACCEPT_RISK,
                        "reviewer-008",
                        "risk-acceptance-001",
                        "risk-acceptance-evidence-001"));
        TestSupport.assertEquals(FindingLifecycleState.ACCEPTED_RISK, acceptedRisk.state(),
                "confirmed finding can enter accepted-risk state");
        assertions++;
        GovernedFinding acceptedClosed = service.transition(
                acceptedRisk,
                request(
                        FindingLifecycleAction.CLOSE,
                        "reviewer-009",
                        "risk-close-001",
                        "risk-close-evidence-001"));
        TestSupport.assertEquals(FindingLifecycleState.CLOSED, acceptedClosed.state(),
                "accepted-risk finding can close");
        assertions++;
        TestSupport.assertTrue(acceptedClosed.confirmed(),
                "closed accepted-risk history retains confirmation");
        assertions++;

        GovernedFinding failedRetest = service.transition(
                retest,
                request(
                        FindingLifecycleAction.FAIL_RETEST,
                        "reviewer-010",
                        "retest-fail-001",
                        "retest-fail-evidence-001"));
        TestSupport.assertEquals(FindingLifecycleState.CONFIRMED, failedRetest.state(),
                "failed retest returns finding to confirmed state");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> service.transition(
                        finding,
                        request(
                                FindingLifecycleAction.ACCEPT_RISK,
                                "reviewer-invalid",
                                "invalid-transition",
                                "invalid-evidence")),
                "review-required candidate cannot accept risk before confirmation");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> service.transition(
                        confirmed,
                        request(
                                FindingLifecycleAction.CLOSE,
                                "reviewer-invalid",
                                "invalid-close",
                                "invalid-close-evidence")),
                "confirmed finding cannot close without a governed disposition");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> service.transition(
                        closed,
                        request(
                                FindingLifecycleAction.CONFIRM,
                                "reviewer-invalid",
                                "reopen-attempt",
                                "reopen-evidence")),
                "closed finding cannot silently reopen");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> service.create(
                        candidate("s13-rejected", FindingCandidateState.REJECTED, "HIGH"),
                        new AuthorizationSeverityEvaluator().evaluate(
                                candidate("s13-rejected", FindingCandidateState.REJECTED, "HIGH"),
                                AuthorizationImpactProfile.none())),
                "rejected candidate cannot enter finding lifecycle");
        assertions++;

        FindingCandidate other = candidate("s13-other", FindingCandidateState.CANDIDATE, "HIGH");
        var otherRisk = new AuthorizationSeverityEvaluator().evaluate(
                other, AuthorizationImpactProfile.none());
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> service.create(candidate, otherRisk),
                "risk assessment for another candidate fails closed");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> new FindingLifecycleTransitionRequest(
                        FindingLifecycleAction.CONFIRM,
                        "Authorization: Bearer abcdefghijklmnopqrstuvwxyz",
                        "decision-safe",
                        List.of("evidence-safe")),
                "secret-bearing reviewer reference fails closed");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> new FindingLifecycleTransitionRequest(
                        FindingLifecycleAction.CONFIRM,
                        "reviewer-safe",
                        "token=supersecret",
                        List.of("evidence-safe")),
                "secret-bearing decision reference fails closed");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> new FindingLifecycleTransitionRequest(
                        FindingLifecycleAction.CONFIRM,
                        "reviewer-safe",
                        "decision-safe",
                        List.of()),
                "human transition without evidence fails closed");
        assertions++;

        GovernedFinding confirmedRepeat = service.transition(
                finding,
                request(
                        FindingLifecycleAction.CONFIRM,
                        "reviewer-001",
                        "review-decision-001",
                        "review-evidence-001"));
        TestSupport.assertEquals(
                confirmed.events().getFirst().eventId(),
                confirmedRepeat.events().getFirst().eventId(),
                "lifecycle event identity is deterministic");
        assertions++;
        TestSupport.assertEquals(
                confirmed.fingerprint(),
                confirmedRepeat.fingerprint(),
                "identical reviewed transition produces deterministic finding fingerprint");
        assertions++;

        boolean hasAutoConfirm = java.util.Arrays.stream(FindingLifecycleService.class.getDeclaredMethods())
                .map(Method::getName)
                .anyMatch(name -> name.toLowerCase().contains("autoconfirm")
                        || name.toLowerCase().contains("confirmautomatically"));
        TestSupport.assertTrue(!hasAutoConfirm,
                "lifecycle service exposes no auto-confirm API");
        assertions++;

        return assertions;
    }

    private static FindingLifecycleTransitionRequest request(
            FindingLifecycleAction action,
            String reviewer,
            String decision,
            String evidence) {
        return new FindingLifecycleTransitionRequest(
                action, reviewer, decision, List.of(evidence));
    }

    private static FindingCandidate candidate(
            String id,
            FindingCandidateState state,
            String confidence) {
        return new FindingCandidate(
                id,
                state,
                "acra-s13",
                List.of("test-s13"),
                List.of("execution-s13"),
                List.of("observation-s13"),
                List.of("assessment-s13"),
                List.of("OBJECT_AUTHORIZATION"),
                "/api/v1/documents/1002",
                "document:1002",
                "user-a",
                "FOREIGN_TENANT",
                AuthorizationDecision.DENY,
                AuthorizationDecision.ALLOW,
                List.of("evidence-s13-a", "evidence-s13-b"),
                List.of(),
                List.of("policy-s13"),
                confidence,
                "review-only lifecycle fixture",
                FindingFingerprint.of(
                        "/api/v1/documents/1002",
                        "document:1002",
                        "user-a",
                        "FOREIGN_TENANT",
                        "DENY_TO_ALLOW",
                        "OBJECT_AUTHORIZATION"));
    }
}
