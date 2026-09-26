package io.acra.standalone.model;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.BflaAssessmentStatus;
import io.acra.core.domain.authorization.BolaAssessmentStatus;
import io.acra.core.domain.authorization.PolicyResolutionState;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record CoreAuthorizationProjectionRecord(
        UUID expectationId,
        String endpoint,
        String action,
        String principalId,
        AuthorizationDecision configuredDecision,
        AuthorizationDecision resolvedDecision,
        PolicyResolutionState resolutionState,
        List<String> effectiveRoleIds,
        List<String> resolutionReasons,
        BolaAssessmentStatus bolaStatus,
        BflaAssessmentStatus bflaStatus
) {
    public CoreAuthorizationProjectionRecord {
        Objects.requireNonNull(expectationId, "expectationId");
        endpoint = endpoint == null ? "" : endpoint;
        action = action == null ? "" : action;
        principalId = principalId == null ? "" : principalId;
        configuredDecision = configuredDecision == null ? AuthorizationDecision.UNKNOWN : configuredDecision;
        resolvedDecision = resolvedDecision == null ? AuthorizationDecision.UNKNOWN : resolvedDecision;
        resolutionState = resolutionState == null ? PolicyResolutionState.UNKNOWN : resolutionState;
        effectiveRoleIds = List.copyOf(effectiveRoleIds == null ? List.of() : effectiveRoleIds);
        resolutionReasons = List.copyOf(resolutionReasons == null ? List.of() : resolutionReasons);
        bolaStatus = bolaStatus == null ? BolaAssessmentStatus.INCONCLUSIVE : bolaStatus;
        bflaStatus = bflaStatus == null ? BflaAssessmentStatus.INCONCLUSIVE : bflaStatus;
    }
}
