package io.acra.core.domain.authorization;

import io.acra.core.domain.finding.AuthorizationRiskAssessment;
import io.acra.core.domain.finding.FindingCandidate;

public record S6AuthorizationAnalysisResult(
        AuthorizationAnalysisResult s5Result,
        EffectiveAuthorizationResolution effectiveResolution,
        TenantIsolationAssessment tenantIsolation,
        RbacAssessment rbac,
        RoleEscalationAssessment roleEscalation,
        PolicyConflictAssessment policyConflict,
        AuthorizationPolicyCoverage coverage,
        EffectiveAuthorizationMatrixEntry matrixEntry,
        FindingCandidate findingCandidate,
        AuthorizationRiskAssessment riskAssessment) {
}
