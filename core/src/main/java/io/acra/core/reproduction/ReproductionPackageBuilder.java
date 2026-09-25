package io.acra.core.reproduction;

import io.acra.core.domain.authorization.AuthorizationReport;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingConfidence;
import io.acra.core.security.TokenFingerprint;
import java.util.List;

public final class ReproductionPackageBuilder {

    public ReproductionPackage build(
            FindingCandidate candidate,
            AuthorizationReport report) {
        if (candidate == null || report == null) {
            throw new IllegalArgumentException("candidate/report required");
        }
        if (!candidate.projectId().equals(report.projectId())) {
            throw new IllegalArgumentException("candidate/report project mismatch");
        }
        if (candidate.state() != report.candidateState()) {
            throw new IllegalArgumentException("candidate/report state mismatch");
        }
        if (!candidate.dimensions().equals(report.dimensions())) {
            throw new IllegalArgumentException("candidate/report dimension mismatch");
        }
        if (!candidate.assessmentIds().equals(report.assessmentIds())) {
            throw new IllegalArgumentException("candidate/report assessment mismatch");
        }
        if (!candidate.supportingEvidenceIds().equals(report.evidenceIds())) {
            throw new IllegalArgumentException("candidate/report evidence mismatch");
        }

        String principalFingerprint = candidate.principalId().isBlank()
                ? ""
                : TokenFingerprint.sha256(candidate.principalId());
        String findingFingerprint = candidate.fingerprint() == null
                ? ""
                : candidate.fingerprint().fingerprint();

        return new ReproductionPackage(
                "",
                ReproductionPackage.VERSION,
                report.reportId(),
                candidate.projectId(),
                candidate.candidateId(),
                candidate.state(),
                true,
                candidate.state() == io.acra.core.domain.finding.FindingCandidateState.CANDIDATE,
                report.severity(),
                report.confidence() == null ? FindingConfidence.INSUFFICIENT : report.confidence(),
                candidate.endpoint(),
                candidate.resourceId(),
                principalFingerprint,
                candidate.tenantRelationship(),
                candidate.expectedDecision(),
                candidate.observedDecision(),
                candidate.dimensions(),
                candidate.assessmentIds(),
                candidate.supportingEvidenceIds(),
                candidate.policyReferences(),
                report.limitations(),
                findingFingerprint,
                "");
    }
}
