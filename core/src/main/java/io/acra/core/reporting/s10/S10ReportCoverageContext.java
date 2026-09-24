package io.acra.core.reporting.s10;

import io.acra.core.coverage.S10CoverageDisposition;
import io.acra.core.coverage.S10CoverageFamily;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.FindingCandidateState;
import java.util.List;

public record S10ReportCoverageContext(
        String coverageId,
        S10CoverageFamily family,
        String policyReference,
        String endpoint,
        String resourceId,
        String action,
        String roleId,
        String tenantId,
        AuthorizationDecision expectedDecision,
        S10CoverageDisposition disposition,
        List<String> observationIds,
        List<String> assessmentIds,
        List<String> findingCandidateIds,
        List<FindingCandidateState> findingStates) {

    public S10ReportCoverageContext {
        coverageId = required(coverageId, "coverageId");
        if (family == null) throw new IllegalArgumentException("family required");
        policyReference = required(policyReference, "policyReference");
        endpoint = required(endpoint, "endpoint");
        resourceId = required(resourceId, "resourceId");
        action = required(action, "action");
        roleId = normalized(roleId);
        tenantId = normalized(tenantId);
        expectedDecision = expectedDecision == null ? AuthorizationDecision.UNKNOWN : expectedDecision;
        if (disposition == null) throw new IllegalArgumentException("disposition required");
        observationIds = List.copyOf(observationIds == null ? List.of() : observationIds);
        assessmentIds = List.copyOf(assessmentIds == null ? List.of() : assessmentIds);
        findingCandidateIds = List.copyOf(findingCandidateIds == null ? List.of() : findingCandidateIds);
        findingStates = List.copyOf(findingStates == null ? List.of() : findingStates);
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " required");
        return value.strip();
    }

    private static String normalized(String value) {
        return value == null || value.isBlank() ? "UNKNOWN" : value.strip();
    }
}
