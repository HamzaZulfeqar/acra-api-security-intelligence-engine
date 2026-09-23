package io.acra.core.domain.authorization;

import java.util.List;

public record EffectiveAuthorizationMatrixEntry(
        String resolutionId,
        String principalId,
        List<String> effectiveRoleIds,
        String subjectTenantId,
        String resourceTenantId,
        TenantRelationship tenantRelationship,
        String resourceId,
        String action,
        String endpoint,
        String policyFingerprint,
        AuthorizationDecision expectedDecision,
        AuthorizationDecision observedDecision,
        PolicyResolutionState state,
        List<String> evidenceIds) {

    public EffectiveAuthorizationMatrixEntry {
        effectiveRoleIds = List.copyOf(effectiveRoleIds == null ? List.of() : effectiveRoleIds);
        tenantRelationship = tenantRelationship == null ? TenantRelationship.UNKNOWN : tenantRelationship;
        expectedDecision = expectedDecision == null ? AuthorizationDecision.UNKNOWN : expectedDecision;
        observedDecision = observedDecision == null ? AuthorizationDecision.UNKNOWN : observedDecision;
        state = state == null ? PolicyResolutionState.UNKNOWN : state;
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
    }

    public static EffectiveAuthorizationMatrixEntry from(EffectiveAuthorizationRequest request,
                                                         EffectiveAuthorizationResolution resolution) {
        return new EffectiveAuthorizationMatrixEntry(resolution.resolutionId(), request.principalId(),
                resolution.effectiveRoleIds(), request.subjectTenantId(), request.resourceTenantId(),
                resolution.tenantRelationship(), request.resourceId(), request.action(), request.endpoint(),
                resolution.policyFingerprint(), resolution.expectedDecision(), resolution.observedDecision(),
                resolution.state(), resolution.evidenceIds());
    }
}
