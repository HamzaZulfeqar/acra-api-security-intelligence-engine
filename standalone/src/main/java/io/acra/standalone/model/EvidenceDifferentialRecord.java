package io.acra.standalone.model;

import io.acra.core.analysis.ResponseComparisonMode;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record EvidenceDifferentialRecord(
        UUID leftSampleId,
        UUID rightSampleId,
        ResponseComparisonMode mode,
        boolean equivalent,
        List<String> changedSignals,
        boolean requestMethodEqual,
        boolean requestUrlEqual,
        int leftStatus,
        int rightStatus
) {
    public EvidenceDifferentialRecord {
        Objects.requireNonNull(leftSampleId, "leftSampleId");
        Objects.requireNonNull(rightSampleId, "rightSampleId");
        Objects.requireNonNull(mode, "mode");
        changedSignals = List.copyOf(changedSignals == null ? List.of() : changedSignals);
    }
}
