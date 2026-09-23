package io.acra.core.domain.authorization;

import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingConfidence;
import io.acra.core.domain.finding.FindingSeverity;
import java.util.List;

public record AuthorizationReport(
        String reportId,
        String projectId,
        String executionId,
        String testId,
        ContextStatus contextStatus,
        CorrelationState correlationState,
        FindingCandidateState candidateState,
        FindingSeverity severity,
        FindingConfidence confidence,
        List<String> dimensions,
        List<String> assessmentIds,
        List<String> evidenceIds,
        List<String> limitations) {

    public AuthorizationReport {
        dimensions = List.copyOf(dimensions == null ? List.of() : dimensions);
        assessmentIds = List.copyOf(assessmentIds == null ? List.of() : assessmentIds);
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
        limitations = List.copyOf(limitations == null ? List.of() : limitations);
    }
}
