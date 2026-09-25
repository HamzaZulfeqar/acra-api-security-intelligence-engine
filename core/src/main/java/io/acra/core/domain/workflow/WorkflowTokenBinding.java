package io.acra.core.domain.workflow;

import io.acra.core.security.UniversalRedactor;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public record WorkflowTokenBinding(
        String bindingId,
        String workflowId,
        String action,
        String principalId,
        String roleId,
        String tenantId,
        String tokenContextFingerprint,
        List<String> evidenceIds) {

    private static final UniversalRedactor REDACTOR = new UniversalRedactor();
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");

    public WorkflowTokenBinding {
        bindingId = required(bindingId, "bindingId");
        workflowId = required(workflowId, "workflowId");
        action = required(action, "action");
        principalId = safe(principalId);
        roleId = safe(roleId);
        tenantId = safe(tenantId);
        tokenContextFingerprint = fingerprint(tokenContextFingerprint);
        evidenceIds = safeList(evidenceIds);
    }

    private static String fingerprint(String value) {
        String safe = safe(value).toLowerCase(Locale.ROOT);
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
                .map(WorkflowTokenBinding::safe)
                .filter(v -> !v.isBlank())
                .distinct()
                .sorted()
                .toList();
    }
}
