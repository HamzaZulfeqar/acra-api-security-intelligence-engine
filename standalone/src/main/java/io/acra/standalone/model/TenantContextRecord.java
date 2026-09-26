package io.acra.standalone.model;

import io.acra.core.domain.common.Confidence;
import io.acra.core.domain.evidence.EvidenceSource;
import io.acra.core.domain.tenant.Tenant;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TenantContextRecord(
        UUID id,
        UUID projectId,
        String tenantId,
        String name,
        Instant createdAt
) {
    public TenantContextRecord {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(projectId, "projectId");
        tenantId = required(tenantId, "tenantId", 160);
        name = normalize(name, 240);
        Objects.requireNonNull(createdAt, "createdAt");
        new Tenant(tenantId, name, EvidenceSource.USER_POLICY, Confidence.unknown());
    }

    private static String required(String value, String field, int max) {
        String normalized = normalize(value, max);
        if (normalized.isBlank()) throw new IllegalArgumentException(field + " is required");
        return normalized;
    }

    private static String normalize(String value, int max) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.length() > max) throw new IllegalArgumentException(field + " exceeds " + max + " characters");
        return normalized;
    }
}
