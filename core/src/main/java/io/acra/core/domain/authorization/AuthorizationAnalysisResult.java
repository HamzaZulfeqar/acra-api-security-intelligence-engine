package io.acra.core.domain.authorization;

import io.acra.core.domain.finding.AuthorizationRiskAssessment;
import io.acra.core.domain.finding.FindingCandidate;

public record AuthorizationAnalysisResult(
        AuthorizationContextAssessment contextAssessment,
        BolaAssessment bola,
        BflaAssessment bfla,
        TenantAuthorizationAssessment tenant,
        WorkflowAuthorizationAssessment workflow,
        PropertyAuthorizationAssessment property,
        AuthorizationCorrelationEnvelope correlation,
        FindingCandidate findingCandidate,
        AuthorizationRiskAssessment riskAssessment) {
}
