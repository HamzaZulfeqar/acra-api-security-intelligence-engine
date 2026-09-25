package io.acra.core.domain.workflow;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.security.UniversalRedactor;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public record WorkflowPolicySnapshot(
        String policyId,
        String version,
        String source,
        List<WorkflowTransitionRule> transitionRules,
        List<WorkflowTokenBinding> tokenBindings,
        List<String> evidenceIds,
        AuthorizationDecision defaultDecision,
        Instant capturedAt,
        String fingerprint) {

    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public WorkflowPolicySnapshot {
        policyId = required(policyId, "policyId");
        version = safe(version);
        source = safe(source);
        transitionRules = List.copyOf(transitionRules == null ? List.<WorkflowTransitionRule>of() : transitionRules)
                .stream().sorted(Comparator.comparing(WorkflowTransitionRule::ruleId)).toList();
        tokenBindings = List.copyOf(tokenBindings == null ? List.<WorkflowTokenBinding>of() : tokenBindings)
                .stream().sorted(Comparator.comparing(WorkflowTokenBinding::bindingId)).toList();
        evidenceIds = safeList(evidenceIds);
        defaultDecision = defaultDecision == null ? AuthorizationDecision.UNKNOWN : defaultDecision;
        if (capturedAt == null) throw new IllegalArgumentException("capturedAt required");
        String computed = compute(policyId, version, source, transitionRules, tokenBindings, evidenceIds, defaultDecision);
        fingerprint = fingerprint == null || fingerprint.isBlank() ? computed : safe(fingerprint);
        if (!fingerprint.equals(computed)) throw new IllegalArgumentException("workflow policy fingerprint mismatch");
    }

    public static WorkflowPolicySnapshot create(
            String policyId,
            String version,
            String source,
            List<WorkflowTransitionRule> transitionRules,
            List<WorkflowTokenBinding> tokenBindings,
            List<String> evidenceIds,
            AuthorizationDecision defaultDecision,
            Instant capturedAt) {
        return new WorkflowPolicySnapshot(policyId, version, source, transitionRules, tokenBindings,
                evidenceIds, defaultDecision, capturedAt, "");
    }

    private static String compute(
            String policyId,
            String version,
            String source,
            List<WorkflowTransitionRule> rules,
            List<WorkflowTokenBinding> bindings,
            List<String> evidenceIds,
            AuthorizationDecision defaultDecision) {
        String material = policyId + "|" + version + "|" + source + "|" + rules + "|" + bindings + "|"
                + evidenceIds + "|" + defaultDecision;
        return "workflow-policy-" + TokenFingerprint.sha256(material);
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
                .map(WorkflowPolicySnapshot::safe)
                .filter(v -> !v.isBlank())
                .distinct()
                .sorted()
                .toList();
    }
}
