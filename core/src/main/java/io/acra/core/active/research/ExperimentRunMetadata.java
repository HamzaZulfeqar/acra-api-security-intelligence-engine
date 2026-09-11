package io.acra.core.active.research;

import io.acra.core.active.model.ConfigurationSnapshot;
import io.acra.core.domain.common.Validation;
import java.time.Instant;
import java.util.List;

public record ExperimentRunMetadata(
        String experimentId,
        String datasetId,
        String labVersion,
        long seed,
        List<String> testOrdering,
        List<String> mutationOrdering,
        ConfigurationSnapshot configuration,
        List<String> executionIds,
        Instant createdAt) {
    public ExperimentRunMetadata {
        experimentId = Validation.requireNonBlank(experimentId, "experimentId");
        datasetId = Validation.requireNonBlank(datasetId, "datasetId");
        labVersion = Validation.requireNonBlank(labVersion, "labVersion");
        testOrdering = List.copyOf(testOrdering == null ? List.of() : testOrdering);
        mutationOrdering = List.copyOf(mutationOrdering == null ? List.of() : mutationOrdering);
        executionIds = List.copyOf(executionIds == null ? List.of() : executionIds);
        if (configuration == null || createdAt == null) throw new IllegalArgumentException("experiment metadata required");
    }
}
