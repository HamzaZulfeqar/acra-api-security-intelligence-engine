package io.acra.standalone.model;

import io.acra.core.domain.finding.ReviewedFinding;

import java.util.Objects;
import java.util.UUID;

public record StoredFindingReviewRecord(
        UUID sourceRunId,
        ReviewedFinding finding
) {
    public StoredFindingReviewRecord {
        Objects.requireNonNull(sourceRunId, "sourceRunId");
        Objects.requireNonNull(finding, "finding");
    }
}
