package io.acra.core.product.reproduction;

import io.acra.core.reporting.s12.S12BurpIssueProjection;
import io.acra.core.reporting.s12.S12ReproductionExportArtifact;
import io.acra.core.reporting.s12.S12ReproductionPackage;

public record S12ReproductionProductEntry(
        S12ReproductionPackage reproductionPackage,
        S12ReproductionExportArtifact jsonExport,
        S12ReproductionExportArtifact sarifExport,
        S12BurpIssueProjection burpProjection) {

    public S12ReproductionProductEntry {
        if (reproductionPackage == null || jsonExport == null || sarifExport == null || burpProjection == null) {
            throw new IllegalArgumentException("complete Sprint 12 product entry required");
        }
        if (!reproductionPackage.sourceCandidateId().equals(burpProjection.candidateId())) {
            throw new IllegalArgumentException("Burp projection candidate mismatch");
        }
        if (burpProjection.publishable()) {
            throw new IllegalArgumentException("core product workspace cannot contain publishable Burp projection");
        }
    }
}
