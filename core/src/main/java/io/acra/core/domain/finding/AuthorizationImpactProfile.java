package io.acra.core.domain.finding;

import java.util.List;

public record AuthorizationImpactProfile(
        boolean confidentialityImpact,
        boolean integrityImpact,
        boolean availabilityImpact,
        boolean crossTenantImpact,
        boolean privilegedFunction,
        boolean bulkImpact,
        List<String> suppliedReasons) {

    public AuthorizationImpactProfile {
        suppliedReasons = List.copyOf(suppliedReasons == null ? List.of() : suppliedReasons);
    }

    public static AuthorizationImpactProfile none() {
        return new AuthorizationImpactProfile(false, false, false, false, false, false, List.of());
    }
}
