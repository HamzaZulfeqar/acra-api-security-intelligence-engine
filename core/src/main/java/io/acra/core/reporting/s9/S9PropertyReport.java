package io.acra.core.reporting.s9;

import io.acra.core.domain.authorization.PropertyAuthorizationAssessment;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.engine.PolicyValidationEvaluator;
import io.acra.core.property.PropertyAccessObservation;
import io.acra.core.property.PropertyAuthorizationCoverageEntry;
import java.time.Instant;
import java.util.List;

public record S9PropertyReport(
        String reportId,
        String reportVersion,
        S9PropertyReportStatus status,
        Instant generatedAt,
        S9PropertyReportSummary summary,
        List<PolicyValidationEvaluator.PropertyPolicy> policies,
        List<PropertyAccessObservation> observations,
        List<PropertyAuthorizationAssessment> assessments,
        List<FindingCandidate> findingCandidates,
        List<PropertyAuthorizationCoverageEntry> coverageEntries,
        List<String> evidenceIds,
        List<String> limitations) {

    public S9PropertyReport {
        if (reportId == null || reportId.isBlank()) throw new IllegalArgumentException("reportId required");
        if (reportVersion == null || reportVersion.isBlank()) throw new IllegalArgumentException("reportVersion required");
        status = status == null ? S9PropertyReportStatus.NO_PROPERTY_EVIDENCE : status;
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
