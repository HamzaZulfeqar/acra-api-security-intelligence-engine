package io.acra.core.active.planning;

import io.acra.core.active.model.TestContract;
import io.acra.core.domain.authorization.EffectiveAuthorizationResolution;
import io.acra.core.domain.authorization.PolicyResolutionState;
import io.acra.core.domain.authorization.TenantRelationship;
import java.util.ArrayList;
import java.util.List;

public final class S6PolicyPlanningAdvisor {

    public List<S6PolicyPlanningRecommendation> recommend(EffectiveAuthorizationResolution resolution) {
        if (resolution == null) return List.of();
        List<S6PolicyPlanningRecommendation> out = new ArrayList<>();

        TenantRelationship relationship = resolution.tenantRelationship();
        if (relationship == TenantRelationship.CROSS_TENANT) {
            out.add(new S6PolicyPlanningRecommendation(TestContract.CROSS_TENANT, true,
                    resolution.mismatchCandidate() ? 100 : 85, resolution.policyFingerprint(),
                    "Explicit cross-tenant relationship merits controlled tenant-isolation coverage"));
        } else if (relationship == TenantRelationship.DELEGATED || relationship == TenantRelationship.GLOBAL
                || relationship == TenantRelationship.SHARED) {
            out.add(new S6PolicyPlanningRecommendation(TestContract.CROSS_TENANT, true, 55,
                    resolution.policyFingerprint(),
                    "Legitimate cross-boundary context should be retained as a false-positive control"));
        }

        if (!resolution.effectiveRoleIds().isEmpty() || resolution.mismatchCandidate()
                || resolution.state() == PolicyResolutionState.CONFLICTING) {
            int boost = resolution.state() == PolicyResolutionState.CONFLICTING ? 95
                    : resolution.mismatchCandidate() ? 90 : 65;
            String reason = resolution.effectiveRoleIds().isEmpty()
                    ? "Resolved deny/observed allow merits controlled RBAC comparison even though no target-scope role is effective"
                    : "Effective roles and permissions are available for controlled RBAC comparison";
            out.add(new S6PolicyPlanningRecommendation(TestContract.ROLE_COMPARISON, true, boost,
                    resolution.policyFingerprint(), reason));
        }

        return out.stream().sorted((a,b) -> {
            int p = Integer.compare(b.priorityBoost(), a.priorityBoost());
            return p != 0 ? p : a.contract().name().compareTo(b.contract().name());
        }).toList();
    }
}
