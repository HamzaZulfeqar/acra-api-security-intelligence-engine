package io.acra.core.domain.authorization;

import io.acra.core.security.TokenFingerprint;
import io.acra.core.security.UniversalRedactor;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public record AuthorizationPolicySnapshot(
        String policyId,
        String version,
        String source,
        List<TenantMembership> memberships,
        List<RoleAssignment> roleAssignments,
        List<RoleInheritance> roleInheritances,
        List<Permission> permissions,
        List<RolePermissionAssignment> rolePermissionAssignments,
        List<AuthorizationRule> rules,
        List<Delegation> delegations,
        List<String> evidenceIds,
        AuthorizationDecision defaultDecision,
        Instant capturedAt,
        String fingerprint) {

    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public AuthorizationPolicySnapshot {
        policyId = required(policyId, "policyId");
        version = safe(version);
        source = safe(source);
        memberships = sort(memberships, Comparator.comparing(TenantMembership::membershipId));
        roleAssignments = sort(roleAssignments, Comparator.comparing(RoleAssignment::assignmentId));
        roleInheritances = sort(roleInheritances, Comparator.comparing(RoleInheritance::inheritanceId));
        permissions = sort(permissions, Comparator.comparing(Permission::permissionId));
        rolePermissionAssignments = sort(rolePermissionAssignments,
                Comparator.comparing(RolePermissionAssignment::assignmentId));
        rules = sort(rules, Comparator.comparing(AuthorizationRule::ruleId));
        delegations = sort(delegations, Comparator.comparing(Delegation::delegationId));
        evidenceIds = List.copyOf(evidenceIds == null ? List.<String>of() : evidenceIds).stream()
                .map(AuthorizationPolicySnapshot::safe).filter(v -> !v.isBlank()).distinct().sorted().toList();
        defaultDecision = defaultDecision == null ? AuthorizationDecision.UNKNOWN : defaultDecision;
        if (capturedAt == null) throw new IllegalArgumentException("capturedAt required");
        String computed = computeFingerprint(policyId, version, source, memberships, roleAssignments,
                roleInheritances, permissions, rolePermissionAssignments, rules, delegations, evidenceIds, defaultDecision);
        fingerprint = fingerprint == null || fingerprint.isBlank() ? computed : safe(fingerprint);
        if (!fingerprint.equals(computed)) throw new IllegalArgumentException("policy fingerprint mismatch");
    }

    public static AuthorizationPolicySnapshot create(String policyId, String version, String source,
            List<TenantMembership> memberships, List<RoleAssignment> roleAssignments,
            List<RoleInheritance> roleInheritances, List<Permission> permissions,
            List<RolePermissionAssignment> rolePermissionAssignments, List<AuthorizationRule> rules,
            List<Delegation> delegations, List<String> evidenceIds, Instant capturedAt) {
        return create(policyId, version, source, memberships, roleAssignments, roleInheritances, permissions,
                rolePermissionAssignments, rules, delegations, evidenceIds, AuthorizationDecision.UNKNOWN, capturedAt);
    }

    public static AuthorizationPolicySnapshot create(String policyId, String version, String source,
            List<TenantMembership> memberships, List<RoleAssignment> roleAssignments,
            List<RoleInheritance> roleInheritances, List<Permission> permissions,
            List<RolePermissionAssignment> rolePermissionAssignments, List<AuthorizationRule> rules,
            List<Delegation> delegations, List<String> evidenceIds, AuthorizationDecision defaultDecision,
            Instant capturedAt) {
        return new AuthorizationPolicySnapshot(policyId, version, source, memberships, roleAssignments,
                roleInheritances, permissions, rolePermissionAssignments, rules, delegations,
                evidenceIds, defaultDecision, capturedAt, "");
    }

    private static String computeFingerprint(String policyId, String version, String source,
            List<TenantMembership> memberships, List<RoleAssignment> roleAssignments,
            List<RoleInheritance> roleInheritances, List<Permission> permissions,
            List<RolePermissionAssignment> rolePermissionAssignments, List<AuthorizationRule> rules,
            List<Delegation> delegations, List<String> evidenceIds, AuthorizationDecision defaultDecision) {
        String material = policyId + "|" + version + "|" + source + "|" + memberships + "|" + roleAssignments
                + "|" + roleInheritances + "|" + permissions + "|" + rolePermissionAssignments + "|" + rules
                + "|" + delegations + "|" + evidenceIds + "|" + defaultDecision;
        return "policy-" + TokenFingerprint.sha256(material);
    }

    private static <T> List<T> sort(List<T> values, Comparator<T> comparator) {
        return List.copyOf(values == null ? List.<T>of() : values).stream().sorted(comparator).toList();
    }

    private static String required(String value, String name) {
        String safe = safe(value);
        if (safe.isBlank()) throw new IllegalArgumentException(name + " required");
        return safe;
    }

    private static String safe(String value) {
        return REDACTOR.redactText(value == null ? "" : value);
    }
}
