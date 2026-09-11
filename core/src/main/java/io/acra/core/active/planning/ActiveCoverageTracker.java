package io.acra.core.active.planning;

import io.acra.core.active.execution.TestExecutionResult;
import io.acra.core.active.model.MutationType;
import io.acra.core.active.model.SecurityTest;
import io.acra.core.domain.resource.Resource;
import io.acra.core.domain.testing.TestState;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public final class ActiveCoverageTracker {
    private final PlanningInput input;
    private final PlanningResult planning;
    private final Set<String> recordedExecutions = new HashSet<>();
    private int executed;
    private int runtimeSkipped;
    private int failed;
    private int blocked;
    private int cancelled;

    public ActiveCoverageTracker(PlanningInput input, PlanningResult planning) {
        if (input == null || planning == null) throw new IllegalArgumentException("planning input/result required");
        this.input = input;
        this.planning = planning;
    }

    public synchronized boolean record(TestExecutionResult result) {
        if (result == null) throw new IllegalArgumentException("execution result required");
        if (!recordedExecutions.add(result.executionId())) return false;
        switch (result.state()) {
            case COMPLETED -> executed++;
            case SKIPPED -> runtimeSkipped++;
            case FAILED -> failed++;
            case BLOCKED -> blocked++;
            case CANCELLED -> cancelled++;
            default -> { }
        }
        return true;
    }

    public synchronized ActiveCoverageSnapshot snapshot() {
        Set<String> eligibleEndpoints = new LinkedHashSet<>();
        input.apiInventory().forEach(record -> eligibleEndpoints.add(record.endpoint().endpointId()));
        Set<String> testedEndpoints = new LinkedHashSet<>();
        Set<Object> testedContexts = new LinkedHashSet<>();
        Set<String> eligibleResources = new LinkedHashSet<>();
        Set<String> testedResources = new LinkedHashSet<>();
        Set<MutationType> eligibleMutations = new LinkedHashSet<>();
        Set<MutationType> testedMutations = new LinkedHashSet<>();
        input.seeds().forEach(seed -> {
            eligibleMutations.add(seed.mutation().type());
            addResource(eligibleResources, seed.sourceResource());
            addResource(eligibleResources, seed.targetResource());
        });
        for (SecurityTest test : planning.plan().tests()) {
            testedEndpoints.add(test.endpoint().endpointId());
            testedContexts.add(test.sourceContext());
            testedContexts.add(test.targetContext());
            addResource(testedResources, test.sourceResource());
            addResource(testedResources, test.targetResource());
            testedMutations.add(test.mutation().type());
        }
        PlanningMetrics metrics = planning.metrics();
        return new ActiveCoverageSnapshot(eligibleEndpoints.size(), testedEndpoints.size(),
                input.securityContexts().size(), testedContexts.size(), eligibleResources.size(), testedResources.size(),
                eligibleMutations, testedMutations, metrics.plannedTests(), metrics.deduplicatedTests(),
                metrics.filteredTests(), executed, planning.plan().skipped().size() + runtimeSkipped,
                failed, blocked, cancelled);
    }

    public synchronized RequestEfficiencySnapshot efficiency() {
        PlanningMetrics metrics = planning.metrics();
        return new RequestEfficiencySnapshot(metrics.candidateTests(), metrics.deduplicatedTests(),
                metrics.scopeFilteredTests(), metrics.budgetFilteredTests(), executed);
    }

    private static void addResource(Set<String> values, Resource resource) {
        if (resource != null) values.add(resource.resourceType() + ':' + resource.resourceId());
    }
}
