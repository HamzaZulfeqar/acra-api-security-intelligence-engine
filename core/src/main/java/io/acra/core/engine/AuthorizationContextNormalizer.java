package io.acra.core.engine;

import io.acra.core.active.evidence.EvidenceReferenceValidation;
import io.acra.core.active.evidence.EvidenceReferenceValidator;
import io.acra.core.domain.authorization.ActionType;
import io.acra.core.domain.authorization.AuthorizationContext;
import io.acra.core.domain.authorization.AuthorizationContextAssessment;
import io.acra.core.domain.authorization.AuthorizationContextCompleteness;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.AuthorizationFactState;
import io.acra.core.domain.authorization.ContextStatus;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.security.UniversalRedactor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AuthorizationContextNormalizer {
    private static final UniversalRedactor REDACTOR = new UniversalRedactor();
    private final EvidenceReferenceValidator evidenceValidator;

    public AuthorizationContextNormalizer(EvidenceReferenceValidator evidenceValidator) {
        this.evidenceValidator = evidenceValidator;
    }

    public AuthorizationContextAssessment assess(AuthorizationContext context, String endpoint, String policyReference,
                                                  String observationId, String executionId, String testId,
                                                  String projectId) {
        Map<String, AuthorizationFactState> facts = new LinkedHashMap<>();
        List<String> reasons = new ArrayList<>();

        facts.put("principal", state(context == null || context.principal() == null ? null : context.principal().principalId()));
        facts.put("role", state(context == null || context.role() == null ? null : context.role().roleId()));
        facts.put("tenant", state(context == null || context.tenant() == null ? null : context.tenant().tenantId()));
        facts.put("resource", state(context == null || context.resource() == null ? null : context.resource().resourceId()));
        facts.put("owner", ownerState(context));
        facts.put("action", actionState(context));
        facts.put("workflow", state(context == null || context.workflowState() == null ? null : context.workflowState().name()));
        facts.put("endpoint", state(endpoint));
        facts.put("policy", state(policyReference));
        facts.put("expectedDecision", decisionState(context == null ? null : context.expectedDecision()));
        facts.put("observedDecision", decisionState(context == null ? null : context.observedDecision()));

        EvidenceReferenceValidation evidenceValidation;
        if (evidenceValidator == null || context == null) {
            evidenceValidation = EvidenceReferenceValidation.rejected(List.of("EVIDENCE_VALIDATION_UNAVAILABLE"));
        } else {
            evidenceValidation = evidenceValidator.validate(context.evidenceIds(), observationId, executionId, testId, projectId);
        }
        facts.put("evidence", evidenceValidation.valid() ? AuthorizationFactState.VERIFIED : AuthorizationFactState.CONFLICTING);
        if (!evidenceValidation.valid()) reasons.addAll(evidenceValidation.reasons());

        AuthorizationContextCompleteness completeness = new AuthorizationContextCompleteness(
                verified(facts, "principal"), verified(facts, "role"), verified(facts, "tenant"),
                verified(facts, "resource"), verified(facts, "owner"), verified(facts, "action"),
                verified(facts, "workflow"), verified(facts, "endpoint"), verified(facts, "policy"),
                verified(facts, "evidence"), verified(facts, "expectedDecision"), verified(facts, "observedDecision"));

        if (context == null) reasons.add("AUTHORIZATION_CONTEXT_MISSING");
        for (Map.Entry<String, AuthorizationFactState> entry : facts.entrySet()) {
            if (entry.getValue() != AuthorizationFactState.VERIFIED) {
                reasons.add("FACT_" + entry.getKey().toUpperCase(Locale.ROOT) + "_" + entry.getValue().name());
            }
        }

        ContextStatus resolutionStatus = resolveStatus(context, evidenceValidation, facts);
        AuthorizationContext normalized = context == null ? null : new AuthorizationContext(
                context.principal(), context.role(), context.tenant(), context.resource(), context.ownerPrincipalId(),
                context.action(), context.workflowState(), context.expectedDecision(), context.observedDecision(),
                context.evidenceIds(), resolutionStatus);

        String material = canonical(projectId, observationId, executionId, testId, endpoint, policyReference,
                resolutionStatus.name());
        return new AuthorizationContextAssessment("ctx-" + TokenFingerprint.sha256(material).substring(0, 24),
                normalized, endpoint, policyReference, projectId, observationId, executionId, testId, completeness,
                facts, evidenceValidation.valid(), resolutionStatus, reasons);
    }

    private ContextStatus resolveStatus(AuthorizationContext context, EvidenceReferenceValidation evidence,
                                        Map<String, AuthorizationFactState> facts) {
        if (context == null) return ContextStatus.UNKNOWN;
        if (context.status() == ContextStatus.CONFLICTING_EVIDENCE) return ContextStatus.CONFLICTING_EVIDENCE;
        if (!evidence.valid() || facts.values().stream().anyMatch(value -> value == AuthorizationFactState.CONFLICTING)) {
            return ContextStatus.CONFLICTING_EVIDENCE;
        }
        if (context.status() == ContextStatus.RESOLVED) return ContextStatus.RESOLVED;
        if (facts.values().stream().anyMatch(value -> value == AuthorizationFactState.VERIFIED)) return ContextStatus.PARTIAL;
        return ContextStatus.UNKNOWN;
    }

    private AuthorizationFactState ownerState(AuthorizationContext context) {
        if (context == null || context.resource() == null) return AuthorizationFactState.MISSING;
        AuthorizationFactState contextOwner = state(context.ownerPrincipalId());
        AuthorizationFactState resourceOwner = state(context.resource().ownerPrincipalId());
        if (contextOwner != AuthorizationFactState.VERIFIED || resourceOwner != AuthorizationFactState.VERIFIED) {
            return contextOwner == AuthorizationFactState.REDACTED || resourceOwner == AuthorizationFactState.REDACTED
                    ? AuthorizationFactState.REDACTED : AuthorizationFactState.MISSING;
        }
        return context.ownerPrincipalId().equals(context.resource().ownerPrincipalId())
                ? AuthorizationFactState.VERIFIED : AuthorizationFactState.CONFLICTING;
    }

    private AuthorizationFactState actionState(AuthorizationContext context) {
        if (context == null || context.action() == null || context.action().actionType() == ActionType.UNKNOWN) {
            return AuthorizationFactState.MISSING;
        }
        return state(context.action().applicationAction());
    }

    private AuthorizationFactState decisionState(AuthorizationDecision decision) {
        if (decision == AuthorizationDecision.ALLOW || decision == AuthorizationDecision.DENY) {
            return AuthorizationFactState.VERIFIED;
        }
        return decision == null || decision == AuthorizationDecision.UNKNOWN
                ? AuthorizationFactState.UNKNOWN : AuthorizationFactState.CONFLICTING;
    }

    private AuthorizationFactState state(String value) {
        if (value == null || value.isBlank()) return AuthorizationFactState.MISSING;
        if (value.toLowerCase(Locale.ROOT).contains(UniversalRedactor.REDACTED)
                || !value.equals(REDACTOR.redactText(value))) return AuthorizationFactState.REDACTED;
        if ("UNKNOWN".equalsIgnoreCase(value.strip())) return AuthorizationFactState.UNKNOWN;
        return AuthorizationFactState.VERIFIED;
    }

    private boolean verified(Map<String, AuthorizationFactState> facts, String key) {
        return facts.get(key) == AuthorizationFactState.VERIFIED;
    }

    private String canonical(String... values) {
        StringBuilder result = new StringBuilder();
        for (String value : values) {
            result.append(value == null ? -1 : value.length()).append(':').append(value == null ? "" : value);
        }
        return result.toString();
    }
}
