package io.acra.core.domain.authorization;

import io.acra.core.security.UniversalRedactor;
import java.time.Instant;
import java.util.List;

public record Delegation(
        String delegationId,
        String delegatorPrincipalId,
        String delegatePrincipalId,
        String sourceTenantId,
        String targetTenantId,
        String roleId,
        List<String> actions,
        AuthorizationScope scope,
        Instant validFrom,
        Instant validUntil,
        List<String> evidenceIds) {

    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public Delegation {
        delegationId = required(delegationId, "delegationId");
        delegatorPrincipalId = required(delegatorPrincipalId, "delegatorPrincipalId");
        delegatePrincipalId = required(delegatePrincipalId, "delegatePrincipalId");
        sourceTenantId = safe(sourceTenantId);
        targetTenantId = safe(targetTenantId);
        roleId = safe(roleId);
        actions = safeList(actions);
        scope = scope == null ? AuthorizationScope.tenant(targetTenantId) : scope;
        evidenceIds = safeList(evidenceIds);
        if (validFrom != null && validUntil != null && validUntil.isBefore(validFrom)) {
            throw new IllegalArgumentException("validUntil before validFrom");
        }
    }

    public boolean activeAt(Instant instant) {
        Instant at = instant == null ? Instant.now() : instant;
        return (validFrom == null || !at.isBefore(validFrom))
                && (validUntil == null || !at.isAfter(validUntil));
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
                .map(Delegation::safe).filter(v -> !v.isBlank()).distinct().sorted().toList();
    }
}
