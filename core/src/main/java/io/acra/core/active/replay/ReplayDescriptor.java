package io.acra.core.active.replay;

import io.acra.core.active.model.SecurityTest;
import io.acra.core.domain.common.Validation;
import java.time.Instant;

public record ReplayDescriptor(
        String originalExecutionId,
        SecurityTest test,
        String testSignature,
        String configurationFingerprint,
        String environmentFingerprint,
        Instant createdAt) {
    public ReplayDescriptor {
        originalExecutionId = Validation.requireNonBlank(originalExecutionId, "originalExecutionId");
        if (test == null || createdAt == null) throw new IllegalArgumentException("replay test/time required");
        testSignature = Validation.requireNonBlank(testSignature, "testSignature");
        configurationFingerprint = Validation.requireNonBlank(configurationFingerprint, "configurationFingerprint");
        environmentFingerprint = Validation.requireNonBlank(environmentFingerprint, "environmentFingerprint");
    }
}
