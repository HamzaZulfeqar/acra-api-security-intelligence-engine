package io.acra.core.reproduction;

import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.security.TokenFingerprint;

public record ReproductionInteroperabilityBundle(
        String bundleId,
        ReproductionPackage reproductionPackage,
        ReproductionExportArtifact jsonArtifact,
        ReproductionExportArtifact sarifArtifact,
        BurpIssueProjection burpIssue,
        String fingerprint) {

    public ReproductionInteroperabilityBundle {
        if (reproductionPackage == null || jsonArtifact == null
                || sarifArtifact == null || burpIssue == null) {
            throw new IllegalArgumentException("package and all three projections required");
        }
        if (reproductionPackage.candidateState() != FindingCandidateState.CANDIDATE) {
            throw new IllegalArgumentException("interoperability bundle requires CANDIDATE package");
        }
        if (!reproductionPackage.reviewOnly() || !burpIssue.reviewOnly()) {
            throw new IllegalArgumentException("interoperability bundle must remain review-only");
        }
        if (jsonArtifact.target() != ReproductionExportTarget.JSON) {
            throw new IllegalArgumentException("JSON artifact target mismatch");
        }
        if (sarifArtifact.target() != ReproductionExportTarget.SARIF) {
            throw new IllegalArgumentException("SARIF artifact target mismatch");
        }
        if (!burpIssue.packageId().equals(reproductionPackage.packageId())) {
            throw new IllegalArgumentException("Burp issue package lineage mismatch");
        }
        requireContains(jsonArtifact.content(), reproductionPackage.packageId(), "JSON package lineage");
        requireContains(jsonArtifact.content(), reproductionPackage.candidateId(), "JSON candidate lineage");
        requireContains(sarifArtifact.content(), reproductionPackage.packageId(), "SARIF package lineage");
        requireContains(sarifArtifact.content(), reproductionPackage.candidateId(), "SARIF candidate lineage");
        for (String evidenceId : reproductionPackage.evidenceIds()) {
            requireContains(jsonArtifact.content(), evidenceId, "JSON evidence lineage");
            requireContains(sarifArtifact.content(), evidenceId, "SARIF evidence lineage");
            if (!burpIssue.evidenceIds().contains(evidenceId)) {
                throw new IllegalArgumentException("Burp evidence lineage mismatch");
            }
        }

        String material = String.join("|",
                reproductionPackage.packageId(),
                reproductionPackage.fingerprint(),
                jsonArtifact.sha256(),
                sarifArtifact.sha256(),
                burpIssue.projectionId(),
                burpIssue.fingerprint());
        String expectedId = "interop-" + TokenFingerprint.sha256(material).substring(0, 24);
        bundleId = bundleId == null || bundleId.isBlank() ? expectedId : bundleId.strip();
        if (!bundleId.equals(expectedId)) {
            throw new IllegalArgumentException("bundleId mismatch");
        }
        String calculated = TokenFingerprint.sha256(bundleId + "|" + material);
        fingerprint = fingerprint == null || fingerprint.isBlank() ? calculated : fingerprint.strip();
        if (!fingerprint.equals(calculated)) {
            throw new IllegalArgumentException("bundle fingerprint mismatch");
        }
    }

    private static void requireContains(String value, String expected, String name) {
        if (value == null || !value.contains(expected)) {
            throw new IllegalArgumentException(name + " missing");
        }
    }
}
