package io.acra.core.reproduction;

import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingSeverity;
import java.util.List;

public final class ReproductionPackageProjector {

    public ReproductionPackage project(
            FindingCandidate candidate,
            FindingSeverity severity,
            String summary) {
        if (candidate == null) throw new IllegalArgumentException("candidate required");
        if (candidate.fingerprint() == null) throw new IllegalArgumentException("candidate fingerprint required");
        if (candidate.supportingEvidenceIds().isEmpty()) {
            throw new IllegalArgumentException("reproduction package requires supporting evidence");
        }

        return new ReproductionPackage(
                "acra-reproduction-package-v1",
                "",
                candidate.candidateId(),
                candidate.state(),
                true,
                candidate.projectId(),
                severity,
                candidate.confidence(),
                candidate.endpoint(),
                candidate.resourceId(),
                candidate.expectedDecision(),
                candidate.observedDecision(),
                candidate.dimensions(),
                candidate.policyReferences(),
                candidate.supportingEvidenceIds(),
                summary == null || summary.isBlank() ? candidate.rationale() : summary,
                List.of(
                        ReproductionExportTarget.JSON,
                        ReproductionExportTarget.SARIF,
                        ReproductionExportTarget.BURP_ISSUE),
                "");
    }
}
