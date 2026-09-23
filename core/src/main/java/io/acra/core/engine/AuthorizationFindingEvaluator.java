package io.acra.core.engine;

import io.acra.core.active.evidence.EvidenceReferenceValidation;
import io.acra.core.active.evidence.EvidenceReferenceValidator;
import io.acra.core.domain.authorization.AuthorizationAssessmentAggregate;
import io.acra.core.domain.authorization.AuthorizationContext;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.BflaAssessment;
import io.acra.core.domain.authorization.BflaAssessmentStatus;
import io.acra.core.domain.authorization.BolaAssessment;
import io.acra.core.domain.authorization.BolaAssessmentStatus;
import io.acra.core.domain.authorization.CorrelationState;
import io.acra.core.domain.authorization.PolicyValidationState;
import io.acra.core.domain.authorization.PropertyAuthorizationAssessment;
import io.acra.core.domain.authorization.TenantAuthorizationAssessment;
import io.acra.core.domain.authorization.WorkflowAuthorizationAssessment;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.security.UniversalRedactor;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Conservative candidate evaluator. It converts validated S5 assessments into a
 * FindingCandidate only when an explicit expected DENY conflicts with observed ALLOW.
 * A candidate is not a confirmed vulnerability finding.
 */
public final class AuthorizationFindingEvaluator {
    private static final UniversalRedactor REDACTOR = new UniversalRedactor();
    private final EvidenceReferenceValidator evidenceValidator;

    public AuthorizationFindingEvaluator(EvidenceReferenceValidator evidenceValidator) {
        this.evidenceValidator = evidenceValidator;
    }

    public FindingCandidate evaluate(
            AuthorizationContext context,
            AuthorizationAssessmentAggregate aggregate,
            BolaAssessment bola,
            BflaAssessment bfla,
            TenantAuthorizationAssessment tenant,
            WorkflowAuthorizationAssessment workflow,
            PropertyAuthorizationAssessment property,
            String observationId,
            String executionId,
            String testId,
            String projectId,
            String endpoint) {

        List<String> reasons = new ArrayList<>();
        Set<String> assessmentIds = new LinkedHashSet<>();
        Set<String> evidenceIds = new LinkedHashSet<>();
        Set<String> violationTypes = new LinkedHashSet<>();

        if (context == null) reasons.add("AUTHORIZATION_CONTEXT_MISSING");
        if (evidenceValidator == null) reasons.add("EVIDENCE_VALIDATION_UNAVAILABLE");

        if (context != null) evidenceIds.addAll(context.evidenceIds());
        if (bola != null) {
            add(assessmentIds, bola.assessmentId());
            evidenceIds.addAll(bola.evidenceIds());
            if (bola.status() == BolaAssessmentStatus.BOLA_CANDIDATE
                    && violation(bola.expectedDecision(), bola.observedDecision())) {
                violationTypes.add("BOLA");
            } else if (bola.status() == BolaAssessmentStatus.INCONCLUSIVE) {
                reasons.add("BOLA_INCONCLUSIVE");
            }
        }
        if (bfla != null) {
            add(assessmentIds, bfla.assessmentId());
            evidenceIds.addAll(bfla.evidenceIds());
            if (bfla.status() == BflaAssessmentStatus.BFLA_CANDIDATE
                    && violation(bfla.expectedDecision(), bfla.observedDecision())) {
                violationTypes.add("BFLA");
            } else if (bfla.status() == BflaAssessmentStatus.INCONCLUSIVE) {
                reasons.add("BFLA_INCONCLUSIVE");
            }
        }

        addPolicy(tenant == null ? null : tenant.assessmentId(),
                tenant == null ? null : tenant.state(),
                tenant == null ? null : tenant.expectedDecision(),
                tenant == null ? null : tenant.observedDecision(),
                tenant == null ? List.of() : tenant.reasons(),
                tenant == null ? List.of() : tenant.evidenceIds(),
                "TENANT", assessmentIds, evidenceIds, violationTypes, reasons);

        addPolicy(workflow == null ? null : workflow.assessmentId(),
                workflow == null ? null : workflow.state(),
                workflow == null ? null : workflow.expectedDecision(),
                workflow == null ? null : workflow.observedDecision(),
                workflow == null ? List.of() : workflow.reasons(),
                workflow == null ? List.of() : workflow.evidenceIds(),
                "WORKFLOW", assessmentIds, evidenceIds, violationTypes, reasons);

        addPolicy(property == null ? null : property.assessmentId(),
                property == null ? null : property.state(),
                property == null ? null : property.expectedDecision(),
                property == null ? null : property.observedDecision(),
                property == null ? List.of() : property.reasons(),
                property == null ? List.of() : property.evidenceIds(),
                "PROPERTY", assessmentIds, evidenceIds, violationTypes, reasons);

        if (aggregate != null) {
            add(assessmentIds, aggregate.correlationId());
            evidenceIds.addAll(aggregate.evidenceIds());
            if (aggregate.state() == CorrelationState.CONFLICTING) reasons.add("ASSESSMENT_CORRELATION_CONFLICT");
            if (aggregate.state() == CorrelationState.INSUFFICIENT
                    || aggregate.state() == CorrelationState.INCONCLUSIVE) {
                reasons.add("ASSESSMENT_CORRELATION_INSUFFICIENT");
            }
        }

        if (context != null && evidenceValidator != null) {
            EvidenceReferenceValidation validation = evidenceValidator.validate(
                    List.copyOf(evidenceIds), observationId, executionId, testId, projectId);
            if (!validation.valid()) reasons.addAll(validation.reasons());
        }

        FindingCandidateState state;
        String confidence;
        if (containsEvidenceFailure(reasons)) {
            state = FindingCandidateState.INCONCLUSIVE;
            confidence = "INSUFFICIENT";
        } else if (reasons.stream().anyMatch(reason -> reason.contains("CONFLICT"))) {
            state = FindingCandidateState.CONFLICTING;
            confidence = "INSUFFICIENT";
        } else if (!violationTypes.isEmpty()) {
            state = FindingCandidateState.SUPPORTED;
            confidence = "HIGH";
        } else if (reasons.stream().anyMatch(reason -> reason.contains("INCONCLUSIVE")
                || reason.contains("INSUFFICIENT") || reason.contains("MISSING"))) {
            state = FindingCandidateState.INCONCLUSIVE;
            confidence = "INSUFFICIENT";
        } else {
            state = FindingCandidateState.REJECTED;
            confidence = "MEDIUM";
        }

        AuthorizationDecision expected = context == null ? AuthorizationDecision.UNKNOWN : context.expectedDecision();
        AuthorizationDecision observed = context == null ? AuthorizationDecision.UNKNOWN : context.observedDecision();
        String principal = context == null || context.principal() == null ? "" : context.principal().principalId();
        String resource = context == null || context.resource() == null ? "" : context.resource().resourceId();
        String tenantRelationship = tenantRelationship(context, tenant);
        String category = violationTypes.isEmpty() ? "AUTHORIZATION" : String.join("+", violationTypes);
        String candidateId = "candidate-" + TokenFingerprint.sha256(canonical(
                projectId, observationId, executionId, testId, category, principal, resource, endpoint,
                expected.name(), observed.name())).substring(0, 24);
        FindingFingerprint fingerprint = FindingFingerprint.of(endpoint, resource, principal, tenantRelationship,
                category, "expected-vs-observed-authorization");
        String rationale = state == FindingCandidateState.SUPPORTED
                ? "Validated authorization assessment contains explicit expected-deny/observed-allow evidence"
                : "Evidence did not establish a supported authorization finding candidate";

        return new FindingCandidate(candidateId, fingerprint, category, observationId, executionId, testId,
                projectId, principal, resource, endpoint, expected, observed, state, confidence,
                List.copyOf(assessmentIds), List.copyOf(evidenceIds), rationale, reasons);
    }

    private void addPolicy(String assessmentId, PolicyValidationState state, AuthorizationDecision expected,
                           AuthorizationDecision observed, List<String> policyReasons, List<String> policyEvidence,
                           String category, Set<String> assessmentIds, Set<String> evidenceIds,
                           Set<String> violationTypes, List<String> reasons) {
        if (assessmentId == null) return;
        add(assessmentIds, assessmentId);
        evidenceIds.addAll(policyEvidence);
        if (state == PolicyValidationState.CONFLICTING && violation(expected, observed)
                && policyReasons.isEmpty()) {
            violationTypes.add(category);
            return;
        }
        if (state == PolicyValidationState.INCONCLUSIVE) reasons.add(category + "_INCONCLUSIVE");
        if (state == PolicyValidationState.CONFLICTING && !policyReasons.isEmpty()) {
            reasons.add(category + "_POLICY_CONFLICT");
            reasons.addAll(policyReasons);
        }
    }

    private boolean containsEvidenceFailure(List<String> reasons) {
        return reasons.stream().map(value -> value.toUpperCase(Locale.ROOT)).anyMatch(value ->
                value.contains("EVIDENCE_") || value.contains("OBSERVATION_")
                        || value.contains("PROVENANCE") || value.contains("CROSS_PROJECT")
                        || value.contains("OWNERSHIP_MISMATCH") || value.contains("REPLAY_LINEAGE"));
    }

    private boolean violation(AuthorizationDecision expected, AuthorizationDecision observed) {
        return expected == AuthorizationDecision.DENY && observed == AuthorizationDecision.ALLOW;
    }

    private String tenantRelationship(AuthorizationContext context, TenantAuthorizationAssessment tenant) {
        if (tenant != null && !tenant.subjectTenantId().isBlank() && !tenant.resourceTenantId().isBlank()) {
            return tenant.subjectTenantId() + "->" + tenant.resourceTenantId();
        }
        String subject = context == null || context.tenant() == null ? "UNKNOWN" : context.tenant().tenantId();
        String resource = context == null || context.resource() == null || context.resource().tenantId() == null
                ? "UNKNOWN" : context.resource().tenantId();
        return subject + "->" + resource;
    }

    private void add(Set<String> values, String value) {
        if (known(value)) values.add(value);
    }

    private boolean known(String value) {
        return value != null && !value.isBlank() && !"UNKNOWN".equalsIgnoreCase(value.strip())
                && !value.toLowerCase(Locale.ROOT).contains(UniversalRedactor.REDACTED)
                && value.equals(REDACTOR.redactText(value));
    }

    private String canonical(String... values) {
        StringBuilder result = new StringBuilder();
        for (String value : values) {
            result.append(value == null ? -1 : value.length()).append(':').append(value == null ? "" : value);
        }
        return result.toString();
    }
}
