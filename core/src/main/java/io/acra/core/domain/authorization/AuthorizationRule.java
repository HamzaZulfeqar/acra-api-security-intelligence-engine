package io.acra.core.domain.authorization;

import io.acra.core.security.UniversalRedactor;
import java.util.List;

public record AuthorizationRule(
        String ruleId,
        AuthorizationRuleEffect effect,
        String principalId,
        String roleId,
        String permissionId,
        String tenantId,
        AuthorizationScope scope,
        Integer precedence,
        String precedenceSource,
        List<String> evidenceIds) {

    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public AuthorizationRule {
        ruleId = required(ruleId, "ruleId");
        effect = effect == null ? AuthorizationRuleEffect.DENY : effect;
        principalId = safe(principalId);
        roleId = safe(roleId);
        permissionId = required(permissionId, "permissionId");
        tenantId = safe(tenantId);
        scope = scope == null ? new AuthorizationScope(AuthorizationScopeType.UNKNOWN, "", "", "", "", "") : scope;
        precedenceSource = safe(precedenceSource);
        evidenceIds = safeList(evidenceIds);
        if (principalId.isBlank() && roleId.isBlank()) {
            throw new IllegalArgumentException("principalId or roleId required");
        }
        if (precedence != null && precedence < 0) throw new IllegalArgumentException("precedence");
    }

    public boolean hasExplicitPrecedence() {
        return precedence != null && !precedenceSource.isBlank();
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
                .map(AuthorizationRule::safe).filter(v -> !v.isBlank()).distinct().sorted().toList();
    }
}
