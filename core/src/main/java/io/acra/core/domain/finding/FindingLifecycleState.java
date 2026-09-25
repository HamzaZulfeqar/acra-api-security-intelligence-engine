package io.acra.core.domain.finding;

public enum FindingLifecycleState {
    REVIEW_REQUIRED,
    CONFIRMED,
    FALSE_POSITIVE,
    ACCEPTED_RISK,
    REMEDIATION_IN_PROGRESS,
    RETEST_REQUIRED,
    RESOLVED,
    CLOSED
}
