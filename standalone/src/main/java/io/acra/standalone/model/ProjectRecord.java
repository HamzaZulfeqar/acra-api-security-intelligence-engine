package io.acra.standalone.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ProjectRecord(
        UUID id,
        String name,
        String description,
        Instant createdAt
) {
    public ProjectRecord {
        Objects.requireNonNull(id, "id");
        name = requireText(name, "name", 120);
        description = normalize(description, 1000);
        Objects.requireNonNull(createdAt, "createdAt");
    }

    private static String requireText(String value, String field, int max) {
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
