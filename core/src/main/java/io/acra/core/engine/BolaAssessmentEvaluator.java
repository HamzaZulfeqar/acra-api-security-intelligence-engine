package io.acra.core.engine;

import io.acra.core.active.evidence.EvidenceReferenceValidator;
import io.acra.core.active.evidence.ExecutionEvidenceStore;
import io.acra.core.domain.authorization.ActionType;
import io.acra.core.domain.authorization.AuthorizationContext;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.BolaAssessment;
import io.acra.core.domain.authorization.BolaAssessmentStatus;
import io.acra.core.domain.authorization.BolaConfidence;
import io.acra.core.domain.authorization.ContextStatus;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.security.UniversalRedactor;
import java.util.List;
import java.util.Locale;

/**
 * Deterministic object-level authorization reasoning.
 * Produces assessments only; it does not create confirmed vulnerability findings.
 */
public final class BolaAssessmentEvaluator {
    private static final UniversalRedactor REDACTOR = new UniversalRedactor();
    private final ExecutionEvidenceStore evidenceStore;

    public BolaAssessmentEvaluator() {
        this(null);
    }

    public BolaAssessmentEvaluator(ExecutionEvidenceStore evidenceStore) {
        this.evidenceStore = evidenceStore;
    }

    public BolaAssessment evaluate(AuthorizationContext context, String observationId,
                                   String executionId, String testId) {
        return evaluate(context, observationId, executionId, testId,
                evidenceStore == null ? null : evidenceStore.projectId());
    }

    public BolaAssessment evaluate(AuthorizationContext context, String observationId,
                                   String executionId, String testId, String projectId) {
        if (context == null || context.status() != ContextStatus.RESOLVED) {
            return assessment(context, observationId, executionId, testId,
                    BolaAssessmentStatus.INCONCLUSIVE, BolaConfidence.INSUFFICIENT,
                    "Authorization context is unavailable, unresolved, or conflicting.");
        }

        if (evidenceStore != null && !new EvidenceReferenceValidator(evidenceStore)
                .validate(context.evidenceIds(), observationId, executionId, testId, projectId).valid()) {
            return assessment(context, observationId, executionId, testId,
                    BolaAssessmentStatus.INCONCLUSIVE, BolaConfidence.INSUFFICIENT,
                    "Evidence references or provenance could not be authenticated.");
        }

        if (missingProvenance(observationId, executionId, testId) || missingEvidence(context.evidenceIds())) {
            return assessment(context, observationId, executionId, testId,
                    BolaAssessmentStatus.INCONCLUSIVE, BolaConfidence.INSUFFICIENT,
                    "Assessment provenance or supporting evidence is incomplete.");
        }

        if (missingRequiredContext(context)) {
            return assessment(context, observationId, executionId, testId,
                    BolaAssessmentStatus.INCONCLUSIVE, BolaConfidence.INSUFFICIENT,
                    "Required authorization evidence is incomplete.");
        }

        if (!context.ownerPrincipalId().equals(context.resource().ownerPrincipalId())) {
            return assessment(context, observationId, executionId, testId,
                    BolaAssessmentStatus.INCONCLUSIVE, BolaConfidence.INSUFFICIENT,
                    "Authorization context owner conflicts with resource owner evidence.");
        }

        AuthorizationDecision expected = context.expectedDecision();
        AuthorizationDecision observed = context.observedDecision();
        if (!isBinary(expected) || !isBinary(observed)) {
            return assessment(context, observationId, executionId, testId,
                    BolaAssessmentStatus.INCONCLUSIVE, BolaConfidence.INSUFFICIENT,
                    "Expected or observed decision is not a resolved binary authorization decision.");
        }

        if (expected == AuthorizationDecision.DENY && observed == AuthorizationDecision.ALLOW) {
            return assessment(context, observationId, executionId, testId,
                    BolaAssessmentStatus.BOLA_CANDIDATE, BolaConfidence.HIGH,
                    "Observed allow conflicts with expected deny for object authorization context.");
        }

        return assessment(context, observationId, executionId, testId,
                BolaAssessmentStatus.NO_VIOLATION, BolaConfidence.MEDIUM,
                "Observed decision is consistent with expected authorization decision.");
    }

    private boolean missingRequiredContext(AuthorizationContext context) {
        return context.principal() == null || isUnknown(context.principal().principalId())
                || context.resource() == null || isUnknown(context.resource().resourceId())
                || context.action() == null || context.action().actionType() == ActionType.UNKNOWN
                || isUnknown(context.ownerPrincipalId()) || isUnknown(context.resource().ownerPrincipalId());
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

    private String deterministicId(String observationId, String executionId, String testId, String status) {
        String material = String.valueOf(observationId) + "|" + String.valueOf(executionId)
                + "|" + String.valueOf(testId) + "|" + status;
        return "bola-" + TokenFingerprint.sha256(material).substring(0, 24);
    }

    private BolaAssessment assessment(AuthorizationContext context, String observationId,
                                      String executionId, String testId,
                                      BolaAssessmentStatus status, BolaConfidence confidence,
                                      String rationale) {
        String principal = context == null || context.principal() == null ? "" : context.principal().principalId();
        String resource = context == null || context.resource() == null ? "" : context.resource().resourceId();
        String owner = context == null ? "" : context.ownerPrincipalId();
        String action = context == null || context.action() == null ? "" : context.action().actionType().name();
        return new BolaAssessment(deterministicId(observationId, executionId, testId, status.name()),
                observationId, executionId, testId, principal, resource, owner, action,
                context == null ? AuthorizationDecision.UNKNOWN : context.expectedDecision(),
                context == null ? AuthorizationDecision.UNKNOWN : context.observedDecision(),
                status, confidence, context == null ? List.of() : context.evidenceIds(), rationale);
    }
}
