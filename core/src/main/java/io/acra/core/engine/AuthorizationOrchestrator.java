package io.acra.core.engine;

import io.acra.core.active.evidence.EvidenceReferenceValidator;
import io.acra.core.active.evidence.ExecutionEvidenceStore;
import io.acra.core.domain.authorization.AuthorizationAnalysisResult;
import io.acra.core.domain.authorization.AuthorizationContextAssessment;
import io.acra.core.domain.authorization.AuthorizationCorrelationEnvelope;
import io.acra.core.domain.authorization.BflaAssessment;
import io.acra.core.domain.authorization.BolaAssessment;
import io.acra.core.domain.authorization.PropertyAuthorizationAssessment;
import io.acra.core.domain.authorization.TenantAuthorizationAssessment;
import io.acra.core.domain.authorization.WorkflowAuthorizationAssessment;
import io.acra.core.domain.finding.AuthorizationRiskAssessment;
import io.acra.core.domain.finding.FindingCandidate;

public final class AuthorizationOrchestrator {
    private final ExecutionEvidenceStore evidenceStore;
    private final EvidenceReferenceValidator evidenceValidator;
    private final AuthorizationContextNormalizer contextNormalizer;
    private final BolaAssessmentEvaluator bolaEvaluator;
    private final BflaAssessmentEvaluator bflaEvaluator;
    private final AuthorizationDimensionEvaluator dimensionEvaluator;
    private final AuthorizationAssessmentCorrelationService correlationService;
    private final AuthorizationFindingEvaluator findingEvaluator;
    private final AuthorizationSeverityEvaluator severityEvaluator;

    public AuthorizationOrchestrator(ExecutionEvidenceStore evidenceStore) {
        if (evidenceStore == null) throw new IllegalArgumentException("evidenceStore required");
        this.evidenceStore = evidenceStore;
        this.evidenceValidator = new EvidenceReferenceValidator(evidenceStore);
        this.contextNormalizer = new AuthorizationContextNormalizer(evidenceValidator);
        this.bolaEvaluator = new BolaAssessmentEvaluator(evidenceStore);
        this.bflaEvaluator = new BflaAssessmentEvaluator(evidenceStore);
        this.dimensionEvaluator = new AuthorizationDimensionEvaluator(new PolicyValidationEvaluator(evidenceValidator));
        this.correlationService = new AuthorizationAssessmentCorrelationService();
        this.findingEvaluator = new AuthorizationFindingEvaluator();
        this.severityEvaluator = new AuthorizationSeverityEvaluator();
    }

    public AuthorizationAnalysisResult analyze(AuthorizationAnalysisRequest request) {
        if (request == null) throw new IllegalArgumentException("request required");
        AuthorizationContextAssessment contextAssessment = contextNormalizer.assess(request.context(),
                request.endpoint(), request.policyReference(), request.observationId(), request.executionId(),
                request.testId(), request.projectId());

        BolaAssessment bola = contextAssessment.readyForObjectLevel()
                ? bolaEvaluator.evaluate(contextAssessment.normalizedContext(), request.observationId(),
                    request.executionId(), request.testId(), request.projectId())
                : bolaEvaluator.evaluate(contextAssessment.normalizedContext(), request.observationId(),
                    request.executionId(), request.testId(), request.projectId());

        BflaAssessment bfla = bflaEvaluator.evaluate(contextAssessment.normalizedContext(), request.observationId(),
                request.executionId(), request.testId(), request.projectId(), request.endpoint());

        TenantAuthorizationAssessment tenant = request.tenantPolicy() == null ? null
                : dimensionEvaluator.evaluateTenant(contextAssessment.normalizedContext(), request.tenantPolicy(),
                    request.observationId(), request.executionId(), request.testId(), request.projectId());

        WorkflowAuthorizationAssessment workflow = request.workflowPolicy() == null ? null
                : dimensionEvaluator.evaluateWorkflow(contextAssessment.normalizedContext(), request.workflowPolicy(),
                    request.currentWorkflowState(), request.requestedWorkflowState(), request.approvalProvided(),
                    request.roleSeparationSatisfied(), request.observationId(), request.executionId(),
                    request.testId(), request.projectId());

        PropertyAuthorizationAssessment property = request.propertyPolicy() == null ? null
                : dimensionEvaluator.evaluateProperty(contextAssessment.normalizedContext(), request.propertyPolicy(),
                    request.property(), request.propertyOperation(), request.endpoint(), request.observationId(),
                    request.executionId(), request.testId(), request.projectId());

        AuthorizationCorrelationEnvelope correlation = correlationService.correlate(bola, bfla, tenant, workflow,
                property, evidenceValidator, request.projectId(), request.policyReference(), false);
        FindingCandidate candidate = findingEvaluator.evaluate(contextAssessment, bola, bfla, tenant, workflow,
                property, correlation, request.endpoint());
        AuthorizationRiskAssessment risk = severityEvaluator.evaluate(candidate, request.impactProfile());
        return new AuthorizationAnalysisResult(contextAssessment, bola, bfla, tenant, workflow, property,
                correlation, candidate, risk);
    }

    public String projectId() {
        return evidenceStore.projectId();
    }
}
