package io.acra.core.active.planning;

import io.acra.core.active.model.Mutation;
import io.acra.core.active.model.MutationLocation;
import io.acra.core.active.model.MutationType;
import io.acra.core.active.model.SafetyClass;
import io.acra.core.active.model.TestContract;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.PolicyResolutionState;
import io.acra.core.domain.http.HttpMethod;
import io.acra.core.security.TokenFingerprint;
import java.util.ArrayList;
import java.util.List;

public final class S6PolicyTestSeedFactory {
    private final S6PolicyPlanningAdvisor advisor = new S6PolicyPlanningAdvisor();

    public S6PolicySeedGenerationResult generate(S6PolicyPlanningCandidate candidate) {
        if (candidate == null) throw new IllegalArgumentException("candidate required");
        List<TestSeed> seeds = new ArrayList<>();
        List<String> skipped = new ArrayList<>();

        if (!resolvable(candidate.targetResolution())) {
            skipped.add(candidate.candidateId() + ":TARGET_POLICY_UNRESOLVED");
            return new S6PolicySeedGenerationResult(seeds, skipped);
        }
        if (!resolvable(candidate.baselineResolution())) {
            skipped.add(candidate.candidateId() + ":BASELINE_POLICY_UNRESOLVED");
            return new S6PolicySeedGenerationResult(seeds, skipped);
        }

        for (S6PolicyPlanningRecommendation recommendation : advisor.recommend(candidate.targetResolution())) {
            if (!recommendation.recommended() || !candidate.allowedContracts().contains(recommendation.contract())) {
                continue;
            }
            switch (recommendation.contract()) {
                case CROSS_TENANT -> crossTenant(candidate, recommendation, seeds, skipped);
                case ROLE_COMPARISON -> skipped.add(candidate.candidateId()
                        + ":ROLE_COMPARISON_REQUIRES_CREDENTIAL_SAFE_CONTEXT_SUBSTITUTION");
                default -> skipped.add(candidate.candidateId() + ":UNSUPPORTED_S6_CONTRACT:"
                        + recommendation.contract().name());
            }
        }
        return new S6PolicySeedGenerationResult(seeds, skipped);
    }

    private void crossTenant(S6PolicyPlanningCandidate candidate, S6PolicyPlanningRecommendation recommendation,
                             List<TestSeed> seeds, List<String> skipped) {
        String sourceTenant = candidate.sourceContext().tenant();
        String targetTenant = candidate.targetContext().tenant();
        if (sourceTenant == null || sourceTenant.isBlank() || targetTenant == null || targetTenant.isBlank()
                || sourceTenant.equals(targetTenant)) {
            skipped.add(candidate.candidateId() + ":CROSS_TENANT_CONTEXT_NOT_DISTINCT");
            return;
        }
        String rawTarget = candidate.baseline().request().rawTarget();
        if (occurrences(rawTarget, sourceTenant) != 1) {
            skipped.add(candidate.candidateId() + ":BASELINE_PATH_DOES_NOT_CONTAIN_ONE_SOURCE_TENANT");
            return;
        }

        String key = candidate.targetResolution().policyFingerprint() + "|" + candidate.endpoint().endpointId()
                + "|" + sourceTenant + "->" + targetTenant + "|" + recommendation.contract();
        String suffix = TokenFingerprint.sha256(key).substring(0, 24);
        SafetyClass safetyClass = safe(candidate.endpoint().method())
                ? SafetyClass.SAFE_READ_ONLY : SafetyClass.STATE_CHANGING;
        Mutation mutation = new Mutation(
                "S6-MUT-TENANT-" + suffix,
                MutationType.TENANT_SUBSTITUTION,
                MutationLocation.TENANT,
                sourceTenant,
                targetTenant,
                context(candidate.sourceContext()),
                context(candidate.targetContext()),
                "policy-aware tenant substitution using explicit supplied S6 contexts",
                "target policy expects " + candidate.targetResolution().expectedDecision(),
                safetyClass,
                "s6-tenant:" + suffix);

        List<String> invariants = new ArrayList<>(candidate.invariants());
        invariants.add("only the declared tenant path value is changed by the generated mutation");
        invariants.add("authorization credentials are supplied by explicit request controls and are never copied into Mutation");
        invariants.add("target policy fingerprint=" + candidate.targetResolution().policyFingerprint());

        List<String> evidence = candidate.targetResolution().evidenceIds();
        seeds.add(new TestSeed(
                "S6-AUTO-TENANT-" + suffix,
                "1",
                TestContract.CROSS_TENANT,
                candidate.endpoint(),
                candidate.baseline(),
                candidate.positiveControl(),
                candidate.negativeControl(),
                mutation,
                candidate.sourceContext(),
                candidate.targetContext(),
                candidate.sourceResource(),
                candidate.targetResource(),
                candidate.targetResolution().expectedDecision(),
                evidence,
                candidate.dependencies(),
                invariants,
                4,
                true,
                candidate.userSelected(),
                candidate.userExcluded(),
                recommendation.reason() + "; policy=" + candidate.targetResolution().policyFingerprint()));
    }

    private static boolean resolvable(io.acra.core.domain.authorization.EffectiveAuthorizationResolution resolution) {
        return resolution != null
                && resolution.expectedDecision() != AuthorizationDecision.UNKNOWN
                && resolution.state() != PolicyResolutionState.CONFLICTING
                && resolution.state() != PolicyResolutionState.INCOMPLETE
                && resolution.state() != PolicyResolutionState.UNKNOWN;
    }

    private static boolean safe(HttpMethod method) {
        return method == HttpMethod.GET || method == HttpMethod.HEAD || method == HttpMethod.OPTIONS;
    }

    private static int occurrences(String source, String value) {
        int count = 0;
        int offset = 0;
        while (source != null && value != null && !value.isEmpty()
                && (offset = source.indexOf(value, offset)) >= 0) {
            count++;
            offset += value.length();
        }
        return count;
    }

    private static String context(io.acra.core.recon.SecurityContextFingerprint value) {
        return value.principal() + "|" + value.role() + "|" + value.tenant();
    }
}
