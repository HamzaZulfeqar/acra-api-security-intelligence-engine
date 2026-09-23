package io.acra.core.active.planning;

import io.acra.core.active.model.TestContract;

public record S6PolicyPlanningRecommendation(
        TestContract contract,
        boolean recommended,
        int priorityBoost,
        String policyFingerprint,
        String reason) {

    public S6PolicyPlanningRecommendation {
        if (contract == null) throw new IllegalArgumentException("contract required");
        if (priorityBoost < 0 || priorityBoost > 100) throw new IllegalArgumentException("priorityBoost");
        policyFingerprint = policyFingerprint == null ? "" : policyFingerprint;
        reason = reason == null ? "" : reason;
    }
}
