package io.acra.core.active.safety;

import java.util.List;

public record ValidationDecision(ValidationStatus status, List<String> reasons) {
    public ValidationDecision {
        if (status == null) throw new IllegalArgumentException("validation status required");
        reasons = List.copyOf(reasons == null ? List.of() : reasons);
        if (status != ValidationStatus.ALLOWED && reasons.isEmpty()) {
            throw new IllegalArgumentException("non-allowed decision requires a reason");
        }
    }

    public static ValidationDecision allowed() {
        return new ValidationDecision(ValidationStatus.ALLOWED, List.of());
    }

    public static ValidationDecision blocked(String reason) {
        return new ValidationDecision(ValidationStatus.BLOCKED, List.of(reason));
    }

    public static ValidationDecision confirmation(String reason) {
        return new ValidationDecision(ValidationStatus.REQUIRES_CONFIRMATION, List.of(reason));
    }

    public static ValidationDecision invalid(String reason) {
        return new ValidationDecision(ValidationStatus.INVALID, List.of(reason));
    }
}
