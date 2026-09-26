package io.acra.standalone.service;

import io.acra.core.product.finding.FindingReviewCase;
import io.acra.core.reporting.finding.FindingBurpIssueDraft;
import io.acra.core.reporting.finding.FindingBurpIssueDraftGenerator;
import io.acra.core.reporting.finding.FindingReproductionExportArtifact;
import io.acra.core.reporting.finding.FindingReproductionJsonExporter;
import io.acra.core.reporting.finding.FindingReproductionPackage;
import io.acra.core.reporting.finding.FindingReproductionPackageGenerator;
import io.acra.core.reporting.finding.FindingReproductionSarifExporter;
import io.acra.standalone.model.StandaloneFindingReviewRecord;
import io.acra.standalone.store.LocalWorkspaceStore;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

public final class StandaloneFindingReproductionService {
    private final StandaloneFindingLifecycleService lifecycleService;
    private final FindingReproductionPackageGenerator generator = new FindingReproductionPackageGenerator();
    private final FindingReproductionJsonExporter jsonExporter = new FindingReproductionJsonExporter();
    private final FindingReproductionSarifExporter sarifExporter = new FindingReproductionSarifExporter();
    private final FindingBurpIssueDraftGenerator burpDraftGenerator = new FindingBurpIssueDraftGenerator();

    public StandaloneFindingReproductionService(LocalWorkspaceStore workspace) {
        this.lifecycleService = new StandaloneFindingLifecycleService(
                Objects.requireNonNull(workspace));
    }

    public FindingReproductionPackage reproduction(UUID projectId, String findingId) throws IOException {
        StandaloneFindingReviewRecord record = lifecycleService.finding(projectId, findingId);
        FindingReviewCase reviewCase = new FindingReviewCase(
                record.candidate(),
                record.risk(),
                record.finding());

        // Use state timestamp rather than wall-clock render time so unchanged reviewed state
        // produces a stable package and a stable JSON digest.
        return generator.generate(reviewCase, record.finding().updatedAt());
    }

    public FindingReproductionExportArtifact json(UUID projectId, String findingId) throws IOException {
        return jsonExporter.json(reproduction(projectId, findingId));
    }

    public FindingReproductionExportArtifact sarif(UUID projectId, String findingId) throws IOException {
        return sarifExporter.sarif(reproduction(projectId, findingId));
    }

    public FindingBurpIssueDraft burpDraft(UUID projectId, String findingId) throws IOException {
        return burpDraftGenerator.generate(reproduction(projectId, findingId));
    }
}

