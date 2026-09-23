package io.acra.core.engine;

import io.acra.core.active.evidence.EvidenceReferenceValidator;
import io.acra.core.active.evidence.ExecutionEvidenceStore;
import io.acra.core.domain.finding.AuthorizationRiskAssessment;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.workflow.S7WorkflowAnalysisResult;
import io.acra.core.domain.workflow.WorkflowAuthorizationResolution;
import io.acra.core.domain.workflow.WorkflowTransitionAssessment;

public final class S7WorkflowOrchestrator {
    private final WorkflowAuthorizationResolver resolver = new WorkflowAuthorizationResolver();
    private final S7WorkflowAssessmentEvaluator assessmentEvaluator = new S7WorkflowAssessmentEvaluator();
    private final S7WorkflowFindingCandidateEvaluator candidateEvaluator;
    private final AuthorizationSeverityEvaluator severityEvaluator = new AuthorizationSeverityEvaluator();

    public S7WorkflowOrchestrator(ExecutionEvidenceStore evidenceStore) {
        if (evidenceStore == null) throw new IllegalArgumentException("evidenceStore required");
        this.candidateEvaluator = new S7WorkflowFindingCandidateEvaluator(
                new EvidenceReferenceValidator(evidenceStore));
    }

    public S7WorkflowAnalysisResult analyze(S7WorkflowAnalysisRequest request) {
        if (request == null) throw new IllegalArgumentException("request required");
        WorkflowAuthorizationResolution resolution = resolver.resolve(
                request.workflowPolicy(), request.authorizationPolicy(), request.workflowRequest());
        WorkflowTransitionAssessment assessment = assessmentEvaluator.evaluate(resolution);
        FindingCandidate candidate = candidateEvaluator.evaluate(resolution, assessment, request);
        AuthorizationRiskAssessment risk = severityEvaluator.evaluate(candidate, request.impactProfile());
        return new S7WorkflowAnalysisResult(resolution, assessment, candidate, risk);
    }
}
