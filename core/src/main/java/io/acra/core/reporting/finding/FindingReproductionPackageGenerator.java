package io.acra.core.reporting.finding;

import io.acra.core.domain.finding.FindingLifecycleState;
import io.acra.core.product.finding.FindingReviewCase;
import io.acra.core.security.TokenFingerprint;
import java.time.Instant;
import java.util.List;
import java.util.TreeSet;

public final class FindingReproductionPackageGenerator {
    public static final String SCHEMA_VERSION = "s11-finding-reproduction-v1";

    public FindingReproductionPackage generate(
            FindingReviewCase reviewCase,
            Instant generatedAt) {
        if (reviewCase == null) throw new IllegalArgumentException("reviewCase required");
        if (generatedAt == null) throw new IllegalArgumentException("generatedAt required");

        var candidate = reviewCase.candidate();
        var finding = reviewCase.finding();

        TreeSet<String> evidence = new TreeSet<>(candidate.supportingEvidenceIds());
        evidence.addAll(finding.supportingEvidenceIds());

        List<FindingReviewTrailEntry> trail = finding.history().stream()
                .map(transition -> new FindingReviewTrailEntry(
                        transition.fromState(),
                        transition.toState(),
                        transition.occurredAt(),
                        transition.evidenceIds()))
                .toList();

        boolean confirmed = finding.state() == FindingLifecycleState.CONFIRMED
                || finding.state() == FindingLifecycleState.ACCEPTED_RISK;

        String identityMaterial = String.join("|",
                SCHEMA_VERSION,
                finding.projectId(),
                finding.findingId(),
                finding.state().name(),
                finding.fingerprint().fingerprint(),
                String.join(",", evidence),
                finding.history().stream()
                        .map(value -> value.transitionId())
                        .sorted()
                        .reduce("", (left, right) -> left + "|" + right));
        String reproductionId = "reproduction-"
                + TokenFingerprint.sha256(identityMaterial).substring(0, 24);

        return new FindingReproductionPackage(
                reproductionId,
                SCHEMA_VERSION,
                generatedAt,
                finding.projectId(),
                finding.findingId(),
                finding.candidateId(),
                finding.state(),
                finding.severity(),
                finding.confidence(),
                confirmed,
                finding.fingerprint().fingerprint(),
                candidate.endpoint(),
                candidate.resourceId(),
                candidate.principalId(),
                candidate.tenantRelationship(),
                candidate.expectedDecision(),
                candidate.observedDecision(),
                candidate.dimensions(),
                candidate.testIds(),
                candidate.executionIds(),
                candidate.observationIds(),
                candidate.assessmentIds(),
                List.copyOf(evidence),
                candidate.policyReferences(),
                trail,
                List.of(
                        "Burp Issue and SARIF adapters are separate output phases.",
                        "Evidence identifiers reference separately retained evidence.",
                        "Finding lifecycle state is human-review state, not scanner auto-confirmation.",
                        "No raw credentials are included.",
                        "No raw HTTP request or response body is fabricated by this package."));
    }
}
