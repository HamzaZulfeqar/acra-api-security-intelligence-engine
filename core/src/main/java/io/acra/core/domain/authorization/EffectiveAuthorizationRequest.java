package io.acra.core.domain.authorization;

import io.acra.core.security.UniversalRedactor;
import java.time.Instant;

public record EffectiveAuthorizationRequest(
        String principalId,
        String subjectTenantId,
        String resourceTenantId,
        String resourceId,
        String resourceType,
        String endpoint,
        String property,
        String action,
        AuthorizationDecision observedDecision,
        boolean sharedResource,
        Instant evaluatedAt) {

    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public EffectiveAuthorizationRequest {
        principalId = required(principalId, "principalId");
        subjectTenantId = safe(subjectTenantId);
        resourceTenantId = safe(resourceTenantId);
        resourceId = safe(resourceId);
        resourceType = safe(resourceType);
        endpoint = safe(endpoint);
        property = safe(property);
        action = required(action, "action");
        observedDecision = observedDecision == null ? AuthorizationDecision.UNKNOWN : observedDecision;
        evaluatedAt = evaluatedAt == null ? Instant.now() : evaluatedAt;
    }

    private static String required(String value, String name) {
        String safe = safe(value);
        if (safe.isBlank()) throw new IllegalArgumentException(name + " required");
        return safe;
    }

    private static String safe(String value) {
        return REDACTOR.redactText(value == null ? "" : value);
    }
}
