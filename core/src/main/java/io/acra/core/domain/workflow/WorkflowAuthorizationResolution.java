package io.acra.core.domain.workflow;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.PolicyResolutionState;
import java.util.List;

public record WorkflowAuthorizationResolution(
        String resolutionId,
        String policyFingerprint,
        String workflowId,
        String principalId,
        String tenantId,
        String resourceId,
        String action,
        String fromState,
        String toState,
        List<String> matchedRuleIds,
        List<String> matchedBindingIds,
        List<String> delegationIds,
        AuthorizationDecision expectedDecision,
        AuthorizationDecision observedDecision,
        PolicyResolutionState state,
        List<String> evidenceIds,
        List<String> reasons) {

    public WorkflowAuthorizationResolution {
        matchedRuleIds = sorted(matchedRuleIds);
        matchedBindingIds = sorted(matchedBindingIds);
        delegationIds = sorted(delegationIds);
        expectedDecision = expectedDecision == null ? AuthorizationDecision.UNKNOWN : expectedDecision;
        observedDecision = observedDecision == null ? AuthorizationDecision.UNKNOWN : observedDecision;
        state = state == null ? PolicyResolutionState.UNKNOWN : state;
        evidenceIds = sorted(evidenceIds);
        reasons = sorted(reasons);
    }

    public boolean mismatchCandidate() {
        return expectedDecision == AuthorizationDecision.DENY
                && observedDecision == AuthorizationDecision.ALLOW
                && state == PolicyResolutionState.RESOLVED_DENY;
    }

    private static List<String> sorted(List<String> values) {
        return List.copyOf(values == null ? List.<String>of() : values).stream()
                .filter(v -> v != null && !v.isBlank())
                .distinct()
                .sorted()
                .toList();
    }
}
