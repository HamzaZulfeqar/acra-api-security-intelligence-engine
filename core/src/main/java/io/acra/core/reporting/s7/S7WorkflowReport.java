package io.acra.core.reporting.s7;

import io.acra.core.domain.finding.AuthorizationRiskAssessment;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.workflow.WorkflowAuthorizationResolution;
import io.acra.core.domain.workflow.WorkflowPolicySnapshot;
import io.acra.core.domain.workflow.WorkflowTransitionAssessment;
import io.acra.core.domain.workflow.WorkflowTransitionCoverageEntry;
import java.time.Instant;
import java.util.List;

public record S7WorkflowReport(
        String reportId,
        String reportVersion,
        S7WorkflowReportStatus status,
        Instant generatedAt,
        WorkflowPolicySnapshot policy,
        S7WorkflowReportSummary summary,
        List<WorkflowAuthorizationResolution> resolutions,
        List<WorkflowTransitionAssessment> assessments,
        List<WorkflowTransitionCoverageEntry> coverageEntries,
        List<FindingCandidate> findingCandidates,
        List<AuthorizationRiskAssessment> riskAssessments,
        List<String> evidenceIds,
        List<String> limitations) {

    public S7WorkflowReport {
        if (reportId == null || reportId.isBlank()) throw new IllegalArgumentException("reportId required");
        if (reportVersion == null || reportVersion.isBlank()) throw new IllegalArgumentException("reportVersion required");
        status = status == null ? S7WorkflowReportStatus.POLICY_NOT_LOADED : status;
        if (generatedAt == null) throw new IllegalArgumentException("generatedAt required");
        if (summary == null) throw new IllegalArgumentException("summary required");
        resolutions = List.copyOf(resolutions == null ? List.of() : resolutions);
        assessments = List.copyOf(assessments == null ? List.of() : assessments);
        coverageEntries = List.copyOf(coverageEntries == null ? List.of() : coverageEntries);
        findingCandidates = List.copyOf(findingCandidates == null ? List.of() : findingCandidates);
        riskAssessments = List.copyOf(riskAssessments == null ? List.of() : riskAssessments);
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
        limitations = List.copyOf(limitations == null ? List.of() : limitations);
    }
}
