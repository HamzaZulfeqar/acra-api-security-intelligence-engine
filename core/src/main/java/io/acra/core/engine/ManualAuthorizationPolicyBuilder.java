package io.acra.core.engine;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.AuthorizationPolicySnapshot;
import io.acra.core.domain.authorization.AuthorizationRule;
import io.acra.core.domain.authorization.Delegation;
import io.acra.core.domain.authorization.Permission;
import io.acra.core.domain.authorization.RoleAssignment;
import io.acra.core.domain.authorization.RoleInheritance;
import io.acra.core.domain.authorization.RolePermissionAssignment;
import io.acra.core.domain.authorization.TenantMembership;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class ManualAuthorizationPolicyBuilder {
    private final String policyId;
    private String version = "";
    private String source = "manual";
    private AuthorizationDecision defaultDecision = AuthorizationDecision.UNKNOWN;
    private final List<TenantMembership> memberships = new ArrayList<>();
    private final List<RoleAssignment> roleAssignments = new ArrayList<>();
    private final List<RoleInheritance> inheritances = new ArrayList<>();
    private final List<Permission> permissions = new ArrayList<>();
    private final List<RolePermissionAssignment> rolePermissions = new ArrayList<>();
    private final List<AuthorizationRule> rules = new ArrayList<>();
    private final List<Delegation> delegations = new ArrayList<>();
    private final List<String> evidenceIds = new ArrayList<>();

    public ManualAuthorizationPolicyBuilder(String policyId) {
        if (policyId == null || policyId.isBlank()) throw new IllegalArgumentException("policyId required");
        this.policyId = policyId;
    }

    public ManualAuthorizationPolicyBuilder version(String value) { this.version = value == null ? "" : value; return this; }
    public ManualAuthorizationPolicyBuilder source(String value) { this.source = value == null ? "manual" : value; return this; }
    public ManualAuthorizationPolicyBuilder defaultDecision(AuthorizationDecision value) { this.defaultDecision = value; return this; }
    public ManualAuthorizationPolicyBuilder membership(TenantMembership value) { memberships.add(value); return this; }
    public ManualAuthorizationPolicyBuilder role(RoleAssignment value) { roleAssignments.add(value); return this; }
    public ManualAuthorizationPolicyBuilder inheritance(RoleInheritance value) { inheritances.add(value); return this; }
    public ManualAuthorizationPolicyBuilder permission(Permission value) { permissions.add(value); return this; }
    public ManualAuthorizationPolicyBuilder rolePermission(RolePermissionAssignment value) { rolePermissions.add(value); return this; }
    public ManualAuthorizationPolicyBuilder rule(AuthorizationRule value) { rules.add(value); return this; }
    public ManualAuthorizationPolicyBuilder delegation(Delegation value) { delegations.add(value); return this; }
    public ManualAuthorizationPolicyBuilder evidence(String evidenceId) { if (evidenceId != null) evidenceIds.add(evidenceId); return this; }

    public AuthorizationPolicySnapshot build(Instant capturedAt) {
        return AuthorizationPolicySnapshot.create(policyId, version, source, memberships, roleAssignments,
                inheritances, permissions, rolePermissions, rules, delegations, evidenceIds,
                defaultDecision, capturedAt);
    }
}
