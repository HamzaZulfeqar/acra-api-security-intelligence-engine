package io.acra.core.domain.finding;

import java.util.List;

/** Explicit impact inputs used for deterministic severity; never inferred from endpoint test priority. */
public record AuthorizationImpact(
        ImpactLevel resourceImpact,
        boolean writeCapable,
        boolean destructive,
        boolean crossTenant,
        boolean privilegedFunction,
        boolean sensitiveProperty,
        boolean bulkExposure,
        List<String> evidenceIds) {
    public AuthorizationImpact {
        resourceImpact = resourceImpact == null ? ImpactLevel.UNKNOWN : resourceImpact;
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
    }
}
