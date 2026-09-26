package io.acra.standalone.model;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record CoreProjectionSnapshot(
        UUID projectId,
        String policyFingerprint,
        int membershipCount,
        int roleAssignmentCount,
        int permissionCount,
        int ruleCount,
        List<CoreAuthorizationProjectionRecord> authorization,
        boolean workflowWorkspaceProjected,
        int workflowResolutionCount,
        boolean routingWorkspaceProjected,
        int routingAssessmentCount,
        boolean propertyWorkspaceProjected,
        int propertyAssessmentCount
) {
    public CoreProjectionSnapshot {
        Objects.requireNonNull(projectId, "projectId");
        policyFingerprint = policyFingerprint == null ? "" : policyFingerprint;
        authorization = List.copyOf(authorization == null ? List.of() : authorization);
        if (membershipCount < 0 || roleAssignmentCount < 0 || permissionCount < 0 || ruleCount < 0
                || workflowResolutionCount < 0 || routingAssessmentCount < 0 || propertyAssessmentCount < 0) {
            throw new IllegalArgumentException("projection counts must be non-negative");
        }
    }
}
