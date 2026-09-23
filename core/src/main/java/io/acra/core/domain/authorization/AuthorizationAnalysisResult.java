package io.acra.core.domain.authorization;

import io.acra.core.domain.finding.AuthorizationSeverity;
import io.acra.core.domain.finding.FindingCandidate;

/** Complete S5 software result for one S4 Observation. */
public record AuthorizationAnalysisResult(
        AuthorizationContextNormalizationResult normalization,
        BolaAssessment bola,
        BflaAssessment bfla,
        TenantAuthorizationAssessment tenant,
        WorkflowAuthorizationAssessment workflow,
        PropertyAuthorizationAssessment property,
        AuthorizationAssessmentAggregate aggregate,
        FindingCandidate candidate,
        AuthorizationSeverity severity) {
    public AuthorizationAnalysisResult {
        if (normalization == null) throw new IllegalArgumentException("normalization required");
        if (candidate == null) throw new IllegalArgumentException("candidate required");
        if (severity == null) throw new IllegalArgumentException("severity required");
    }
}
