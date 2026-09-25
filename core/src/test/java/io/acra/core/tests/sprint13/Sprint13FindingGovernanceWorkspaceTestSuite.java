package io.acra.core.tests.sprint13;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.AuthorizationImpactProfile;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.domain.finding.FindingLifecycleAction;
import io.acra.core.domain.finding.FindingLifecycleState;
import io.acra.core.domain.finding.FindingLifecycleTransitionRequest;
import io.acra.core.engine.AuthorizationSeverityEvaluator;
import io.acra.core.product.finding.FindingGovernanceQueue;
import io.acra.core.product.finding.FindingGovernanceWorkspace;
import io.acra.core.tests.TestSupport;
import java.lang.reflect.Method;
import java.util.List;

public final class Sprint13FindingGovernanceWorkspaceTestSuite {
    private Sprint13FindingGovernanceWorkspaceTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT13_FINDING_GOVERNANCE_WORKSPACE PASS assertions=" + assertions);
    }

    public static int run() {
        FindingGovernanceWorkspace workspace = new FindingGovernanceWorkspace();
        var candidateA = candidate("candidate-a", "document:1002");
        var candidateB = candidate("candidate-b", "document:1003");
        var riskA = risk(candidateA, true);
        var riskB = risk(candidateB, false);
        int assertions = 0;

        var findingA = workspace.open(candidateA, riskA);
        var findingB = workspace.open(candidateB, riskB);

        TestSupport.assertEquals(2, workspace.snapshot().findingCount(),
                "workspace indexes two governed findings");
        assertions++;
        TestSupport.assertEquals(2L,
                workspace.snapshot().queueCount(FindingGovernanceQueue.REVIEW_REQUIRED),
                "new candidates enter review-required queue");
        assertions++;
        TestSupport.assertEquals(0L,
                workspace.snapshot().queueCount(FindingGovernanceQueue.CONFIRMED),
                "no candidate is automatically confirmed");
        assertions++;

        var repeatedA = workspace.open(candidateA, riskA);
        TestSupport.assertEquals(findingA.fingerprint(), repeatedA.fingerprint(),
                "opening identical candidate/risk is idempotent");
        assertions++;
        TestSupport.assertEquals(2, workspace.snapshot().findingCount(),
                "idempotent open does not expand denominator");
        assertions++;

        var confirmedA = workspace.transition(
                findingA.findingId(),
                findingA.fingerprint(),
                request(FindingLifecycleAction.CONFIRM, "review-a"));
        TestSupport.assertEquals(FindingLifecycleState.CONFIRMED, confirmedA.state(),
                "workspace delegates explicit confirmation to lifecycle service");
        assertions++;
        TestSupport.assertEquals(1L,
                workspace.snapshot().queueCount(FindingGovernanceQueue.CONFIRMED),
                "confirmed queue reflects transition");
        assertions++;
        TestSupport.assertEquals(1L,
                workspace.snapshot().queueCount(FindingGovernanceQueue.REVIEW_REQUIRED),
                "other finding remains review-required");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> workspace.transition(
                        findingA.findingId(),
                        findingA.fingerprint(),
                        request(FindingLifecycleAction.START_REMEDIATION, "stale-review")),
                "stale fingerprint cannot transition current finding");
        assertions++;
        TestSupport.assertEquals(
                confirmedA.fingerprint(),
                workspace.find(findingA.findingId()).fingerprint(),
                "stale transition attempt cannot mutate workspace");
        assertions++;

        var remediationA = workspace.transition(
                confirmedA.findingId(),
                confirmedA.fingerprint(),
                request(FindingLifecycleAction.START_REMEDIATION, "remediation-a"));
        TestSupport.assertEquals(1L,
                workspace.snapshot().queueCount(FindingGovernanceQueue.REMEDIATION),
                "remediation queue reflects transition");
        assertions++;

        var retestA = workspace.transition(
                remediationA.findingId(),
                remediationA.fingerprint(),
                request(FindingLifecycleAction.REQUEST_RETEST, "retest-a"));
        TestSupport.assertEquals(1L,
                workspace.snapshot().queueCount(FindingGovernanceQueue.RETEST),
                "retest queue reflects transition");
        assertions++;

        var resolvedA = workspace.transition(
                retestA.findingId(),
                retestA.fingerprint(),
                request(FindingLifecycleAction.PASS_RETEST, "pass-a"));
        TestSupport.assertEquals(1L,
                workspace.snapshot().queueCount(FindingGovernanceQueue.TERMINAL),
                "resolved finding enters terminal queue");
        assertions++;

        var closedA = workspace.transition(
                resolvedA.findingId(),
                resolvedA.fingerprint(),
                request(FindingLifecycleAction.CLOSE, "close-a"));
        TestSupport.assertEquals(FindingLifecycleState.CLOSED, closedA.state(),
                "terminal finding can close through lifecycle service");
        assertions++;
        TestSupport.assertEquals(1L, workspace.snapshot().confirmedHistoryCount(),
                "closed resolved finding retains confirmed history");
        assertions++;

        var falsePositiveB = workspace.transition(
                findingB.findingId(),
                findingB.fingerprint(),
                request(FindingLifecycleAction.MARK_FALSE_POSITIVE, "fp-b"));
        TestSupport.assertEquals(2L,
                workspace.snapshot().queueCount(FindingGovernanceQueue.TERMINAL),
                "false-positive review joins terminal queue");
        assertions++;
        TestSupport.assertEquals(1L, workspace.snapshot().confirmedHistoryCount(),
                "false-positive disposition does not add confirmed history");
        assertions++;

        var closedB = workspace.transition(
                falsePositiveB.findingId(),
                falsePositiveB.fingerprint(),
                request(FindingLifecycleAction.CLOSE, "close-b"));
        TestSupport.assertTrue(!closedB.confirmed(),
                "closed false-positive finding remains historically unconfirmed");
        assertions++;

        var driftRisk = risk(candidateA, false);
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> workspace.open(candidateA, driftRisk),
                "existing candidate cannot silently drift to a new risk assessment");
        assertions++;

        var driftCandidate = candidate("candidate-a", "document:DIFFERENT");
        var driftCandidateRisk = risk(driftCandidate, true);
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> workspace.open(driftCandidate, driftCandidateRisk),
                "existing candidate ID cannot silently drift candidate fingerprint");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> workspace.transition(
                        "missing-finding",
                        "missing-fingerprint",
                        request(FindingLifecycleAction.CONFIRM, "missing")),
                "unknown finding transition fails closed");
        assertions++;

        TestSupport.assertEquals(
                List.of(closedA.findingId(), closedB.findingId()).stream().sorted().toList(),
                workspace.snapshot().findings().stream()
                        .map(value -> value.findingId()).sorted().toList(),
                "snapshot ordering is deterministic");
        assertions++;

        boolean hasDirectStateMutation = java.util.Arrays.stream(
                        FindingGovernanceWorkspace.class.getDeclaredMethods())
                .map(Method::getName)
                .anyMatch(name -> name.equalsIgnoreCase("setState")
                        || name.equalsIgnoreCase("confirm")
                        || name.equalsIgnoreCase("publish"));
        TestSupport.assertTrue(!hasDirectStateMutation,
                "workspace exposes no direct state/publish shortcut");
        assertions++;

        workspace.clear();
        TestSupport.assertEquals(0, workspace.snapshot().findingCount(),
                "clear removes finding and candidate indexes");
        assertions++;

        var reopened = workspace.open(driftCandidate, driftCandidateRisk);
        TestSupport.assertEquals(FindingLifecycleState.REVIEW_REQUIRED, reopened.state(),
                "cleared workspace can accept a new candidate identity");
        assertions++;

        return assertions;
    }

    private static FindingLifecycleTransitionRequest request(
            FindingLifecycleAction action,
            String suffix) {
        return new FindingLifecycleTransitionRequest(
                action,
                "reviewer-" + suffix,
                "decision-" + suffix,
                List.of("evidence-" + suffix));
    }

    private static io.acra.core.domain.finding.AuthorizationRiskAssessment risk(
            FindingCandidate candidate,
            boolean highImpact) {
        return new AuthorizationSeverityEvaluator().evaluate(
                candidate,
                highImpact
                        ? new AuthorizationImpactProfile(
                                true, true, false, true, true, false, List.of("high-impact"))
                        : AuthorizationImpactProfile.none());
    }

    private static FindingCandidate candidate(String id, String resource) {
        return new FindingCandidate(
                id,
                FindingCandidateState.CANDIDATE,
                "acra-s13",
                List.of("test-" + id),
                List.of("execution-" + id),
                List.of("observation-" + id),
                List.of("assessment-" + id),
                List.of("OBJECT_AUTHORIZATION"),
                "/api/v1/documents/{id}",
                resource,
                "user-a",
                "FOREIGN_TENANT",
                AuthorizationDecision.DENY,
                AuthorizationDecision.ALLOW,
                List.of("evidence-" + id),
                List.of(),
                List.of("policy-" + id),
                "HIGH",
                "review-only governance fixture",
                FindingFingerprint.of(
                        "/api/v1/documents/{id}",
                        resource,
                        "user-a",
                        "FOREIGN_TENANT",
                        "DENY_TO_ALLOW",
                        "OBJECT_AUTHORIZATION"));
    }
}
