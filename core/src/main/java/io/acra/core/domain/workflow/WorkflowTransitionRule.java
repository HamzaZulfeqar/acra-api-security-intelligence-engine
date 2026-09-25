package io.acra.core.domain.workflow;

import io.acra.core.domain.authorization.AuthorizationRuleEffect;
import io.acra.core.security.UniversalRedactor;
import java.util.List;

public record WorkflowTransitionRule(
        String ruleId,
        String workflowId,
        String fromState,
        String toState,
        String action,
        String tenantId,
        List<String> requiredRoleIds,
        boolean approvalRequired,
        boolean roleSeparationRequired,
        boolean terminalSource,
        boolean delegationAllowed,
        String tokenBindingId,
        AuthorizationRuleEffect effect,
        Integer precedence,
        String precedenceSource,
        List<String> evidenceIds) {

    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public WorkflowTransitionRule {
        ruleId = required(ruleId, "ruleId");
        workflowId = required(workflowId, "workflowId");
        fromState = required(fromState, "fromState");
        toState = required(toState, "toState");
        action = required(action, "action");
        tenantId = safe(tenantId);
        requiredRoleIds = safeList(requiredRoleIds);
        tokenBindingId = safe(tokenBindingId);
        effect = effect == null ? AuthorizationRuleEffect.DENY : effect;
        precedenceSource = safe(precedenceSource);
        evidenceIds = safeList(evidenceIds);
        if (precedence != null && precedence < 0) throw new IllegalArgumentException("precedence");
    }

    public boolean hasExplicitPrecedence() {
        return precedence != null && !precedenceSource.isBlank();
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
                .map(WorkflowTransitionRule::safe)
                .filter(v -> !v.isBlank())
                .distinct()
                .sorted()
                .toList();
    }
}
