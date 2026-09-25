package io.acra.core.domain.authorization;

import io.acra.core.security.UniversalRedactor;
import java.util.List;

public record RoleInheritance(
        String inheritanceId,
        String childRoleId,
        String parentRoleId,
        String tenantId,
        List<String> evidenceIds) {

    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public RoleInheritance {
        inheritanceId = required(inheritanceId, "inheritanceId");
        childRoleId = required(childRoleId, "childRoleId");
        parentRoleId = required(parentRoleId, "parentRoleId");
        tenantId = safe(tenantId);
        evidenceIds = safeList(evidenceIds);
        if (childRoleId.equals(parentRoleId)) throw new IllegalArgumentException("role cannot inherit itself");
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
                .map(RoleInheritance::safe).filter(v -> !v.isBlank()).distinct().sorted().toList();
    }
}
