package io.acra.core.domain.workflow;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.security.UniversalRedactor;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public record WorkflowAuthorizationRequest(
        String workflowId,
        String principalId,
        List<String> roleIds,
        String tenantId,
        String resourceId,
        String action,
        String fromState,
        String toState,
        boolean approvalProvided,
        boolean roleSeparationSatisfied,
        String delegationId,
        String tokenContextFingerprint,
        AuthorizationDecision observedDecision,
        Instant evaluatedAt) {

    private static final UniversalRedactor REDACTOR = new UniversalRedactor();
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");

    public WorkflowAuthorizationRequest {
        workflowId = required(workflowId, "workflowId");
        principalId = required(principalId, "principalId");
        roleIds = safeList(roleIds);
        tenantId = safe(tenantId);
        resourceId = safe(resourceId);
        action = required(action, "action");
        fromState = required(fromState, "fromState");
        toState = required(toState, "toState");
        delegationId = safe(delegationId);
        tokenContextFingerprint = optionalFingerprint(tokenContextFingerprint);
        observedDecision = observedDecision == null ? AuthorizationDecision.UNKNOWN : observedDecision;
        if (evaluatedAt == null) throw new IllegalArgumentException("evaluatedAt required");
    }

    private static String optionalFingerprint(String value) {
        String safe = safe(value);
        if (safe.isBlank()) return "";
        safe = safe.toLowerCase(Locale.ROOT);
        if (!SHA256.matcher(safe).matches()) {
            throw new IllegalArgumentException("tokenContextFingerprint must be a SHA-256 fingerprint, not raw authentication material");
        }
        return safe;
    }

    private static String required(String value, String name) {
        String safe = safe(value);
        if (safe.isBlank()) throw new IllegalArgumentException(name + " required");
        return safe;
    }

    private static String safe(String value) {
        return REDACTOR.redactText(value == null ? "" : value);
    }

    private static List<String> safeList(List<String> values) {
        return List.copyOf(values == null ? List.<String>of() : values).stream()
                .map(WorkflowAuthorizationRequest::safe)
                .filter(v -> !v.isBlank())
                .distinct()
                .sorted()
                .toList();
    }
}
