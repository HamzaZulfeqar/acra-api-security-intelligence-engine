package io.acra.core.domain.authorization;

import io.acra.core.security.UniversalRedactor;

public record AuthorizationScope(
        AuthorizationScopeType type,
        String tenantId,
        String resourceId,
        String endpoint,
        String function,
        String property) {

    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public AuthorizationScope {
        type = type == null ? AuthorizationScopeType.UNKNOWN : type;
        tenantId = safe(tenantId);
        resourceId = safe(resourceId);
        endpoint = safe(endpoint);
        function = safe(function);
        property = safe(property);
    }

    public static AuthorizationScope global() {
        return new AuthorizationScope(AuthorizationScopeType.GLOBAL, "", "", "", "", "");
    }

    public static AuthorizationScope tenant(String tenantId) {
        return new AuthorizationScope(AuthorizationScopeType.TENANT, tenantId, "", "", "", "");
    }

    public static AuthorizationScope shared() {
        return new AuthorizationScope(AuthorizationScopeType.SHARED, "", "", "", "", "");
    }

    public boolean appliesToTenant(String tenant) {
        if (type == AuthorizationScopeType.GLOBAL || type == AuthorizationScopeType.SHARED) return true;
        if (type == AuthorizationScopeType.TENANT || !tenantId.isBlank()) {
            return !tenantId.isBlank() && tenantId.equals(safe(tenant));
        }
        return true;
    }

    private static String safe(String value) {
        return REDACTOR.redactText(value == null ? "" : value);
    }
}
