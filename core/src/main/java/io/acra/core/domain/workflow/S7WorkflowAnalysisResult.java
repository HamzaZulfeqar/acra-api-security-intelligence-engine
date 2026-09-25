package io.acra.core.domain.workflow;

import io.acra.core.domain.finding.AuthorizationRiskAssessment;
import io.acra.core.domain.finding.FindingCandidate;

public record S7WorkflowAnalysisResult(
        WorkflowAuthorizationResolution resolution,
        WorkflowTransitionAssessment assessment,
        FindingCandidate findingCandidate,
        AuthorizationRiskAssessment riskAssessment) {
}
