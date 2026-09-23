package io.acra.core.engine;

import io.acra.core.domain.authorization.AuthorizationPolicySnapshot;
import io.acra.core.domain.authorization.EffectiveRoleResolution;
import io.acra.core.domain.authorization.RoleAssignment;
import io.acra.core.domain.authorization.RoleInheritance;
import io.acra.core.domain.authorization.RoleResolutionState;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class RoleHierarchyResolver {

    public EffectiveRoleResolution resolve(AuthorizationPolicySnapshot snapshot, String principalId, String tenantId) {
        if (snapshot == null) throw new IllegalArgumentException("snapshot required");
        String principal = principalId == null ? "" : principalId.strip();
        String tenant = tenantId == null ? "" : tenantId.strip();
        if (principal.isBlank()) {
            return new EffectiveRoleResolution("", tenant, List.of(), List.of(), List.of(),
                    RoleResolutionState.UNKNOWN, List.of("PRINCIPAL_REQUIRED"));
        }

        Set<String> direct = new TreeSet<>();
        for (RoleAssignment assignment : snapshot.roleAssignments()) {
            if (assignment.active() && assignment.principalId().equals(principal) && assignment.appliesToTenant(tenant)) {
                direct.add(assignment.roleId());
            }
        }
        if (direct.isEmpty()) {
            return new EffectiveRoleResolution(principal, tenant, List.of(), List.of(), List.of(),
                    RoleResolutionState.INCOMPLETE, List.of("NO_APPLICABLE_ROLE_ASSIGNMENT"));
        }

        Map<String, List<String>> parents = new HashMap<>();
        for (RoleInheritance edge : snapshot.roleInheritances()) {
            if (edge.appliesToTenant(tenant)) {
                parents.computeIfAbsent(edge.childRoleId(), ignored -> new ArrayList<>()).add(edge.parentRoleId());
            }
        }

        Set<String> inherited = new TreeSet<>();
        Set<String> effective = new TreeSet<>(direct);
        List<String> reasons = new ArrayList<>();
        for (String role : direct) {
            if (!walk(role, parents, inherited, effective, new HashSet<>(), new ArrayDeque<>(), reasons)) {
                return new EffectiveRoleResolution(principal, tenant, List.copyOf(direct), List.copyOf(inherited),
                        List.copyOf(effective), RoleResolutionState.CONFLICTING, reasons);
            }
        }
        inherited.removeAll(direct);
        return new EffectiveRoleResolution(principal, tenant, List.copyOf(direct), List.copyOf(inherited),
                List.copyOf(effective), RoleResolutionState.RESOLVED, reasons);
    }

    private boolean walk(String role, Map<String, List<String>> parents, Set<String> inherited,
                         Set<String> effective, Set<String> visiting, Deque<String> path, List<String> reasons) {
        if (!visiting.add(role)) {
            path.addLast(role);
            reasons.add("ROLE_HIERARCHY_CYCLE:" + String.join("->", path));
            path.removeLast();
            return false;
        }
        path.addLast(role);
        for (String parent : parents.getOrDefault(role, List.of()).stream().sorted().toList()) {
            effective.add(parent);
            inherited.add(parent);
            if (!walk(parent, parents, inherited, effective, visiting, path, reasons)) return false;
        }
        path.removeLast();
        visiting.remove(role);
        return true;
    }
}
