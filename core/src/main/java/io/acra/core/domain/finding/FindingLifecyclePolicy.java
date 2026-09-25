package io.acra.core.domain.finding;

public final class FindingLifecyclePolicy {
    private FindingLifecyclePolicy() { }

    public static FindingLifecycleState target(
            FindingLifecycleState current,
            FindingLifecycleAction action) {
        if (current == null || action == null) {
            throw new IllegalArgumentException("current/action required");
        }

        return switch (current) {
            case REVIEW_REQUIRED -> switch (action) {
                case CONFIRM -> FindingLifecycleState.CONFIRMED;
                case MARK_FALSE_POSITIVE -> FindingLifecycleState.FALSE_POSITIVE;
                default -> invalid(current, action);
            };
            case CONFIRMED -> switch (action) {
                case ACCEPT_RISK -> FindingLifecycleState.ACCEPTED_RISK;
                case START_REMEDIATION -> FindingLifecycleState.REMEDIATION_IN_PROGRESS;
                default -> invalid(current, action);
            };
            case REMEDIATION_IN_PROGRESS -> switch (action) {
                case REQUEST_RETEST -> FindingLifecycleState.RETEST_REQUIRED;
                default -> invalid(current, action);
            };
            case RETEST_REQUIRED -> switch (action) {
                case PASS_RETEST -> FindingLifecycleState.RESOLVED;
                case FAIL_RETEST -> FindingLifecycleState.CONFIRMED;
                default -> invalid(current, action);
            };
            case FALSE_POSITIVE, ACCEPTED_RISK, RESOLVED -> switch (action) {
                case CLOSE -> FindingLifecycleState.CLOSED;
                default -> invalid(current, action);
            };
            case CLOSED -> invalid(current, action);
        };
    }

    private static FindingLifecycleState invalid(
            FindingLifecycleState current,
            FindingLifecycleAction action) {
        throw new IllegalArgumentException(
                "invalid finding lifecycle transition: " + current + " -> " + action);
    }
}
