package io.acra.core.engine;

import io.acra.core.active.evidence.ExecutionEvidenceStore;
import io.acra.core.domain.authorization.AuthorizationAnalysisResult;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.AuthorizationPolicyCoverage;
import io.acra.core.domain.authorization.EffectiveAuthorizationMatrixEntry;
import io.acra.core.domain.authorization.EffectiveAuthorizationResolution;
import io.acra.core.domain.authorization.PolicyConflictAssessment;
import io.acra.core.domain.authorization.PolicyResolutionState;
import io.acra.core.domain.authorization.RbacAssessment;
import io.acra.core.domain.authorization.RoleEscalationAssessment;
import io.acra.core.domain.authorization.S6AuthorizationAnalysisResult;
import io.acra.core.domain.authorization.TenantIsolationAssessment;
import io.acra.core.domain.finding.AuthorizationRiskAssessment;
import io.acra.core.domain.finding.FindingCandidate;

public final class S6AuthorizationOrchestrator {
    private final AuthorizationOrchestrator s5;
    private final EffectiveAuthorizationResolver policyResolver = new EffectiveAuthorizationResolver();
    private final S6AssessmentEvaluator assessmentEvaluator = new S6AssessmentEvaluator();
    private final S6AdvancedAssessmentEvaluator advancedEvaluator = new S6AdvancedAssessmentEvaluator();
    private final S6FindingCandidateEvaluator candidateEvaluator = new S6FindingCandidateEvaluator();
    private final AuthorizationSeverityEvaluator severityEvaluator = new AuthorizationSeverityEvaluator();

    public S6AuthorizationOrchestrator(ExecutionEvidenceStore evidenceStore) {
        this.s5 = new AuthorizationOrchestrator(evidenceStore);
    }

    public S6AuthorizationAnalysisResult analyze(S6AuthorizationAnalysisRequest request) {
        if (request == null) throw new IllegalArgumentException("request required");

        AuthorizationAnalysisResult base = s5.analyze(request.s5Request());
        EffectiveAuthorizationResolution effective = policyResolver.resolve(
                request.policySnapshot(), request.effectiveRequest());
        TenantIsolationAssessment tenant = assessmentEvaluator.tenant(effective);
        RbacAssessment rbac = assessmentEvaluator.rbac(effective);
        RoleEscalationAssessment escalation = advancedEvaluator.roleEscalation(
                effective, request.privilegedAction(), request.requiredRoleId());
        PolicyConflictAssessment conflict = advancedEvaluator.conflict(effective);

        AuthorizationPolicyCoverage coverage = coverage(request, effective);
        EffectiveAuthorizationMatrixEntry matrix = EffectiveAuthorizationMatrixEntry.from(
                request.effectiveRequest(), effective);
        FindingCandidate candidate = candidateEvaluator.evaluate(base.findingCandidate(), effective,
                tenant, rbac, escalation, conflict);
        AuthorizationRiskAssessment risk = severityEvaluator.evaluate(candidate, request.s5Request().impactProfile());

        return new S6AuthorizationAnalysisResult(base, effective, tenant, rbac, escalation, conflict,
                coverage, matrix, candidate, risk);
    }

    private AuthorizationPolicyCoverage coverage(S6AuthorizationAnalysisRequest request,
                                                 EffectiveAuthorizationResolution resolution) {
        boolean tenant = !request.effectiveRequest().resourceTenantId().isBlank();
        boolean role = !resolution.effectiveRoleIds().isEmpty();
        boolean hierarchy = request.policySnapshot().roleInheritances().isEmpty() || role;
        boolean permission = !resolution.permissionIds().isEmpty();
        boolean policyRule = !request.policySnapshot().rules().isEmpty()
                || request.policySnapshot().defaultDecision() == AuthorizationDecision.ALLOW
                || request.policySnapshot().defaultDecision() == AuthorizationDecision.DENY;
        boolean decision = resolution.state() == PolicyResolutionState.RESOLVED_ALLOW
                || resolution.state() == PolicyResolutionState.RESOLVED_DENY;
        return new AuthorizationPolicyCoverage(tenant, role, hierarchy, permission, policyRule, decision);
    }
}
