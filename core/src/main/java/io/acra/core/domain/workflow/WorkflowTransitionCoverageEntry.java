package io.acra.core.domain.workflow;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.PolicyResolutionState;
import io.acra.core.security.TokenFingerprint;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public record WorkflowTransitionCoverageEntry(
        String coverageId,
        String resolutionId,
        String policyFingerprint,
        String workflowId,
        String principalId,
        String tenantId,
        String resourceId,
        String action,
        String fromState,
        String toState,
        AuthorizationDecision expectedDecision,
        PolicyResolutionState resolutionState,
        List<String> matchedRuleIds,
        List<String> policyEvidenceIds,
        List<String> plannedTestIds,
        List<String> executionIds,
        List<String> observationIds,
        List<String> observationEvidenceIds) {

    public WorkflowTransitionCoverageEntry {
        coverageId = required(coverageId, "coverageId");
        resolutionId = required(resolutionId, "resolutionId");
        policyFingerprint = required(policyFingerprint, "policyFingerprint");
        workflowId = required(workflowId, "workflowId");
        principalId = required(principalId, "principalId");
        tenantId = safe(tenantId);
        resourceId = safe(resourceId);
        action = required(action, "action");
        fromState = required(fromState, "fromState");
        toState = required(toState, "toState");
        expectedDecision = expectedDecision == null ? AuthorizationDecision.UNKNOWN : expectedDecision;
        resolutionState = resolutionState == null ? PolicyResolutionState.UNKNOWN : resolutionState;
        matchedRuleIds = sorted(matchedRuleIds);
        policyEvidenceIds = sorted(policyEvidenceIds);
        plannedTestIds = sorted(plannedTestIds);
        executionIds = sorted(executionIds);
        observationIds = sorted(observationIds);
        observationEvidenceIds = sorted(observationEvidenceIds);
    }

    public static WorkflowTransitionCoverageEntry from(WorkflowAuthorizationResolution resolution) {
        if (resolution == null) throw new IllegalArgumentException("resolution required");
        String material = String.join("|",
                resolution.policyFingerprint(),
                resolution.workflowId(),
                resolution.principalId(),
                resolution.tenantId(),
                resolution.resourceId(),
                resolution.action(),
                resolution.fromState(),
                resolution.toState());
        return new WorkflowTransitionCoverageEntry(
                "s7-coverage-" + TokenFingerprint.sha256(material).substring(0, 24),
                resolution.resolutionId(),
                resolution.policyFingerprint(),
                resolution.workflowId(),
                resolution.principalId(),
                resolution.tenantId(),
                resolution.resourceId(),
                resolution.action(),
                resolution.fromState(),
                resolution.toState(),
                resolution.expectedDecision(),
                resolution.state(),
                resolution.matchedRuleIds(),
                resolution.evidenceIds(),
                List.of(),
                List.of(),
                List.of(),
                List.of());
    }

    public boolean resolved() {
        return resolutionState == PolicyResolutionState.RESOLVED_ALLOW
                || resolutionState == PolicyResolutionState.RESOLVED_DENY;
    }

    public WorkflowCoverageStage stage() {
        if (!observationIds.isEmpty()) return WorkflowCoverageStage.OBSERVED;
        if (!executionIds.isEmpty()) return WorkflowCoverageStage.ATTEMPTED;
        if (!plannedTestIds.isEmpty()) return WorkflowCoverageStage.PLANNED;
        return WorkflowCoverageStage.POLICY_ONLY;
    }

    public WorkflowTransitionCoverageEntry carryLifecycleFrom(WorkflowTransitionCoverageEntry existing) {
        if (existing == null) return this;
        if (!coverageId.equals(existing.coverageId())) {
            throw new IllegalArgumentException("coverage identity mismatch");
        }
        if (expectedDecision != existing.expectedDecision()
                || resolutionState != existing.resolutionState()
                || !matchedRuleIds.equals(existing.matchedRuleIds())) {
            throw new IllegalArgumentException("workflow policy resolution drift for existing coverage identity");
        }
        return new WorkflowTransitionCoverageEntry(
                coverageId, resolutionId, policyFingerprint, workflowId, principalId, tenantId, resourceId,
                action, fromState, toState, expectedDecision, resolutionState, matchedRuleIds,
                union(policyEvidenceIds, existing.policyEvidenceIds()),
                existing.plannedTestIds(), existing.executionIds(), existing.observationIds(),
                existing.observationEvidenceIds());
    }

    public WorkflowTransitionCoverageEntry withPlannedTest(String testId) {
        return new WorkflowTransitionCoverageEntry(
                coverageId, resolutionId, policyFingerprint, workflowId, principalId, tenantId, resourceId,
                action, fromState, toState, expectedDecision, resolutionState, matchedRuleIds, policyEvidenceIds,
                union(plannedTestIds, List.of(required(testId, "testId"))), executionIds, observationIds,
                observationEvidenceIds);
    }

    public WorkflowTransitionCoverageEntry withExecution(
            String executionId,
            String observationId,
            List<String> evidenceIds) {
        String execution = required(executionId, "executionId");
        List<String> observations = observationId == null || observationId.isBlank()
                ? observationIds : union(observationIds, List.of(observationId));
        return new WorkflowTransitionCoverageEntry(
                coverageId, resolutionId, policyFingerprint, workflowId, principalId, tenantId, resourceId,
                action, fromState, toState, expectedDecision, resolutionState, matchedRuleIds, policyEvidenceIds,
                plannedTestIds, union(executionIds, List.of(execution)), observations,
                union(observationEvidenceIds, evidenceIds));
    }

    private static List<String> union(List<String> left, List<String> right) {
        TreeSet<String> values = new TreeSet<>();
        if (left != null) values.addAll(left);
        if (right != null) values.addAll(right);
        values.removeIf(value -> value == null || value.isBlank());
        return List.copyOf(values);
    }

    private static List<String> sorted(List<String> values) {
        return union(List.of(), values);
    }

    private static String required(String value, String name) {
        String safe = safe(value);
        if (safe.isBlank()) throw new IllegalArgumentException(name + " required");
        return safe;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
