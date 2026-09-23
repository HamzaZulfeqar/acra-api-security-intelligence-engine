package io.acra.core.active.planning;

import java.util.List;

public record S6PolicySeedGenerationResult(
        List<TestSeed> seeds,
        List<String> skippedReasons) {

    public S6PolicySeedGenerationResult {
        seeds = List.copyOf(seeds == null ? List.of() : seeds);
        skippedReasons = List.copyOf(skippedReasons == null ? List.of() : skippedReasons);
    }
}
