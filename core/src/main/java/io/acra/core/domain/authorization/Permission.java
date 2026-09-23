package io.acra.core.domain.authorization;

import io.acra.core.security.UniversalRedactor;
import java.util.List;

public record Permission(
        String permissionId,
        String action,
        String resourceType,
        String endpoint,
        String property,
        AuthorizationScope scope,
        List<String> evidenceIds) {

    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public Permission {
        permissionId = required(permissionId, "permissionId");
        action = required(action, "action");
        resourceType = safe(resourceType);
        endpoint = safe(endpoint);
        property = safe(property);
        scope = scope == null ? new AuthorizationScope(AuthorizationScopeType.UNKNOWN, "", "", "", "", "") : scope;
        evidenceIds = safeList(evidenceIds);
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
                .map(Permission::safe).filter(v -> !v.isBlank()).distinct().sorted().toList();
    }
}
