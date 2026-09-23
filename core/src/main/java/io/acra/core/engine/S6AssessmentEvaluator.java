package io.acra.core.engine;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.EffectiveAuthorizationResolution;
import io.acra.core.domain.authorization.PolicyResolutionState;
import io.acra.core.domain.authorization.RbacAssessment;
import io.acra.core.domain.authorization.RbacAssessmentState;
import io.acra.core.domain.authorization.TenantIsolationAssessment;
import io.acra.core.domain.authorization.TenantIsolationAssessmentState;
import io.acra.core.domain.authorization.TenantRelationship;
import io.acra.core.security.TokenFingerprint;

public final class S6AssessmentEvaluator {

    public TenantIsolationAssessment tenant(EffectiveAuthorizationResolution resolution) {
        if (resolution == null) throw new IllegalArgumentException("resolution required");
        TenantIsolationAssessmentState state;
        String rationale;
        if (resolution.state() == PolicyResolutionState.CONFLICTING) {
            state = TenantIsolationAssessmentState.CONFLICTING;
            rationale = "Tenant authorization policy is conflicting";
        } else if (resolution.expectedDecision() == AuthorizationDecision.UNKNOWN) {
            state = TenantIsolationAssessmentState.INCONCLUSIVE;
            rationale = "Tenant authorization expectation is unresolved";
        } else if (resolution.tenantRelationship() != TenantRelationship.CROSS_TENANT) {
            state = TenantIsolationAssessmentState.NOT_APPLICABLE;
            rationale = "Relationship is explicitly same-tenant, global, shared, delegated, or otherwise non-cross-tenant";
        } else if (resolution.expectedDecision() == AuthorizationDecision.DENY
                && resolution.observedDecision() == AuthorizationDecision.ALLOW) {
            state = TenantIsolationAssessmentState.CANDIDATE;
            rationale = "Observed cross-tenant allow conflicts with resolved expected deny";
        } else {
            state = TenantIsolationAssessmentState.NO_VIOLATION;
            rationale = "Observed cross-tenant behavior does not establish an isolation mismatch";
        }
        return new TenantIsolationAssessment(id("tenant", resolution), resolution.tenantRelationship(),
                resolution.expectedDecision(), resolution.observedDecision(), state,
                resolution.evidenceIds(), rationale);
    }

    public RbacAssessment rbac(EffectiveAuthorizationResolution resolution) {
        if (resolution == null) throw new IllegalArgumentException("resolution required");
        RbacAssessmentState state;
        String rationale;
        if (resolution.state() == PolicyResolutionState.CONFLICTING) {
            state = RbacAssessmentState.CONFLICTING;
            rationale = "Effective RBAC policy contains unresolved conflicts";
        } else if (resolution.expectedDecision() == AuthorizationDecision.UNKNOWN) {
            state = RbacAssessmentState.INCONCLUSIVE;
            rationale = "Effective RBAC permission could not be resolved";
        } else if (resolution.expectedDecision() == AuthorizationDecision.DENY
                && resolution.observedDecision() == AuthorizationDecision.ALLOW) {
            state = RbacAssessmentState.CANDIDATE;
            rationale = "Observed allow conflicts with effective RBAC deny";
        } else {
            state = RbacAssessmentState.NO_VIOLATION;
            rationale = "Observed behavior is compatible with the resolved RBAC expectation";
        }
        return new RbacAssessment(id("rbac", resolution), resolution.effectiveRoleIds(),
                resolution.permissionIds(), resolution.expectedDecision(), resolution.observedDecision(),
                state, resolution.evidenceIds(), rationale);
    }

    private String id(String prefix, EffectiveAuthorizationResolution resolution) {
        return "s6-" + prefix + "-" + TokenFingerprint.sha256(resolution.resolutionId()).substring(0, 24);
    }
}
