package io.acra.core.active.model;

import io.acra.core.domain.common.Validation;

public record PlanSkip(String testId, String reason) {
    public PlanSkip {
        testId = Validation.requireNonBlank(testId, "testId");
        reason = Validation.requireNonBlank(reason, "skip reason");
    }
}
