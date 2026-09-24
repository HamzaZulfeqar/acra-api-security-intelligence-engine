package io.acra.core.reproduction;

import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingSeverity;

public final class ReproductionInteroperabilityService {
    private final ReproductionPackageProjector packageProjector = new ReproductionPackageProjector();
    private final ReproductionJsonExporter jsonExporter = new ReproductionJsonExporter();
    private final ReproductionSarifExporter sarifExporter = new ReproductionSarifExporter();
    private final BurpIssueProjector burpIssueProjector = new BurpIssueProjector();

    public ReproductionInteroperabilityBundle project(
            FindingCandidate candidate,
            FindingSeverity severity,
            String summary,
            String origin) {
        ReproductionPackage reproductionPackage =
                packageProjector.project(candidate, severity, summary);
        ReproductionExportArtifact json = jsonExporter.export(reproductionPackage);
        ReproductionExportArtifact sarif = sarifExporter.export(reproductionPackage);
        BurpIssueProjection burp = burpIssueProjector.project(reproductionPackage, origin);
        return new ReproductionInteroperabilityBundle(
                "", reproductionPackage, json, sarif, burp, "");
    }
}
