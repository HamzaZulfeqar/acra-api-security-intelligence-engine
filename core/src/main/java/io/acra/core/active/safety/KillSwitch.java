package io.acra.core.active.safety;

import io.acra.core.active.evidence.SafetyAuditLog;
import io.acra.core.active.evidence.SafetyEventType;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public final class KillSwitch {
    private final AtomicBoolean engaged = new AtomicBoolean(true);
    private final SafetyAuditLog audit;

    public KillSwitch(SafetyAuditLog audit) {
        if (audit == null) throw new IllegalArgumentException("audit log required");
        this.audit = audit;
    }

    public boolean engaged() {
        return engaged.get();
    }

    public void engage(String reason) {
        engaged.set(true);
        audit.append(SafetyEventType.KILL_SWITCH_ENGAGED, "", "", Map.of("reason", reason == null ? "STOP ALL" : reason));
    }

    public void reset(boolean explicitlyAuthorized, String reason) {
        if (!explicitlyAuthorized) throw new IllegalArgumentException("explicit authorization required to reset kill switch");
        engaged.set(false);
        audit.append(SafetyEventType.KILL_SWITCH_RESET, "", "", Map.of("reason", reason == null ? "authorized reset" : reason));
    }
}
