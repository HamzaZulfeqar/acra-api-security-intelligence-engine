package io.acra.core.engine;

import io.acra.core.active.evidence.EvidenceReferenceValidator;
import io.acra.core.active.evidence.ExecutionEvidenceStore;
import io.acra.core.domain.authorization.ActionType;
import io.acra.core.domain.authorization.AuthorizationContext;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.BflaAssessment;
import io.acra.core.domain.authorization.BflaAssessmentStatus;
import io.acra.core.domain.authorization.BflaConfidence;
import io.acra.core.domain.authorization.ContextStatus;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.security.UniversalRedactor;
import java.util.List;
import java.util.Locale;

/** Deterministic function-level authorization reasoning. */
public final class BflaAssessmentEvaluator {
    private static final UniversalRedactor REDACTOR = new UniversalRedactor();
    private final ExecutionEvidenceStore evidenceStore;

    public BflaAssessmentEvaluator() {
        this(null);
    }

    public BflaAssessmentEvaluator(ExecutionEvidenceStore evidenceStore) {
        this.evidenceStore = evidenceStore;
    }

    public BflaAssessment evaluate(AuthorizationContext context, String observationId, String executionId,
                                   String testId) {
        return evaluateInternal(context, observationId, executionId, testId,
                evidenceStore == null ? null : evidenceStore.projectId(), "", false);
    }

    public BflaAssessment evaluate(AuthorizationContext context, String observationId, String executionId,
                                   String testId, String projectId) {
        return evaluateInternal(context, observationId, executionId, testId, projectId, "", false);
    }

    public BflaAssessment evaluate(AuthorizationContext context, String observationId, String executionId,
                                   String testId, String projectId, String endpoint) {
        return evaluateInternal(context, observationId, executionId, testId, projectId, endpoint, true);
    }

    private BflaAssessment evaluateInternal(AuthorizationContext context, String observationId, String executionId,
                                            String testId, String projectId, String endpoint,
                                            boolean requireEndpoint) {
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
        if (missingContext(context) || (requireEndpoint && isUnknown(endpoint))) {
            return assessment(context, observationId, executionId, testId, endpoint,
                    BflaAssessmentStatus.INCONCLUSIVE, BflaConfidence.INSUFFICIENT,
                    "Function authorization context or endpoint binding is incomplete.");
        }
        if (!isBinary(context.expectedDecision()) || !isBinary(context.observedDecision())) {
            return assessment(context, observationId, executionId, testId, endpoint,
                    BflaAssessmentStatus.INCONCLUSIVE, BflaConfidence.INSUFFICIENT,
                    "Expected or observed decision is not a resolved binary authorization decision.");
        }
        if (context.expectedDecision() == AuthorizationDecision.DENY
                && context.observedDecision() == AuthorizationDecision.ALLOW) {
            return assessment(context, observationId, executionId, testId, endpoint,
                    BflaAssessmentStatus.BFLA_CANDIDATE, BflaConfidence.HIGH,
                    "Observed allow conflicts with denied function authorization expectation.");
        }
        return assessment(context, observationId, executionId, testId, endpoint,
                BflaAssessmentStatus.NO_VIOLATION, BflaConfidence.MEDIUM,
                "Observed function decision is consistent with expected authorization decision.");
    }

    private boolean missingContext(AuthorizationContext context) {
        return context.principal() == null || isUnknown(context.principal().principalId())
                || context.role() == null || isUnknown(context.role().roleId()) || isUnknown(context.role().name())
                || context.action() == null || context.action().actionType() == ActionType.UNKNOWN
                || isUnknown(context.action().applicationAction());
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

    private BflaAssessment assessment(AuthorizationContext context, String observationId, String executionId,
                                      String testId, String endpoint, BflaAssessmentStatus status,
                                      BflaConfidence confidence, String rationale) {
        String principal = context == null || context.principal() == null ? "" : context.principal().principalId();
        String role = context == null || context.role() == null ? "" : context.role().roleId();
        String action = context == null || context.action() == null ? "" : context.action().applicationAction();
        String material = String.valueOf(observationId) + "|" + String.valueOf(executionId) + "|"
                + String.valueOf(testId) + "|" + String.valueOf(endpoint) + "|" + status;
        String id = "bfla-" + TokenFingerprint.sha256(material).substring(0, 24);
        return new BflaAssessment(id, observationId, executionId, testId, principal, role, action, endpoint,
                context == null ? AuthorizationDecision.UNKNOWN : context.expectedDecision(),
                context == null ? AuthorizationDecision.UNKNOWN : context.observedDecision(),
                status, confidence, context == null ? List.of() : context.evidenceIds(), rationale);
    }
}
