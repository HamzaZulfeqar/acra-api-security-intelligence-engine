package io.acra.core.engine;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.AuthorizationPolicySnapshot;
import io.acra.core.domain.authorization.AuthorizationRule;
import io.acra.core.domain.authorization.AuthorizationRuleEffect;
import io.acra.core.domain.authorization.AuthorizationScope;
import io.acra.core.domain.authorization.AuthorizationScopeType;
import io.acra.core.domain.authorization.Delegation;
import io.acra.core.domain.authorization.EffectiveAuthorizationRequest;
import io.acra.core.domain.authorization.EffectiveAuthorizationResolution;
import io.acra.core.domain.authorization.EffectiveRoleResolution;
import io.acra.core.domain.authorization.Permission;
import io.acra.core.domain.authorization.PolicyResolutionState;
import io.acra.core.domain.authorization.RoleInheritance;
import io.acra.core.domain.authorization.RolePermissionAssignment;
import io.acra.core.domain.authorization.RoleResolutionState;
import io.acra.core.domain.authorization.TenantRelationship;
import io.acra.core.security.TokenFingerprint;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class EffectiveAuthorizationResolver {
    private final RoleHierarchyResolver roleResolver = new RoleHierarchyResolver();
    private final TenantRelationshipResolver tenantResolver = new TenantRelationshipResolver();

    public EffectiveAuthorizationResolution resolve(AuthorizationPolicySnapshot snapshot,
                                                    EffectiveAuthorizationRequest request) {
        if (snapshot == null) throw new IllegalArgumentException("snapshot required");
        if (request == null) throw new IllegalArgumentException("request required");

        TenantRelationship relationship = tenantResolver.resolve(snapshot, request);
        EffectiveRoleResolution baseRoles = roleResolver.resolve(
                snapshot, request.principalId(), request.resourceTenantId());

        Set<String> effectiveRoles = new TreeSet<>(baseRoles.effectiveRoleIds());
        Set<String> delegationIds = new TreeSet<>();
        Set<String> evidence = new TreeSet<>();
        List<String> reasons = new ArrayList<>();

        for (Delegation delegation : snapshot.delegations()) {
            if (!delegation.delegatePrincipalId().equals(request.principalId())) continue;
            if (!delegation.targetTenantId().equals(request.resourceTenantId())) continue;
            if (!delegation.activeAt(request.evaluatedAt())) continue;
            if (!delegation.actions().isEmpty()
                    && !delegation.actions().contains(request.action())
                    && !delegation.actions().contains("*")) continue;
            if (!scopeMatches(delegation.scope(), request)) continue;
            delegationIds.add(delegation.delegationId());
            evidence.addAll(delegation.evidenceIds());
            if (!delegation.roleId().isBlank()) effectiveRoles.add(delegation.roleId());
        }

        if (!delegationIds.isEmpty()) {
            EffectiveRoleResolution delegated = roleResolver.expand(
                    snapshot, request.principalId(), request.resourceTenantId(), effectiveRoles);
            if (delegated.state() == RoleResolutionState.CONFLICTING) {
                reasons.addAll(delegated.reasons());
                return result(snapshot, request, relationship, effectiveRoles, Set.of(), Set.of(), delegationIds,
                        AuthorizationDecision.UNKNOWN, PolicyResolutionState.CONFLICTING, evidence, reasons);
            }
            effectiveRoles.addAll(delegated.effectiveRoleIds());
        }

        if (baseRoles.state() == RoleResolutionState.CONFLICTING) {
            reasons.addAll(baseRoles.reasons());
            return result(snapshot, request, relationship, effectiveRoles, Set.of(), Set.of(), delegationIds,
                    AuthorizationDecision.UNKNOWN, PolicyResolutionState.CONFLICTING, evidence, reasons);
        }

        Map<String, Permission> permissionsById = new HashMap<>();
        for (Permission permission : snapshot.permissions()) permissionsById.put(permission.permissionId(), permission);

        Set<String> matchedPermissions = new TreeSet<>();
        for (RolePermissionAssignment assignment : snapshot.rolePermissionAssignments()) {
            if (!effectiveRoles.contains(assignment.roleId())) continue;
            if (!assignment.appliesToTenant(request.resourceTenantId())) continue;
            Permission permission = permissionsById.get(assignment.permissionId());
            if (permission != null && permissionMatches(permission, request)) {
                matchedPermissions.add(permission.permissionId());
                evidence.addAll(assignment.evidenceIds());
                evidence.addAll(permission.evidenceIds());
            }
        }

        List<AuthorizationRule> matchedRules = snapshot.rules().stream()
                .filter(rule -> ruleMatches(rule, request, effectiveRoles, matchedPermissions))
                .sorted(Comparator.comparing(AuthorizationRule::ruleId))
                .toList();
        Set<String> ruleIds = new TreeSet<>();
        matchedRules.forEach(rule -> {
            ruleIds.add(rule.ruleId());
            evidence.addAll(rule.evidenceIds());
        });

        Decision decision = decide(matchedRules, !matchedPermissions.isEmpty(), snapshot.defaultDecision());
        reasons.addAll(decision.reasons());
        return result(snapshot, request, relationship, effectiveRoles, matchedPermissions, ruleIds, delegationIds,
                decision.expected(), decision.state(), evidence, reasons);
    }

    private Decision decide(List<AuthorizationRule> rules, boolean permissionGrant,
                            AuthorizationDecision defaultDecision) {
        List<String> reasons = new ArrayList<>();
        List<AuthorizationRule> explicit = rules.stream().filter(AuthorizationRule::hasExplicitPrecedence).toList();
        if (!explicit.isEmpty()) {
            int max = explicit.stream().map(AuthorizationRule::precedence).max(Integer::compareTo).orElse(0);
            Set<AuthorizationRuleEffect> effects = new HashSet<>();
            explicit.stream().filter(r -> r.precedence() == max).forEach(r -> effects.add(r.effect()));
            if (effects.size() > 1) {
                reasons.add("SAME_PRECEDENCE_ALLOW_DENY_CONFLICT");
                return new Decision(AuthorizationDecision.UNKNOWN, PolicyResolutionState.CONFLICTING, reasons);
            }
            AuthorizationRuleEffect effect = effects.iterator().next();
            reasons.add("EXPLICIT_PRECEDENCE:" + max);
            return effect == AuthorizationRuleEffect.DENY
                    ? new Decision(AuthorizationDecision.DENY, PolicyResolutionState.RESOLVED_DENY, reasons)
                    : new Decision(AuthorizationDecision.ALLOW, PolicyResolutionState.RESOLVED_ALLOW, reasons);
        }

        boolean allow = rules.stream().anyMatch(r -> r.effect() == AuthorizationRuleEffect.ALLOW);
        boolean deny = rules.stream().anyMatch(r -> r.effect() == AuthorizationRuleEffect.DENY);
        if (allow && deny) {
            reasons.add("ALLOW_DENY_CONFLICT_WITHOUT_PROVEN_PRECEDENCE");
            return new Decision(AuthorizationDecision.UNKNOWN, PolicyResolutionState.CONFLICTING, reasons);
        }
        if (deny) return new Decision(AuthorizationDecision.DENY, PolicyResolutionState.RESOLVED_DENY, reasons);
        if (allow || permissionGrant) {
            return new Decision(AuthorizationDecision.ALLOW, PolicyResolutionState.RESOLVED_ALLOW, reasons);
        }
        if (defaultDecision == AuthorizationDecision.DENY) {
            reasons.add("EXPLICIT_DEFAULT_DENY");
            return new Decision(AuthorizationDecision.DENY, PolicyResolutionState.RESOLVED_DENY, reasons);
        }
        if (defaultDecision == AuthorizationDecision.ALLOW) {
            reasons.add("EXPLICIT_DEFAULT_ALLOW");
            return new Decision(AuthorizationDecision.ALLOW, PolicyResolutionState.RESOLVED_ALLOW, reasons);
        }
        reasons.add("NO_MATCHING_PERMISSION_OR_RULE");
        return new Decision(AuthorizationDecision.UNKNOWN, PolicyResolutionState.INCOMPLETE, reasons);
    }

    private boolean ruleMatches(AuthorizationRule rule, EffectiveAuthorizationRequest request,
                                Set<String> roles, Set<String> permissionIds) {
        if (!rule.principalId().isBlank() && !rule.principalId().equals(request.principalId())) return false;
        if (!rule.roleId().isBlank() && !roles.contains(rule.roleId())) return false;
        if (!rule.permissionId().equals("*") && !permissionIds.contains(rule.permissionId())) return false;
        if (!rule.tenantId().isBlank() && !rule.tenantId().equals(request.resourceTenantId())) return false;
        return scopeMatches(rule.scope(), request);
    }

    private boolean permissionMatches(Permission permission, EffectiveAuthorizationRequest request) {
        if (!permission.action().equals("*") && !permission.action().equals(request.action())) return false;
        if (!permission.resourceType().isBlank() && !permission.resourceType().equals("*")
                && !permission.resourceType().equals(request.resourceType())) return false;
        if (!permission.endpoint().isBlank() && !permission.endpoint().equals("*")
                && !permission.endpoint().equals(request.endpoint())) return false;
        if (!permission.property().isBlank() && !permission.property().equals("*")
                && !permission.property().equals(request.property())) return false;
        return scopeMatches(permission.scope(), request);
    }

    private boolean scopeMatches(AuthorizationScope scope, EffectiveAuthorizationRequest request) {
        if (scope == null || scope.type() == AuthorizationScopeType.UNKNOWN) return true;
        if (!scope.appliesToTenant(request.resourceTenantId())) return false;
        if (!scope.resourceId().isBlank() && !scope.resourceId().equals(request.resourceId())) return false;
        if (!scope.endpoint().isBlank() && !scope.endpoint().equals(request.endpoint())) return false;
        if (!scope.function().isBlank() && !scope.function().equals(request.action())) return false;
        return scope.property().isBlank() || scope.property().equals(request.property());
    }

    private EffectiveAuthorizationResolution result(AuthorizationPolicySnapshot snapshot,
            EffectiveAuthorizationRequest request, TenantRelationship relationship, Set<String> roles,
            Set<String> permissions, Set<String> rules, Set<String> delegations, AuthorizationDecision expected,
            PolicyResolutionState state, Set<String> evidence, List<String> reasons) {
        String material = snapshot.fingerprint() + "|" + request.principalId() + "|" + request.resourceTenantId()
                + "|" + request.resourceId() + "|" + request.endpoint() + "|" + request.property() + "|"
                + request.action() + "|" + expected + "|" + state;
        return new EffectiveAuthorizationResolution(
                "s6-resolution-" + TokenFingerprint.sha256(material).substring(0, 24),
                snapshot.fingerprint(), request.principalId(), request.resourceTenantId(), relationship,
                List.copyOf(roles), List.copyOf(permissions), List.copyOf(rules), List.copyOf(delegations),
                expected, request.observedDecision(), state, List.copyOf(evidence), reasons);
    }

    private record Decision(AuthorizationDecision expected, PolicyResolutionState state, List<String> reasons) { }
}
