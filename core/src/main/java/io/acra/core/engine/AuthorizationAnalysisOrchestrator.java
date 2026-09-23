package io.acra.core.engine;

import io.acra.core.active.evidence.EvidenceReferenceValidator;
import io.acra.core.active.evidence.ExecutionEvidenceStore;
import io.acra.core.active.evidence.Observation;
import io.acra.core.domain.authorization.AuthorizationAnalysisRequest;
import io.acra.core.domain.authorization.AuthorizationAnalysisResult;
import io.acra.core.domain.authorization.AuthorizationAssessmentAggregate;
import io.acra.core.domain.authorization.AuthorizationContext;
import io.acra.core.domain.authorization.AuthorizationContextNormalizationResult;
import io.acra.core.domain.authorization.BflaAssessment;
import io.acra.core.domain.authorization.BolaAssessment;
import io.acra.core.domain.authorization.PropertyAuthorizationAssessment;
import io.acra.core.domain.authorization.TenantAuthorizationAssessment;
import io.acra.core.domain.authorization.WorkflowAuthorizationAssessment;
import io.acra.core.domain.finding.AuthorizationSeverity;
import io.acra.core.domain.finding.FindingCandidate;
import java.util.List;

/**
 * S5 defensive orchestration from an authenticated S4 Observation to assessments,
 * candidate evaluation, and explicit-impact severity. It performs no network I/O.
 */
public final class AuthorizationAnalysisOrchestrator {
    private final EvidenceReferenceValidator validator;
    private final AuthorizationContextNormalizer normalizer;
    private final BolaAssessmentEvaluator bolaEvaluator;
    private final BflaAssessmentEvaluator bflaEvaluator;
    private final TenantAuthorizationEvaluator tenantEvaluator;
    private final WorkflowAuthorizationEvaluator workflowEvaluator;
    private final PropertyAuthorizationEvaluator propertyEvaluator;
    private final AuthorizationFindingEvaluator findingEvaluator;
    private final AuthorizationSeverityEvaluator severityEvaluator;

    public AuthorizationAnalysisOrchestrator(ExecutionEvidenceStore store) {
        if (store == null) throw new IllegalArgumentException("evidence store required");
        this.validator = new EvidenceReferenceValidator(store);
        this.normalizer = new AuthorizationContextNormalizer(validator);
        this.bolaEvaluator = new BolaAssessmentEvaluator(store);
        this.bflaEvaluator = new BflaAssessmentEvaluator(store);
        PolicyValidationEvaluator policyEvaluator = new PolicyValidationEvaluator(validator);
        this.tenantEvaluator = new TenantAuthorizationEvaluator(policyEvaluator);
        this.workflowEvaluator = new WorkflowAuthorizationEvaluator(policyEvaluator);
        this.propertyEvaluator = new PropertyAuthorizationEvaluator(policyEvaluator);
        this.findingEvaluator = new AuthorizationFindingEvaluator(validator);
        this.severityEvaluator = new AuthorizationSeverityEvaluator();
    }

    public AuthorizationAnalysisResult analyze(AuthorizationAnalysisRequest request) {
        if (request == null) throw new IllegalArgumentException("request required");
        Observation observation = request.observation();
        AuthorizationContextNormalizationResult normalization = normalizer.normalizeTarget(
                observation, request.projectId());

        if (!normalization.valid()) {
            FindingCandidate candidate = findingEvaluator.evaluate(
                    null, null, null, null, null, null, null,
                    observation == null ? "" : observation.observationId(),
                    observation == null ? "" : observation.executionFingerprint().executionId(),
                    observation == null ? "" : observation.testId(),
                    request.projectId(), request.endpoint());
            AuthorizationSeverity severity = severityEvaluator.evaluate(candidate, request.impact());
            return new AuthorizationAnalysisResult(normalization, null, null, null, null, null,
                    null, candidate, severity);
        }

        AuthorizationContext context = normalization.context();
        String observationId = observation.observationId();
        String executionId = observation.executionFingerprint().executionId();
        String testId = observation.testId();

        BolaAssessment bola = bolaEvaluator.evaluate(context, observationId, executionId, testId, request.projectId());
        BflaAssessment bfla = bflaEvaluator.evaluate(
                context, observationId, executionId, testId, request.endpoint(), request.projectId());

        TenantAuthorizationAssessment tenant = request.tenantPolicy() == null ? null
                : tenantEvaluator.evaluate(context, request.tenantPolicy(), observationId, executionId,
                        testId, request.projectId());

        WorkflowAuthorizationAssessment workflow = request.workflowPolicy() == null ? null
                : workflowEvaluator.evaluate(context, request.workflowPolicy(),
                        request.currentWorkflowState(), request.requestedWorkflowState(),
                        request.approvalProvided(), request.roleSeparationSatisfied(),
                        observationId, executionId, testId, request.projectId());

        PropertyAuthorizationAssessment property = request.propertyPolicy() == null ? null
                : propertyEvaluator.evaluate(context, request.propertyPolicy(), request.property(),
                        request.propertyOperation(), request.propertyEndpoint(),
                        observationId, executionId, testId, request.projectId());

        AuthorizationAssessmentAggregate aggregate = AuthorizationAssessmentCorrelator.correlate(
                List.of(bola), List.of(bfla), validator, request.projectId());

        FindingCandidate candidate = findingEvaluator.evaluate(
                context, aggregate, bola, bfla, tenant, workflow, property,
                observationId, executionId, testId, request.projectId(), request.endpoint());
        AuthorizationSeverity severity = severityEvaluator.evaluate(candidate, request.impact());

        return new AuthorizationAnalysisResult(normalization, bola, bfla, tenant, workflow, property,
                aggregate, candidate, severity);
    }
}
