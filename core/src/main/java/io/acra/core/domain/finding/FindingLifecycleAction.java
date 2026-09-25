package io.acra.core.domain.finding;

public enum FindingLifecycleAction {
    CONFIRM,
    MARK_FALSE_POSITIVE,
    ACCEPT_RISK,
    START_REMEDIATION,
    REQUEST_RETEST,
    PASS_RETEST,
    FAIL_RETEST,
    CLOSE
}
