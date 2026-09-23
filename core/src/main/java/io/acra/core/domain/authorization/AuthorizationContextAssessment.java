package io.acra.core.domain.authorization;

import io.acra.core.security.UniversalRedactor;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public record AuthorizationContextAssessment(
        String assessmentId,
        AuthorizationContext normalizedContext,
        String endpoint,
        String policyReference,
        String projectId,
        String observationId,
        String executionId,
        String testId,
        AuthorizationContextCompleteness completeness,
        Map<String, AuthorizationFactState> facts,
        boolean evidenceVerified,
        ContextStatus resolutionStatus,
        List<String> reasons) {

    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public AuthorizationContextAssessment {
        assessmentId = safe(assessmentId);
        endpoint = safe(endpoint);
        policyReference = safe(policyReference);
        projectId = safe(projectId);
        observationId = safe(observationId);
        executionId = safe(executionId);
        testId = safe(testId);
        if (completeness == null) {
            completeness = new AuthorizationContextCompleteness(false, false, false, false, false, false,
                    false, false, false, false, false, false);
        }
        facts = Map.copyOf(new TreeMap<>(facts == null ? Map.of() : facts));
        resolutionStatus = resolutionStatus == null ? ContextStatus.UNKNOWN : resolutionStatus;
        reasons = List.copyOf(reasons == null ? List.of() : reasons).stream()
                .map(AuthorizationContextAssessment::safe).distinct().sorted().toList();
    }

    public boolean readyForObjectLevel() {
        return evidenceVerified && resolutionStatus == ContextStatus.RESOLVED && completeness.objectLevelComplete();
    }

    public boolean readyForFunctionLevel() {
        return evidenceVerified && resolutionStatus == ContextStatus.RESOLVED && completeness.functionLevelComplete();
    }

    public boolean readyForTenantLevel() {
        return evidenceVerified && resolutionStatus == ContextStatus.RESOLVED && completeness.tenantLevelComplete();
    }

    public boolean readyForWorkflowLevel() {
        return evidenceVerified && resolutionStatus == ContextStatus.RESOLVED && completeness.workflowLevelComplete();
    }

    public boolean readyForPropertyLevel() {
        return evidenceVerified && resolutionStatus == ContextStatus.RESOLVED && completeness.propertyLevelComplete();
    }

    private static String safe(String value) {
        return REDACTOR.redactText(value == null ? "" : value);
    }
}
