package io.acra.core.reporting.reproduction;

import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingFingerprint;
import java.util.List;

public final class ReproductionPackageFactory {
    private static final String VERSION = "acra-reproduction-v1";

    public ReproductionPackage from(FindingCandidate candidate) {
        if (candidate == null) throw new IllegalArgumentException("candidate required");

        FindingFingerprint fingerprint = candidate.fingerprint() == null
                ? FindingFingerprint.of(
                        candidate.endpoint(),
                        candidate.resourceId(),
                        candidate.principalId(),
                        candidate.tenantRelationship(),
                        String.join("+", candidate.dimensions()),
                        candidate.state().name())
                : candidate.fingerprint();

        return new ReproductionPackage(
                "",
                VERSION,
                candidate.candidateId(),
                candidate.state(),
                candidate.endpoint(),
                candidate.resourceId(),
                candidate.expectedDecision(),
                candidate.observedDecision(),
                candidate.dimensions(),
                candidate.supportingEvidenceIds(),
                candidate.policyReferences(),
                fingerprint.fingerprint(),
                List.of(
                        "Review artifact only; no automatic vulnerability confirmation.",
                        "Raw request/response bodies, credentials, principal identifiers and tenant identifiers are excluded.",
                        "Burp issue submission requires a separately verified Burp runtime adapter."));
    }
}
