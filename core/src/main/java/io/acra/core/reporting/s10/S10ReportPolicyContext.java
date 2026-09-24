package io.acra.core.reporting.s10;

import io.acra.core.coverage.S10CoverageFamily;
import io.acra.core.domain.authorization.AuthorizationDecision;
import java.util.List;

public record S10ReportPolicyContext(
        S10CoverageFamily family,
        String policyReference,
        String endpoint,
        String resourceId,
        String action,
        String roleId,
        String tenantId,
        AuthorizationDecision expectedDecision,
        List<String> evidenceIds) {

    public S10ReportPolicyContext {
        if (family == null) throw new IllegalArgumentException("family required");
        policyReference = required(policyReference, "policyReference");
        endpoint = required(endpoint, "endpoint");
        resourceId = required(resourceId, "resourceId");
        action = required(action, "action");
        roleId = normalized(roleId);
        tenantId = normalized(tenantId);
        expectedDecision = expectedDecision == null ? AuthorizationDecision.UNKNOWN : expectedDecision;
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " required");
        return value.strip();
    }

    private static String normalized(String value) {
        return value == null || value.isBlank() ? "UNKNOWN" : value.strip();
    }
}
