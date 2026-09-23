package io.acra.core.reporting.s6;

import io.acra.core.domain.authorization.AuthorizationPolicyCoverage;
import io.acra.core.domain.authorization.AuthorizationPolicySnapshot;
import io.acra.core.domain.authorization.EffectiveAuthorizationMatrixEntry;
import io.acra.core.domain.authorization.PolicyConflictAssessment;
import io.acra.core.domain.finding.AuthorizationRiskAssessment;
import io.acra.core.domain.finding.FindingCandidate;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public record S6AuthorizationReport(
        String reportId,
        String reportVersion,
        S6AuthorizationReportStatus status,
        Instant generatedAt,
        AuthorizationPolicySnapshot policy,
        S6AuthorizationReportSummary summary,
        List<EffectiveAuthorizationMatrixEntry> effectiveAuthorization,
        List<PolicyConflictAssessment> policyConflicts,
        Map<String, AuthorizationPolicyCoverage> coverageByResolution,
        List<FindingCandidate> findingCandidates,
        List<AuthorizationRiskAssessment> riskAssessments,
        List<String> evidenceIds,
        List<String> limitations) {

    public S6AuthorizationReport {
        if (reportId == null || reportId.isBlank()) throw new IllegalArgumentException("reportId required");
        if (reportVersion == null || reportVersion.isBlank()) throw new IllegalArgumentException("reportVersion required");
        status = status == null ? S6AuthorizationReportStatus.POLICY_NOT_LOADED : status;
        if (generatedAt == null) throw new IllegalArgumentException("generatedAt required");
        if (summary == null) throw new IllegalArgumentException("summary required");
        effectiveAuthorization = List.copyOf(effectiveAuthorization == null ? List.of() : effectiveAuthorization);
        policyConflicts = List.copyOf(policyConflicts == null ? List.of() : policyConflicts);
        coverageByResolution = Map.copyOf(new TreeMap<>(
                coverageByResolution == null ? Map.of() : coverageByResolution));
        findingCandidates = List.copyOf(findingCandidates == null ? List.of() : findingCandidates);
        riskAssessments = List.copyOf(riskAssessments == null ? List.of() : riskAssessments);
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
        limitations = List.copyOf(limitations == null ? List.of() : limitations);
    }
}
