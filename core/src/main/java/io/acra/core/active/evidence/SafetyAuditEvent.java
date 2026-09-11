package io.acra.core.active.evidence;

import io.acra.core.domain.common.Validation;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public record SafetyAuditEvent(
        String eventId,
        Instant timestamp,
        SafetyEventType type,
        String executionId,
        String testId,
        Map<String, String> details) {
    public SafetyAuditEvent {
        eventId = Validation.requireNonBlank(eventId, "eventId");
        if (timestamp == null || type == null) throw new IllegalArgumentException("audit timestamp and type required");
        executionId = executionId == null ? "" : executionId;
        testId = testId == null ? "" : testId;
        TreeMap<String, String> copy = new TreeMap<>();
        if (details != null) copy.putAll(details);
        details = Collections.unmodifiableMap(copy);
    }
}
