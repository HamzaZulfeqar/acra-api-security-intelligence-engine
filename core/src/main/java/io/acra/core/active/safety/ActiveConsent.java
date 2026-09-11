package io.acra.core.active.safety;

public record ActiveConsent(
        boolean activeTestingEnabled,
        boolean targetAuthorized,
        boolean testApproved,
        boolean confirmationAcknowledged,
        boolean destructiveOperationApproved) {
    public static ActiveConsent disabled() {
        return new ActiveConsent(false, false, false, false, false);
    }
}
