package io.acra.core.active.model;

import io.acra.core.domain.http.HttpMethod;
import java.util.Set;

public record SafetyPolicy(
        SafetyClass safetyClass,
        boolean confirmationRequired,
        boolean destructiveConfirmationRequired,
        int requestBudget,
        int mutationBudget,
        int maxResponseBytes,
        Set<HttpMethod> allowedMethods) {
    public SafetyPolicy {
        if (safetyClass == null) safetyClass = SafetyClass.SAFE_READ_ONLY;
        if (requestBudget < 0 || mutationBudget < 0 || maxResponseBytes < 1) {
            throw new IllegalArgumentException("invalid safety limits");
        }
        if (safetyClass == SafetyClass.DESTRUCTIVE && !destructiveConfirmationRequired) {
            throw new IllegalArgumentException("destructive policies must require destructive confirmation");
        }
        allowedMethods = Set.copyOf(allowedMethods == null ? Set.of() : allowedMethods);
    }

    public static SafetyPolicy safeLabReadOnly(int requestBudget, int mutationBudget) {
        return new SafetyPolicy(
                SafetyClass.SAFE_READ_ONLY,
                true,
                true,
                requestBudget,
                mutationBudget,
                2 * 1024 * 1024,
                Set.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS));
    }
}
