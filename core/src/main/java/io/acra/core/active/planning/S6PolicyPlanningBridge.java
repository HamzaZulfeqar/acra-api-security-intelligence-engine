package io.acra.core.active.planning;

import io.acra.core.active.model.ConfigurationSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class S6PolicyPlanningBridge {
    private final S6PolicyTestSeedFactory seedFactory = new S6PolicyTestSeedFactory();

    public S6PolicyPlanningAugmentation augment(PlanningInput base, List<S6PolicyPlanningCandidate> candidates) {
        if (base == null) throw new IllegalArgumentException("base planning input required");
        List<TestSeed> generated = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        Set<String> policyFingerprints = new TreeSet<>();

        for (S6PolicyPlanningCandidate candidate : candidates == null
                ? List.<S6PolicyPlanningCandidate>of() : candidates) {
            S6PolicySeedGenerationResult result = seedFactory.generate(candidate);
            generated.addAll(result.seeds());
            skipped.addAll(result.skippedReasons());
            policyFingerprints.add(candidate.targetResolution().policyFingerprint());
        }

        List<TestSeed> merged = new ArrayList<>(base.seeds());
        merged.addAll(generated);
        TreeMap<String, String> values = new TreeMap<>(base.configurationSnapshot().values());
        values.put("s6PolicyAwarePlanning", "true");
        values.put("s6GeneratedSeedCount", Integer.toString(generated.size()));
        values.put("s6GenerationSkipCount", Integer.toString(skipped.size()));
        values.put("s6PolicyFingerprintCount", Integer.toString(policyFingerprints.size()));
        ConfigurationSnapshot configuration = ConfigurationSnapshot.of(values);

        PlanningInput augmented = new PlanningInput(
                base.planId(),
                base.apiInventory(),
                base.securityContextGraph(),
                base.authorizationMatrix(),
                base.securityContexts(),
                base.routes(),
                base.openApi(),
                base.target(),
                base.enabledContracts(),
                base.riskPriority(),
                base.contextCoverage(),
                base.requestBudget(),
                base.mutationBudget(),
                base.safetyPolicy(),
                base.selectionMode(),
                base.profile(),
                configuration,
                merged,
                base.createdAt());

        return new S6PolicyPlanningAugmentation(augmented, generated, skipped);
    }
}
