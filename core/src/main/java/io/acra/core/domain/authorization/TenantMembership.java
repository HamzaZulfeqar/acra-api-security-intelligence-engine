package io.acra.core.domain.authorization;

import io.acra.core.security.UniversalRedactor;
import java.util.List;

public record TenantMembership(
        String membershipId,
        String principalId,
        String tenantId,
        TenantMembershipType type,
        boolean active,
        List<String> evidenceIds) {

    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public TenantMembership {
        membershipId = required(membershipId, "membershipId");
        principalId = required(principalId, "principalId");
        tenantId = safe(tenantId);
        type = type == null ? TenantMembershipType.UNKNOWN : type;
        evidenceIds = safeList(evidenceIds);
        if (type != TenantMembershipType.GLOBAL && type != TenantMembershipType.UNKNOWN && tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId required for tenant-bound membership");
        }
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
                .map(TenantMembership::safe).filter(v -> !v.isBlank()).distinct().sorted().toList();
    }
}
