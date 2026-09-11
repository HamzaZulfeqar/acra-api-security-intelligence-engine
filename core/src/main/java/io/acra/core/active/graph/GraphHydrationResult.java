package io.acra.core.active.graph;

import io.acra.core.domain.common.Validation;
import java.util.List;

public record GraphHydrationResult(
        GraphHydrationStatus status,
        String executionId,
        String testId,
        String observationId,
        List<String> nodeIds,
        List<String> edgeIds,
        List<String> evidenceIds,
        List<String> reasons) {
    public GraphHydrationResult {
        if (status == null) throw new IllegalArgumentException("graph hydration status required");
        executionId = Validation.requireNonBlank(executionId, "executionId");
        testId = Validation.requireNonBlank(testId, "testId");
        observationId = observationId == null ? "" : observationId;
        nodeIds = List.copyOf(nodeIds == null ? List.of() : nodeIds);
        edgeIds = List.copyOf(edgeIds == null ? List.of() : edgeIds);
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
        reasons = List.copyOf(reasons == null ? List.of() : reasons);
        if (status != GraphHydrationStatus.HYDRATED
                && status != GraphHydrationStatus.CONFLICTING_CONTEXT
                && reasons.isEmpty()) {
            throw new IllegalArgumentException("non-hydrated graph result requires a reason");
        }
    }

    public boolean graphUpdated() {
        return status == GraphHydrationStatus.HYDRATED || status == GraphHydrationStatus.CONFLICTING_CONTEXT;
    }
}
