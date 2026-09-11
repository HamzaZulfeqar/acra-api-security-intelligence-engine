package io.acra.core.active.model;

import io.acra.core.domain.common.Validation;
import java.time.Instant;
import java.util.List;

public record ReproducibilityMetadata(
        String engineVersion,
        long deterministicSeed,
        Instant createdAt,
        List<String> sourceReferences) {
    public ReproducibilityMetadata {
        engineVersion = Validation.requireNonBlank(engineVersion, "engineVersion");
        if (createdAt == null) throw new IllegalArgumentException("createdAt required");
        sourceReferences = List.copyOf(sourceReferences == null ? List.of() : sourceReferences);
    }
}
