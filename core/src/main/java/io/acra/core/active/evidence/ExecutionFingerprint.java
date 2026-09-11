package io.acra.core.active.evidence;

import io.acra.core.domain.common.Validation;

public record ExecutionFingerprint(
        String executionId,
        String testId,
        String requestFingerprint,
        String responseFingerprint,
        String configurationFingerprint,
        String environmentFingerprint) {
    public ExecutionFingerprint {
        executionId = Validation.requireNonBlank(executionId, "executionId");
        testId = Validation.requireNonBlank(testId, "testId");
        requestFingerprint = Validation.requireNonBlank(requestFingerprint, "requestFingerprint");
        responseFingerprint = Validation.requireNonBlank(responseFingerprint, "responseFingerprint");
        configurationFingerprint = Validation.requireNonBlank(configurationFingerprint, "configurationFingerprint");
        environmentFingerprint = Validation.requireNonBlank(environmentFingerprint, "environmentFingerprint");
    }
}
