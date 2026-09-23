package io.acra.core.engine;

import io.acra.core.domain.authorization.*;
import io.acra.core.active.evidence.EvidenceReferenceValidator;
import io.acra.core.active.evidence.ExecutionEvidenceStore;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.security.UniversalRedactor;

import java.util.List;
import java.util.Locale;

/** Deterministic function-level authorization reasoning foundation. */
public final class BflaAssessmentEvaluator {
    private static final UniversalRedactor REDACTOR = new UniversalRedactor();
    private final ExecutionEvidenceStore evidenceStore;

    public BflaAssessmentEvaluator() {
        this(null);
    }

    public BflaAssessmentEvaluator(ExecutionEvidenceStore evidenceStore) {
        this.evidenceStore = evidenceStore;
    }

    public BflaAssessment evaluate(AuthorizationContext context, String observationId, String executionId, String testId) {
        return evaluate(context, observationId, executionId, testId,
                evidenceStore == null ? null : evidenceStore.projectId());
    }

    public BflaAssessment evaluate(AuthorizationContext context, String observationId, String executionId,
                                   String testId, String projectId) {
        return evaluateInternal(context, observationId, executionId, testId, "", projectId, false);
    }

    public BflaAssessment evaluate(AuthorizationContext context, String observationId, String executionId,
                                   String testId, String endpoint, String projectId) {
        return evaluateInternal(context, observationId, executionId, testId, endpoint, projectId, true);
    }

    private BflaAssessment evaluateInternal(AuthorizationContext context, String observationId, String executionId,
                                            String testId, String endpoint, String projectId,
                                            boolean endpointRequired) {
        if (context == null || context.status() != ContextStatus.RESOLVED) {
            return assessment(context, observationId, executionId, testId, endpoint,
                    BflaAssessmentStatus.INCONCLUSIVE, BflaConfidence.INSUFFICIENT,
                    "Authorization context is unavailable, unresolved, or conflicting.");
        }
        if (evidenceStore != null && !new EvidenceReferenceValidator(evidenceStore)
                .validate(context.evidenceIds(), observationId, executionId, testId, projectId).valid()) {
            return assessment(context, observationId, executionId, testId, endpoint,
                    BflaAssessmentStatus.INCONCLUSIVE, BflaConfidence.INSUFFICIENT,
                    "Evidence references or provenance could not be authenticated.");
        }
        if (missingProvenance(observationId, executionId, testId) || missingEvidence(context.evidenceIds())) {
            return assessment(context, observationId, executionId, testId, endpoint,
                    BflaAssessmentStatus.INCONCLUSIVE, BflaConfidence.INSUFFICIENT,
                    "Assessment provenance or supporting evidence is incomplete.");
        }
        if (missingContext(context)) {
            return assessment(context, observationId, executionId, testId, endpoint,
                    BflaAssessmentStatus.INCONCLUSIVE, BflaConfidence.INSUFFICIENT,
                    "Function authorization context is incomplete.");
        }
        if (endpointRequired && isUnknown(endpoint)) {
            return assessment(context, observationId, executionId, testId, endpoint,
                    BflaAssessmentStatus.INCONCLUSIVE, BflaConfidence.INSUFFICIENT,
                    "Function endpoint binding is unavailable.");
        }
        if (!isBinary(context.expectedDecision()) || !isBinary(context.observedDecision())) {
            return assessment(context, observationId, executionId, testId, endpoint,
                    BflaAssessmentStatus.INCONCLUSIVE, BflaConfidence.INSUFFICIENT,
                    "Expected or observed decision is not a resolved binary authorization decision.");
        }
        if (context.expectedDecision() == AuthorizationDecision.DENY && context.observedDecision() == AuthorizationDecision.ALLOW) {
            return assessment(context, observationId, executionId, testId, endpoint,
                    BflaAssessmentStatus.BFLA_CANDIDATE, BflaConfidence.HIGH,
                    "Observed allow conflicts with denied function authorization expectation.");
        }
        return assessment(context, observationId, executionId, testId, endpoint,
                BflaAssessmentStatus.NO_VIOLATION, BflaConfidence.MEDIUM,
                "Observed function decision is consistent with expected authorization decision.");
    }

    private boolean missingContext(AuthorizationContext c) {
        return c.principal() == null || isUnknown(c.principal().principalId())
                || c.role() == null || isUnknown(c.role().roleId()) || isUnknown(c.role().name())
                || c.action() == null || c.action().actionType() == ActionType.UNKNOWN
                || isUnknown(c.action().applicationAction());
    }

    private boolean missingProvenance(String observationId, String executionId, String testId) {
        return isUnknown(observationId) || isUnknown(executionId) || isUnknown(testId);
    }

    private boolean missingEvidence(List<String> evidenceIds) {
        return evidenceIds.isEmpty() || evidenceIds.stream().anyMatch(this::isUnknown);
    }

    private boolean isBinary(AuthorizationDecision decision) {
        return decision == AuthorizationDecision.ALLOW || decision == AuthorizationDecision.DENY;
    }

    private boolean isUnknown(String value) {
        return value == null || value.isBlank() || "UNKNOWN".equalsIgnoreCase(value.strip())
                || value.toLowerCase(Locale.ROOT).contains(UniversalRedactor.REDACTED)
                || !value.equals(REDACTOR.redactText(value));
    }

    private BflaAssessment assessment(AuthorizationContext c, String obs, String exec, String test,
                                      String endpoint, BflaAssessmentStatus status,
                                      BflaConfidence confidence, String rationale) {
        String principal = c == null || c.principal() == null ? "" : c.principal().principalId();
        String role = c == null || c.role() == null ? "" : c.role().roleId();
        String action = c == null || c.action() == null ? "" : c.action().applicationAction();
        String material = String.valueOf(obs) + "|" + String.valueOf(exec) + "|" + String.valueOf(test)
                + "|" + String.valueOf(endpoint) + "|" + status;
        String id = "bfla-" + TokenFingerprint.sha256(material).substring(0, 24);
        return new BflaAssessment(id, obs, exec, test, principal, role, action, endpoint,
                c == null ? AuthorizationDecision.UNKNOWN : c.expectedDecision(),
                c == null ? AuthorizationDecision.UNKNOWN : c.observedDecision(), status, confidence,
                c == null ? List.of() : c.evidenceIds(), rationale);
    }
}
