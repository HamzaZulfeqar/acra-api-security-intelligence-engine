package io.acra.core.active.safety;

import io.acra.core.active.model.SafetyClass;
import io.acra.core.active.model.SecurityTest;

public final class ConsentGuard {
    public ValidationDecision evaluate(SecurityTest test, ActiveConsent consent) {
        if (test == null || consent == null) return ValidationDecision.invalid("test and consent are required");
        if (!consent.activeTestingEnabled()) return ValidationDecision.blocked("active testing is disabled");
        if (!consent.targetAuthorized() || !test.target().authorized()) return ValidationDecision.blocked("target authorization is absent");
        if (!consent.testApproved()) return ValidationDecision.confirmation("test approval is required");
        if (test.safetyPolicy().confirmationRequired() && !consent.confirmationAcknowledged()) {
            return ValidationDecision.confirmation("execution confirmation is required");
        }
        if ((test.mutation().safetyClass() == SafetyClass.DESTRUCTIVE
                || test.safetyPolicy().safetyClass() == SafetyClass.DESTRUCTIVE)
                && !consent.destructiveOperationApproved()) {
            return ValidationDecision.blocked("destructive operation approval is absent");
        }
        return ValidationDecision.allowed();
    }
}
