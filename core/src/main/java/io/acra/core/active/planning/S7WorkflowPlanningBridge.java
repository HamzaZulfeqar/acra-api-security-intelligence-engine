package io.acra.core.active.planning;

import io.acra.core.active.model.ConfigurationSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

public final class S7WorkflowPlanningBridge {
    private final S7WorkflowTestSeedFactory seedFactory = new S7WorkflowTestSeedFactory();

    public S7WorkflowPlanningAugmentation augment(PlanningInput base, List<S7WorkflowPlanningCandidate> candidates) {
        if (base == null) throw new IllegalArgumentException("base planning input required");
        List<TestSeed> generated = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        TreeSet<String> policyFingerprints = new TreeSet<>();
        for (S7WorkflowPlanningCandidate candidate : candidates == null
                ? List.<S7WorkflowPlanningCandidate>of() : candidates) {
            S7WorkflowSeedGenerationResult result = seedFactory.generate(candidate);
            generated.addAll(result.seeds());
            skipped.addAll(result.skippedReasons());
            policyFingerprints.add(candidate.targetResolution().policyFingerprint());
        }

        List<TestSeed> merged = new ArrayList<>(base.seeds());
        merged.addAll(generated);
        TreeMap<String, String> values = new TreeMap<>(base.configurationSnapshot().values());
        values.put("s7WorkflowAwarePlanning", "true");
        values.put("s7GeneratedSeedCount", Integer.toString(generated.size()));
        values.put("s7GenerationSkipCount", Integer.toString(skipped.size()));
        values.put("s7WorkflowPolicyFingerprintCount", Integer.toString(policyFingerprints.size()));
        ConfigurationSnapshot configuration = ConfigurationSnapshot.of(values);

        PlanningInput augmented = new PlanningInput(
                base.planId(), base.apiInventory(), base.securityContextGraph(), base.authorizationMatrix(),
                base.securityContexts(), base.routes(), base.openApi(), base.target(), base.enabledContracts(),
                base.riskPriority(), base.contextCoverage(), base.requestBudget(), base.mutationBudget(),
                base.safetyPolicy(), base.selectionMode(), base.profile(), configuration, merged, base.createdAt());
        return new S7WorkflowPlanningAugmentation(augmented, generated, skipped);
    }
}
