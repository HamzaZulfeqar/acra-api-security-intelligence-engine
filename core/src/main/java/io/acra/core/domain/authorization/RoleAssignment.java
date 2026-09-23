package io.acra.core.domain.authorization;

import io.acra.core.security.UniversalRedactor;
import java.util.List;

public record RoleAssignment(
        String assignmentId,
        String principalId,
        String roleId,
        String tenantId,
        AuthorizationScope scope,
        boolean active,
        List<String> evidenceIds) {

    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public RoleAssignment {
        assignmentId = required(assignmentId, "assignmentId");
        principalId = required(principalId, "principalId");
        roleId = required(roleId, "roleId");
        tenantId = safe(tenantId);
        scope = scope == null ? AuthorizationScope.tenant(tenantId) : scope;
        evidenceIds = safeList(evidenceIds);
        if (scope.type() != AuthorizationScopeType.GLOBAL
                && scope.type() != AuthorizationScopeType.SHARED
                && tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId required for non-global role assignment");
        }
    }

    public boolean appliesToTenant(String tenant) {
        if (!active) return false;
        if (scope.type() == AuthorizationScopeType.GLOBAL || scope.type() == AuthorizationScopeType.SHARED) return true;
        return !tenantId.isBlank() && tenantId.equals(safe(tenant)) && scope.appliesToTenant(tenant);
    }

    private static String required(String value, String name) {
        String safe = safe(value);
        if (safe.isBlank()) throw new IllegalArgumentException(name + " required");
        return safe;
    }

    private static String safe(String value) {
        return REDACTOR.redactText(value == null ? "" : value);
    }

    private static List<String> safeList(List<String> values) {
        return List.copyOf(values == null ? List.<String>of() : values).stream()
                .map(RoleAssignment::safe).filter(v -> !v.isBlank()).distinct().sorted().toList();
    }
}
