package io.acra.core.reporting.s10;

import java.time.Instant;
import java.util.List;

public record S10SessionReport(
        String reportId,
        String reportVersion,
        S10SessionReportStatus status,
        Instant generatedAt,
        S10SessionReportSummary summary,
        List<ObservationView> observations,
        List<CorrelationView> correlations,
        List<AssessmentView> assessments,
        List<CandidateView> findingCandidates,
        List<CoverageView> coverageEntries,
        List<String> evidenceIds,
        List<String> limitations) {

    public S10SessionReport {
        if (reportId == null || reportId.isBlank()) throw new IllegalArgumentException("reportId required");
        if (reportVersion == null || reportVersion.isBlank()) throw new IllegalArgumentException("reportVersion required");
        status = status == null ? S10SessionReportStatus.NO_SESSION_EVIDENCE : status;
        if (generatedAt == null) throw new IllegalArgumentException("generatedAt required");
        if (summary == null) throw new IllegalArgumentException("summary required");
        observations = List.copyOf(observations == null ? List.of() : observations);
        correlations = List.copyOf(correlations == null ? List.of() : correlations);
        assessments = List.copyOf(assessments == null ? List.of() : assessments);
        findingCandidates = List.copyOf(findingCandidates == null ? List.of() : findingCandidates);
        coverageEntries = List.copyOf(coverageEntries == null ? List.of() : coverageEntries);
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
        limitations = List.copyOf(limitations == null ? List.of() : limitations);
    }

    public record ObservationView(
            String observationId,
            String authContextRef,
            String principalId,
            String roleId,
            String tenantId,
            List<String> scopes,
            String authenticationType,
            String identityState,
            Instant observedAt,
            List<String> evidenceIds) {
        public ObservationView {
            scopes = List.copyOf(scopes == null ? List.of() : scopes);
            evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
        }
    }

    public record CorrelationView(
            String correlationId,
            String authContextRef,
            String previousObservationId,
            String currentObservationId,
            String state,
            boolean tokenRotated,
            boolean previousIdentityVerified,
            boolean currentIdentityVerified,
            List<String> driftDimensions,
            List<String> evidenceIds,
            List<String> reasons) {
        public CorrelationView {
            driftDimensions = List.copyOf(driftDimensions == null ? List.of() : driftDimensions);
            evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
            reasons = List.copyOf(reasons == null ? List.of() : reasons);
        }
    }

    public record AssessmentView(
            String assessmentId,
            String authContextRef,
            String state,
            boolean tokenRotated,
            boolean identityVerified,
            List<String> driftDimensions,
            List<String> evidenceIds,
            String rationale,
            List<String> reasons) {
        public AssessmentView {
            driftDimensions = List.copyOf(driftDimensions == null ? List.of() : driftDimensions);
            evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
            reasons = List.copyOf(reasons == null ? List.of() : reasons);
        }
    }

    public record CandidateView(
            String candidateId,
            String authContextRef,
            String state,
            String principalId,
            String tenantId,
            List<String> ruleReferences,
            List<String> dimensions,
            String confidence,
            List<String> evidenceIds,
            String rationale) {
        public CandidateView {
            ruleReferences = List.copyOf(ruleReferences == null ? List.of() : ruleReferences);
            dimensions = List.copyOf(dimensions == null ? List.of() : dimensions);
            evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
        }
    }

    public record CoverageView(
            String coverageId,
            String targetId,
            String authContextRef,
            String objective,
            String ruleReference,
            String disposition,
            int observationCount,
            int correlationCount,
            int assessmentCount,
            int findingCount) { }
}
