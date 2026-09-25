package io.acra.core.product.finding;

import io.acra.core.domain.finding.FindingLifecycleState;

public enum FindingGovernanceQueue {
    REVIEW_REQUIRED,
    CONFIRMED,
    REMEDIATION,
    RETEST,
    TERMINAL;

    public static FindingGovernanceQueue from(FindingLifecycleState state) {
        if (state == null) throw new IllegalArgumentException("state required");
        return switch (state) {
            case REVIEW_REQUIRED -> REVIEW_REQUIRED;
            case CONFIRMED -> CONFIRMED;
            case REMEDIATION_IN_PROGRESS -> REMEDIATION;
            case RETEST_REQUIRED -> RETEST;
            case FALSE_POSITIVE, ACCEPTED_RISK, RESOLVED, CLOSED -> TERMINAL;
        };
    }
}
