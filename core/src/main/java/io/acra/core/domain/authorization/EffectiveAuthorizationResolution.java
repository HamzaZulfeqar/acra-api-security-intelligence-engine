package io.acra.core.domain.authorization;

import java.util.List;

public record EffectiveAuthorizationResolution(
        String resolutionId,
        String policyFingerprint,
        String principalId,
        String resourceTenantId,
        TenantRelationship tenantRelationship,
        List<String> effectiveRoleIds,
        List<String> permissionIds,
        List<String> matchedRuleIds,
        List<String> delegationIds,
        AuthorizationDecision expectedDecision,
        AuthorizationDecision observedDecision,
        PolicyResolutionState state,
        List<String> evidenceIds,
        List<String> reasons) {

    public EffectiveAuthorizationResolution {
        tenantRelationship = tenantRelationship == null ? TenantRelationship.UNKNOWN : tenantRelationship;
        effectiveRoleIds = sorted(effectiveRoleIds);
        permissionIds = sorted(permissionIds);
        matchedRuleIds = sorted(matchedRuleIds);
        delegationIds = sorted(delegationIds);
        expectedDecision = expectedDecision == null ? AuthorizationDecision.UNKNOWN : expectedDecision;
        observedDecision = observedDecision == null ? AuthorizationDecision.UNKNOWN : observedDecision;
        state = state == null ? PolicyResolutionState.UNKNOWN : state;
        evidenceIds = sorted(evidenceIds);
        reasons = sorted(reasons);
    }

    public boolean mismatchCandidate() {
        return expectedDecision == AuthorizationDecision.DENY
                && observedDecision == AuthorizationDecision.ALLOW
                && state == PolicyResolutionState.RESOLVED_DENY;
    }

    private static List<String> sorted(List<String> values) {
        return List.copyOf(values == null ? List.<String>of() : values).stream()
                .filter(v -> v != null && !v.isBlank()).distinct().sorted().toList();
    }
}
