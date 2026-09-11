package io.acra.core.active.execution;

import io.acra.core.active.evidence.EvidenceChainEntry;
import io.acra.core.active.evidence.Observation;
import io.acra.core.active.safety.ValidationDecision;
import io.acra.core.domain.testing.TestState;
import java.util.List;

public record TestExecutionResult(
        String executionId,
        String testId,
        TestState state,
        ValidationDecision validation,
        Observation observation,
        ExecutionFailure failure,
        List<EvidenceChainEntry> evidenceChain) {
    public TestExecutionResult {
        if (executionId == null || executionId.isBlank() || testId == null || testId.isBlank()
                || state == null || validation == null) {
            throw new IllegalArgumentException("execution result metadata required");
        }
        evidenceChain = List.copyOf(evidenceChain == null ? List.of() : evidenceChain);
        if (state == TestState.COMPLETED && observation == null) throw new IllegalArgumentException("completed result requires observation");
        if (failure != null && observation != null) throw new IllegalArgumentException("result cannot contain failure and observation");
    }
}
