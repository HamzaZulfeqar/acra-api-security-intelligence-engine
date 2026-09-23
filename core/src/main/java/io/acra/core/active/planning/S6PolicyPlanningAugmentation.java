package io.acra.core.active.planning;

import java.util.List;

public record S6PolicyPlanningAugmentation(
        PlanningInput planningInput,
        List<TestSeed> generatedSeeds,
        List<String> skippedReasons) {

    public S6PolicyPlanningAugmentation {
        if (planningInput == null) throw new IllegalArgumentException("planningInput required");
        generatedSeeds = List.copyOf(generatedSeeds == null ? List.of() : generatedSeeds);
        skippedReasons = List.copyOf(skippedReasons == null ? List.of() : skippedReasons);
    }
}
