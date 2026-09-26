package io.acra.standalone.model;

import io.acra.core.domain.common.Confidence;
import io.acra.core.domain.identity.AuthenticationType;
import io.acra.core.domain.identity.Principal;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PrincipalContextRecord(
        UUID id,
        UUID projectId,
        String principalId,
        String displayName,
        AuthenticationType authenticationType,
        Instant createdAt
) {
    public PrincipalContextRecord {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(projectId, "projectId");
        principalId = required(principalId, "principalId", 160);
        displayName = normalize(displayName, 240);
        authenticationType = authenticationType == null ? AuthenticationType.UNKNOWN : authenticationType;
        Objects.requireNonNull(createdAt, "createdAt");
        new Principal(principalId, displayName, authenticationType, Confidence.unknown());
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
