package io.acra.core.active.model;

import io.acra.core.domain.common.Validation;

public record Mutation(
        String mutationId,
        MutationType type,
        MutationLocation targetLocation,
        String originalValue,
        String mutatedValue,
        String sourceContext,
        String targetContext,
        String rationale,
        String expectedSecurityEffect,
        SafetyClass safetyClass,
        String deduplicationKey) {
    public Mutation {
        mutationId = Validation.requireNonBlank(mutationId, "mutationId");
        if (type == null) throw new IllegalArgumentException("mutation type required");
        if (targetLocation == null) throw new IllegalArgumentException("mutation location required");
        originalValue = originalValue == null ? "" : originalValue;
        mutatedValue = mutatedValue == null ? "" : mutatedValue;
        if (originalValue.equals(mutatedValue)) throw new IllegalArgumentException("mutation must change one value");
        sourceContext = sourceContext == null ? "UNKNOWN" : sourceContext;
        targetContext = targetContext == null ? "UNKNOWN" : targetContext;
        rationale = Validation.requireNonBlank(rationale, "mutation rationale");
        expectedSecurityEffect = expectedSecurityEffect == null ? "UNKNOWN" : expectedSecurityEffect;
        if (safetyClass == null) safetyClass = SafetyClass.SAFE_READ_ONLY;
        deduplicationKey = Validation.requireNonBlank(deduplicationKey, "deduplicationKey");
    }
}
