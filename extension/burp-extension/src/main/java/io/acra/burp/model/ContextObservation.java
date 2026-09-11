package io.acra.burp.model;

import io.acra.core.domain.authorization.ActionType;
import io.acra.core.domain.authorization.ContextStatus;
import java.time.Instant;
import java.util.List;

/** Passive UI/inventory projection. It is evidence, not a vulnerability verdict. */
public record ContextObservation(
        String transactionId,
        Instant timestamp,
        String principal,
        String role,
        String tenant,
        String session,
        String resource,
        String owner,
        ActionType action,
        String rawUri,
        List<String> evidenceIds,
        double confidence,
        ContextStatus status) {
    public ContextObservation {
        principal = safe(principal);
        role = safe(role);
        tenant = safe(tenant);
        session = safe(session);
        resource = safe(resource);
        owner = safe(owner);
        rawUri = safe(rawUri);
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
        if (confidence < 0.0 || confidence > 1.0) throw new IllegalArgumentException("confidence must be 0-1");
        if (action == null) action = ActionType.UNKNOWN;
        if (status == null) status = ContextStatus.PARTIAL;
    }
    private static String safe(String value){ return value == null || value.isBlank() ? "UNKNOWN" : value; }
}
