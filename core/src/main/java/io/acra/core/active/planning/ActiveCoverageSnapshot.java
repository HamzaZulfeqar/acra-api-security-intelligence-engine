package io.acra.core.active.planning;

import io.acra.core.active.model.MutationType;
import java.util.Set;

public record ActiveCoverageSnapshot(
        int endpointsEligible,
        int endpointsTested,
        int contextsEligible,
        int contextsTested,
        int resourcesEligible,
        int resourcesTested,
        Set<MutationType> mutationCategoriesEligible,
        Set<MutationType> mutationCategoriesTested,
        int testsPlanned,
        int testsDeduplicated,
        int testsFiltered,
        int testsExecuted,
        int testsSkipped,
        int testsFailed,
        int testsBlocked,
        int testsCancelled) {
    public ActiveCoverageSnapshot {
        if (endpointsEligible < 0 || endpointsTested < 0 || contextsEligible < 0 || contextsTested < 0
                || resourcesEligible < 0 || resourcesTested < 0 || testsPlanned < 0 || testsDeduplicated < 0
                || testsFiltered < 0 || testsExecuted < 0 || testsSkipped < 0 || testsFailed < 0
                || testsBlocked < 0 || testsCancelled < 0) throw new IllegalArgumentException("negative coverage metric");
        mutationCategoriesEligible = Set.copyOf(mutationCategoriesEligible == null ? Set.of() : mutationCategoriesEligible);
        mutationCategoriesTested = Set.copyOf(mutationCategoriesTested == null ? Set.of() : mutationCategoriesTested);
    }
}
