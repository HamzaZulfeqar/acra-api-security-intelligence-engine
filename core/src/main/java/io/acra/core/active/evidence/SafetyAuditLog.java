package io.acra.core.active.evidence;

import io.acra.core.security.UniversalRedactor;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

public final class SafetyAuditLog {
    private final Clock clock;
    private final UniversalRedactor redactor = new UniversalRedactor();
    private final AtomicLong sequence = new AtomicLong();
    private final List<SafetyAuditEvent> events = new ArrayList<>();

    public SafetyAuditLog() {
        this(Clock.systemUTC());
    }

    public SafetyAuditLog(Clock clock) {
        if (clock == null) throw new IllegalArgumentException("clock required");
        this.clock = clock;
    }

    public synchronized SafetyAuditEvent append(
            SafetyEventType type,
            String executionId,
            String testId,
            Map<String, String> details) {
        if (type == null) throw new IllegalArgumentException("event type required");
        TreeMap<String, String> safe = new TreeMap<>();
        if (details != null) details.forEach((key, value) -> safe.put(key, redactor.redactText(value)));
        SafetyAuditEvent event = new SafetyAuditEvent(
                String.format("S4-AUDIT-%08d", sequence.incrementAndGet()),
                clock.instant(), type, executionId, testId, safe);
        events.add(event);
        return event;
    }

    public synchronized List<SafetyAuditEvent> entries() {
        return List.copyOf(events);
    }
}
