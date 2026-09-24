package io.acra.core.reporting.s12;

import io.acra.core.domain.finding.FindingCandidate;
import java.util.List;

public final class S12ReproductionPackageFactory {

    public S12ReproductionPackage from(FindingCandidate candidate) {
        if (candidate == null) throw new IllegalArgumentException("candidate required");
        return new S12ReproductionPackage(
                "",
                S12ReproductionPackage.VERSION,
                candidate.candidateId(),
                candidate.state(),
                candidate.projectId(),
                candidate.endpoint(),
                candidate.resourceId(),
                candidate.expectedDecision(),
                candidate.observedDecision(),
                candidate.dimensions(),
                candidate.supportingEvidenceIds(),
                candidate.policyReferences(),
                candidate.confidence(),
                List.of(
                        "Review-only export; candidate is not a confirmed vulnerability.",
                        "Raw credentials, request bodies, principal identifiers and rationale are excluded.",
                        "Phase 1 export does not perform active replay or publish a Burp Scanner issue."),
                "");
    }
}
