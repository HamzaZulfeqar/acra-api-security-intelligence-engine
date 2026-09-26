package io.acra.standalone.model;

import io.acra.core.domain.common.Confidence;
import io.acra.core.domain.evidence.EvidenceSource;
import io.acra.core.domain.identity.Role;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record RoleContextRecord(
        UUID id,
        UUID projectId,
        String roleId,
        String name,
        Instant createdAt
) {
    public RoleContextRecord {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(projectId, "projectId");
        roleId = required(roleId, "roleId", 160);
        name = required(name, "name", 240);
        Objects.requireNonNull(createdAt, "createdAt");
        new Role(roleId, name, EvidenceSource.USER_POLICY, Confidence.unknown());
    }

    private static String required(String value, String field, int max) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) throw new IllegalArgumentException(field + " is required");
        if (normalized.length() > max) throw new IllegalArgumentException(field + " exceeds " + max + " characters");
        return normalized;
    }
}
