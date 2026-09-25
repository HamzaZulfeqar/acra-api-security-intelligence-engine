package io.acra.core.reporting.s10;

import java.time.Instant;
import java.util.List;

public record S10BatchIndirectReport(
        String reportId,
        String reportVersion,
        S10BatchIndirectReportStatus status,
        Instant generatedAt,
        S10BatchIndirectReportSummary summary,
        List<S10ReportPolicyContext> policies,
        List<S10ReportObservation> observations,
        List<S10ReportAssessment> assessments,
        List<S10ReportFindingCandidate> findingCandidates,
        List<S10ReportCoverageContext> coverageEntries,
        List<String> evidenceIds,
        List<String> limitations) {

    public S10BatchIndirectReport {
        if (reportId == null || reportId.isBlank()) throw new IllegalArgumentException("reportId required");
        if (reportVersion == null || reportVersion.isBlank()) throw new IllegalArgumentException("reportVersion required");
        status = status == null ? S10BatchIndirectReportStatus.NO_AUTHORIZATION_EVIDENCE : status;
        if (generatedAt == null) throw new IllegalArgumentException("generatedAt required");
        if (summary == null) throw new IllegalArgumentException("summary required");
        policies = List.copyOf(policies == null ? List.of() : policies);
        observations = List.copyOf(observations == null ? List.of() : observations);
        assessments = List.copyOf(assessments == null ? List.of() : assessments);
        findingCandidates = List.copyOf(findingCandidates == null ? List.of() : findingCandidates);
        coverageEntries = List.copyOf(coverageEntries == null ? List.of() : coverageEntries);
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
        limitations = List.copyOf(limitations == null ? List.of() : limitations);
    }
}
