package io.acra.core.engine;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.EffectiveAuthorizationResolution;
import io.acra.core.domain.authorization.PolicyConflictAssessment;
import io.acra.core.domain.authorization.PolicyResolutionState;
import io.acra.core.domain.authorization.RoleEscalationAssessment;
import io.acra.core.domain.authorization.RoleEscalationAssessmentState;
import io.acra.core.security.TokenFingerprint;

public final class S6AdvancedAssessmentEvaluator {

    public RoleEscalationAssessment roleEscalation(EffectiveAuthorizationResolution resolution,
                                                    boolean privilegedAction, String requiredRoleId) {
        if (resolution == null) throw new IllegalArgumentException("resolution required");
        RoleEscalationAssessmentState state;
        String rationale;
        if (!privilegedAction) {
            state = RoleEscalationAssessmentState.NOT_APPLICABLE;
            rationale = "The caller did not classify this action as privileged";
        } else if (resolution.state() == PolicyResolutionState.CONFLICTING) {
            state = RoleEscalationAssessmentState.CONFLICTING;
            rationale = "Role/policy conflict prevents escalation assessment";
        } else if (resolution.expectedDecision() == AuthorizationDecision.UNKNOWN) {
            state = RoleEscalationAssessmentState.INCONCLUSIVE;
            rationale = "Required authorization expectation is unresolved";
        } else if (resolution.expectedDecision() == AuthorizationDecision.DENY
                && resolution.observedDecision() == AuthorizationDecision.ALLOW) {
            state = RoleEscalationAssessmentState.CANDIDATE;
            rationale = "Explicitly privileged action was observed allowed while effective policy resolved deny";
        } else {
            state = RoleEscalationAssessmentState.NO_VIOLATION;
            rationale = "No privilege-boundary mismatch was established";
        }
        return new RoleEscalationAssessment(id("escalation", resolution), privilegedAction,
                requiredRoleId, resolution.effectiveRoleIds(), resolution.expectedDecision(),
                resolution.observedDecision(), state, resolution.evidenceIds(), rationale);
    }

    public PolicyConflictAssessment conflict(EffectiveAuthorizationResolution resolution) {
        if (resolution == null) throw new IllegalArgumentException("resolution required");
        boolean conflict = resolution.state() == PolicyResolutionState.CONFLICTING;
        return new PolicyConflictAssessment(id("conflict", resolution), conflict, resolution.state(),
                resolution.reasons(), resolution.evidenceIds());
    }

    private String id(String prefix, EffectiveAuthorizationResolution resolution) {
        return "s6-" + prefix + "-" + TokenFingerprint.sha256(resolution.resolutionId()).substring(0, 24);
    }
}
