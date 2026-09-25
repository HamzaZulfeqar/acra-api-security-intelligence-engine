package io.acra.core.batch;

import io.acra.core.security.UniversalRedactor;

public record BatchItemProjectionSpec(String itemKey, String resourceId, String action) {
    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public BatchItemProjectionSpec {
        itemKey = requiredSafe(itemKey, "itemKey");
        resourceId = requiredSafe(resourceId, "resourceId");
        action = requiredSafe(action, "action");
    }

    private static String requiredSafe(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " required");
        String normalized = value.strip();
        if (!normalized.equals(REDACTOR.redactText(normalized))) {
            throw new IllegalArgumentException(name + " contains sensitive material");
        }
        return normalized;
    }
}
