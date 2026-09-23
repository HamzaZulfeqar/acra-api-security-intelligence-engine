package io.acra.core.engine;

import io.acra.core.domain.authorization.AuthorizationPolicySnapshot;
import io.acra.core.domain.authorization.AuthorizationScopeType;
import io.acra.core.domain.authorization.Delegation;
import io.acra.core.domain.authorization.EffectiveAuthorizationRequest;
import io.acra.core.domain.authorization.RoleAssignment;
import io.acra.core.domain.authorization.TenantMembership;
import io.acra.core.domain.authorization.TenantMembershipType;
import io.acra.core.domain.authorization.TenantRelationship;

public final class TenantRelationshipResolver {

    public TenantRelationship resolve(AuthorizationPolicySnapshot snapshot, EffectiveAuthorizationRequest request) {
        if (snapshot == null || request == null) return TenantRelationship.UNKNOWN;
        if (request.sharedResource()) return TenantRelationship.SHARED;
        if (request.subjectTenantId().isBlank() || request.resourceTenantId().isBlank()) {
            return TenantRelationship.UNKNOWN;
        }
        if (request.subjectTenantId().equals(request.resourceTenantId())) {
            return TenantRelationship.SAME_TENANT;
        }

        boolean globalMembership = snapshot.memberships().stream()
                .filter(TenantMembership::active)
                .filter(m -> m.principalId().equals(request.principalId()))
                .anyMatch(m -> m.type() == TenantMembershipType.GLOBAL);
        boolean globalRole = snapshot.roleAssignments().stream()
                .filter(RoleAssignment::active)
                .filter(r -> r.principalId().equals(request.principalId()))
                .anyMatch(r -> r.scope().type() == AuthorizationScopeType.GLOBAL);
        if (globalMembership || globalRole) return TenantRelationship.GLOBAL;

        boolean delegated = snapshot.delegations().stream()
                .filter(d -> d.delegatePrincipalId().equals(request.principalId()))
                .filter(d -> d.targetTenantId().equals(request.resourceTenantId()))
                .filter(d -> d.activeAt(request.evaluatedAt()))
                .anyMatch(d -> d.actions().isEmpty() || d.actions().contains(request.action()) || d.actions().contains("*"));
        if (delegated) return TenantRelationship.DELEGATED;
        return TenantRelationship.CROSS_TENANT;
    }
}
