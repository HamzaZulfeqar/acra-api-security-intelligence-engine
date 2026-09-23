package io.acra.core.domain.authorization;

public enum TenantIsolationAssessmentState {
    NO_VIOLATION,
    CANDIDATE,
    INCONCLUSIVE,
    CONFLICTING,
    NOT_APPLICABLE
}
