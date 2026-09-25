package io.acra.core.engine;

import io.acra.core.active.execution.TestExecutionResult;
import io.acra.core.active.model.MutationType;
import io.acra.core.active.model.TestContract;
import io.acra.core.active.planning.TestSeed;
import io.acra.core.domain.authorization.PolicyResolutionState;
import io.acra.core.domain.testing.TestState;
import io.acra.core.domain.workflow.WorkflowAuthorizationResolution;
import io.acra.core.domain.workflow.WorkflowTransitionCoverageEntry;
import io.acra.core.domain.workflow.WorkflowTransitionCoverageMatrix;
import java.util.HashMap;
import java.util.Map;

public final class S7WorkflowCoverageTracker {
    private final WorkflowTransitionCoverageMatrix matrix = new WorkflowTransitionCoverageMatrix();
    private final Map<String, String> testToCoverageId = new HashMap<>();

    public synchronized WorkflowTransitionCoverageEntry recordResolution(WorkflowAuthorizationResolution resolution) {
        WorkflowTransitionCoverageEntry fresh = WorkflowTransitionCoverageEntry.from(resolution);
        WorkflowTransitionCoverageEntry merged = matrix.find(fresh.coverageId())
                .map(fresh::carryLifecycleFrom)
                .orElse(fresh);
        matrix.put(merged);
        return merged;
    }

    public synchronized WorkflowTransitionCoverageEntry recordPlanned(
            WorkflowAuthorizationResolution resolution,
            TestSeed seed) {
        if (resolution == null || seed == null) throw new IllegalArgumentException("resolution and seed required");
        if (!resolved(resolution.state())) {
            throw new IllegalArgumentException("unresolved workflow policy cannot be recorded as planned");
        }
        if (seed.contract() != TestContract.TRANSITION
                || seed.mutation().type() != MutationType.WORKFLOW_TRANSITION) {
            throw new IllegalArgumentException("S7 workflow coverage requires a WORKFLOW_TRANSITION seed");
        }
        if (!seed.mutation().mutatedValue().equals(resolution.toState())) {
            throw new IllegalArgumentException("workflow target state does not match coverage resolution");
        }
        if (seed.expectedDecision() != resolution.expectedDecision()) {
            throw new IllegalArgumentException("seed expected decision does not match workflow resolution");
        }
        if (!seed.targetContext().principal().equals(resolution.principalId())
                || !seed.targetContext().tenant().equals(resolution.tenantId())
                || !seed.targetContext().resource().equals(resolution.resourceId())
                || !seed.targetContext().action().equals(resolution.action())) {
            throw new IllegalArgumentException("seed context does not match workflow resolution");
        }
        if (!seed.expectedEvidence().containsAll(resolution.evidenceIds())) {
            throw new IllegalArgumentException("seed does not preserve workflow policy evidence");
        }

        WorkflowTransitionCoverageEntry entry = recordResolution(resolution).withPlannedTest(seed.testId());
        String previous = testToCoverageId.putIfAbsent(seed.testId(), entry.coverageId());
        if (previous != null && !previous.equals(entry.coverageId())) {
            throw new IllegalArgumentException("testId already belongs to a different workflow coverage context");
        }
        matrix.put(entry);
        return entry;
    }

    public synchronized WorkflowTransitionCoverageEntry recordExecution(
            WorkflowAuthorizationResolution resolution,
            TestExecutionResult result) {
        if (resolution == null || result == null) throw new IllegalArgumentException("resolution and result required");
        WorkflowTransitionCoverageEntry expected = WorkflowTransitionCoverageEntry.from(resolution);
        String mappedCoverage = testToCoverageId.get(result.testId());
        if (mappedCoverage == null) {
            throw new IllegalArgumentException("execution test was not recorded as planned workflow coverage");
        }
        if (!mappedCoverage.equals(expected.coverageId())) {
            throw new IllegalArgumentException("execution belongs to a different workflow coverage context");
        }
        WorkflowTransitionCoverageEntry current = matrix.find(mappedCoverage)
                .orElseThrow(() -> new IllegalStateException("planned workflow coverage entry missing"));

        String observationId = "";
        java.util.List<String> observationEvidence = java.util.List.of();
        if (result.observation() != null) {
            if (!result.observation().testId().equals(result.testId())) {
                throw new IllegalArgumentException("observation/test identity mismatch");
            }
            if (result.observation().expectedDecision().decision() != resolution.expectedDecision()) {
                throw new IllegalArgumentException("observation expected decision does not match workflow resolution");
            }
            observationId = result.observation().observationId();
            observationEvidence = result.observation().evidenceIds();
        } else if (result.state() == TestState.COMPLETED) {
            throw new IllegalArgumentException("completed workflow execution requires observation");
        }

        WorkflowTransitionCoverageEntry updated = current.withExecution(
                result.executionId(), observationId, observationEvidence);
        matrix.put(updated);
        return updated;
    }

    public WorkflowTransitionCoverageMatrix matrix() {
        return matrix;
    }

    private static boolean resolved(PolicyResolutionState state) {
        return state == PolicyResolutionState.RESOLVED_ALLOW || state == PolicyResolutionState.RESOLVED_DENY;
    }
}
