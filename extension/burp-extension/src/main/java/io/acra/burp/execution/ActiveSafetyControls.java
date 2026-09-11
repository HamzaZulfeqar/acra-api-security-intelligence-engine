package io.acra.burp.execution;

import io.acra.burp.scope.ScopeConfiguration;

/** Explicit active-execution safety contract. Sprint 2 intentionally locks active budgets to zero. */
public record ActiveSafetyControls(
        int maxRequests,
        int maxMutations,
        int concurrencyLimit,
        int requestsPerSecond,
        int timeoutMillis,
        int maxBodyBytes,
        boolean userConfirmationRequired,
        boolean killSwitchEngaged) {
    public ActiveSafetyControls {
        if (maxRequests < 0 || maxMutations < 0 || concurrencyLimit < 1 || requestsPerSecond < 1 || timeoutMillis < 1 || maxBodyBytes < 1) {
            throw new IllegalArgumentException("invalid active safety controls");
        }
    }

    public static ActiveSafetyControls sprint2Disabled(ScopeConfiguration configuration) {
        return new ActiveSafetyControls(
                0,
                0,
                configuration.maxConcurrentActiveRequests(),
                configuration.requestsPerSecond(),
                configuration.timeoutMillis(),
                configuration.maxBodyBytes(),
                true,
                true);
    }
}
