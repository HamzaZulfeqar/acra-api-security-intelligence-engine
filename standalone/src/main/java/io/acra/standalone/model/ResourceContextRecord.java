package io.acra.standalone.model;

import io.acra.core.domain.common.Confidence;
import io.acra.core.domain.resource.Resource;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ResourceContextRecord(
        UUID id,
        UUID projectId,
        String resourceId,
        String resourceType,
        String ownerPrincipalId,
        String tenantId,
        String state,
        Instant createdAt
) {
    public ResourceContextRecord {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(projectId, "projectId");
        resourceId = required(resourceId, "resourceId", 180);
        resourceType = required(resourceType, "resourceType", 120);
        ownerPrincipalId = normalize(ownerPrincipalId, 160);
        tenantId = normalize(tenantId, 160);
        state = normalize(state, 120);
        Objects.requireNonNull(createdAt, "createdAt");
        new Resource(resourceId, resourceType, "", ownerPrincipalId, tenantId, state, Confidence.unknown());
    }

    private static String required(String value, String field, int max) {
        String normalized = normalize(value, max);
        if (normalized.isBlank()) throw new IllegalArgumentException(field + " is required");
        return normalized;
    }

    private static String normalize(String value, int max) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.length() > max) throw new IllegalArgumentException("value exceeds " + max + " characters");
        return normalized;
    }
}
