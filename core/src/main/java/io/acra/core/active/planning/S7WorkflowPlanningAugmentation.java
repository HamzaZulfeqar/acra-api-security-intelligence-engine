package io.acra.core.active.planning;

import java.util.List;

public record S7WorkflowPlanningAugmentation(
        PlanningInput planningInput,
        List<TestSeed> generatedSeeds,
        List<String> skippedReasons) {
    public S7WorkflowPlanningAugmentation {
        if (planningInput == null) throw new IllegalArgumentException("planningInput required");
        generatedSeeds = List.copyOf(generatedSeeds == null ? List.of() : generatedSeeds);
        skippedReasons = List.copyOf(skippedReasons == null ? List.of() : skippedReasons);
    }
}
