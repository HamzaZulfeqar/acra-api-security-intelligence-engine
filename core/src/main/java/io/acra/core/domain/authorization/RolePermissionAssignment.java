package io.acra.core.domain.authorization;

import io.acra.core.security.UniversalRedactor;
import java.util.List;

public record RolePermissionAssignment(
        String assignmentId,
        String roleId,
        String permissionId,
        String tenantId,
        List<String> evidenceIds) {

    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public RolePermissionAssignment {
        assignmentId = required(assignmentId, "assignmentId");
        roleId = required(roleId, "roleId");
        permissionId = required(permissionId, "permissionId");
        tenantId = safe(tenantId);
        evidenceIds = safeList(evidenceIds);
    }

    public boolean appliesToTenant(String tenant) {
        return tenantId.isBlank() || tenantId.equals(safe(tenant));
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
                .map(RolePermissionAssignment::safe).filter(v -> !v.isBlank()).distinct().sorted().toList();
    }
}
