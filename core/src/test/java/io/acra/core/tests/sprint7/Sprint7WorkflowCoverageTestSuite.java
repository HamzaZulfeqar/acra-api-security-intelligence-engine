package io.acra.core.tests.sprint7;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.PolicyResolutionState;
import io.acra.core.domain.workflow.WorkflowAuthorizationResolution;
import io.acra.core.domain.workflow.WorkflowCoverageStage;
import io.acra.core.engine.S7WorkflowCoverageTracker;
import io.acra.core.tests.TestSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Sprint7WorkflowCoverageTestSuite {
    private Sprint7WorkflowCoverageTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT7_WORKFLOW_COVERAGE PASS assertions=" + assertions);
    }

    public static int run() {
        int assertions = 0;
        S7WorkflowCoverageTracker tracker = new S7WorkflowCoverageTracker();

        WorkflowAuthorizationResolution unresolved = resolution(
                "r-conflict", "workflow-b", "principal-b", "tenant-b", "resource-b",
                "APPROVE", "SUBMITTED", "APPROVED", AuthorizationDecision.UNKNOWN,
                PolicyResolutionState.CONFLICTING);
        WorkflowAuthorizationResolution allow = resolution(
                "r-allow", "workflow-a", "principal-a", "tenant-a", "resource-a",
                "SUBMIT", "DRAFT", "SUBMITTED", AuthorizationDecision.ALLOW,
                PolicyResolutionState.RESOLVED_ALLOW);
        WorkflowAuthorizationResolution deny = resolution(
                "r-deny", "workflow-a", "principal-a", "tenant-a", "resource-a",
                "SUBMIT", "DRAFT", "APPROVED", AuthorizationDecision.DENY,
                PolicyResolutionState.RESOLVED_DENY);

        tracker.recordResolution(unresolved);
        tracker.recordResolution(deny);
        tracker.recordResolution(allow);
        tracker.recordResolution(allow);

        TestSupport.assertEquals(3, tracker.matrix().size(),
                "duplicate resolution must not duplicate workflow coverage context");
        assertions++;
        TestSupport.assertEquals(3, tracker.matrix().summary().totalContexts(), "three policy contexts tracked");
        assertions++;
        TestSupport.assertEquals(2, tracker.matrix().summary().resolvedContexts(), "two contexts resolved");
        assertions++;
        TestSupport.assertEquals(1, tracker.matrix().summary().unresolvedContexts(), "one conflict remains unresolved");
        assertions++;
        TestSupport.assertEquals(0, tracker.matrix().summary().plannedContexts(),
                "policy-only coverage is not reported as planned");
        assertions++;
        TestSupport.assertEquals(0, tracker.matrix().summary().observedContexts(),
                "policy-only coverage is not reported as observed");
        assertions++;
        TestSupport.assertEquals(2, tracker.matrix().missingPlanningCoverageIds().size(),
                "both resolved contexts remain missing planning coverage");
        assertions++;
        TestSupport.assertEquals(1, tracker.matrix().unresolvedCoverageIds().size(),
                "conflict stays visible as unresolved coverage");
        assertions++;
        TestSupport.assertTrue(tracker.matrix().entries().stream()
                        .allMatch(entry -> entry.stage() == WorkflowCoverageStage.POLICY_ONLY),
                "resolution registration alone remains POLICY_ONLY");
        assertions++;

        List<String> ids = tracker.matrix().entries().stream().map(entry -> entry.coverageId()).toList();
        List<String> sorted = new ArrayList<>(ids);
        Collections.sort(sorted);
        TestSupport.assertEquals(sorted, ids, "coverage matrix ordering must be deterministic");
        assertions++;

        TestSupport.assertEquals(2.0 / 3.0, tracker.matrix().summary().policyResolutionRatio(),
                "policy resolution ratio is based on all recorded contexts");
        assertions++;
        TestSupport.assertEquals(0.0, tracker.matrix().summary().planningRatio(),
                "planning ratio denominator is resolved contexts");
        assertions++;
        TestSupport.assertEquals(0.0, tracker.matrix().summary().observationRatio(),
                "observation ratio is zero until execution evidence exists");
        assertions++;

        return assertions;
    }

    private static WorkflowAuthorizationResolution resolution(
            String resolutionId,
            String workflowId,
            String principalId,
            String tenantId,
            String resourceId,
            String action,
            String from,
            String to,
            AuthorizationDecision expected,
            PolicyResolutionState state) {
        return new WorkflowAuthorizationResolution(
                resolutionId,
                "workflow-policy-coverage",
                workflowId,
                principalId,
                tenantId,
                resourceId,
                action,
                from,
                to,
                List.of("rule-" + resolutionId),
                List.of(),
                List.of(),
                expected,
                AuthorizationDecision.UNKNOWN,
                state,
                List.of("evidence-" + resolutionId),
                state == PolicyResolutionState.CONFLICTING ? List.of("conflicting-policy") : List.of());
    }
}
