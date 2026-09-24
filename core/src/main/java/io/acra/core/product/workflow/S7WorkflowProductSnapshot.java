package io.acra.core.product.workflow;

import io.acra.core.domain.workflow.S7WorkflowAnalysisResult;
import io.acra.core.domain.workflow.WorkflowAuthorizationResolution;
import io.acra.core.domain.workflow.WorkflowCoverageSummary;
import io.acra.core.domain.workflow.WorkflowPolicySnapshot;
import io.acra.core.domain.workflow.WorkflowTransitionCoverageEntry;
import java.util.List;

public record S7WorkflowProductSnapshot(
        WorkflowPolicySnapshot policy,
        List<WorkflowAuthorizationResolution> resolutions,
        List<S7WorkflowAnalysisResult> analyses,
        List<WorkflowTransitionCoverageEntry> coverageEntries,
        WorkflowCoverageSummary coverageSummary) {

    public S7WorkflowProductSnapshot {
        resolutions = List.copyOf(resolutions == null ? List.of() : resolutions);
        analyses = List.copyOf(analyses == null ? List.of() : analyses);
        coverageEntries = List.copyOf(coverageEntries == null ? List.of() : coverageEntries);
        coverageSummary = coverageSummary == null
                ? new WorkflowCoverageSummary(0, 0, 0, 0, 0, 0) : coverageSummary;
    }

    public boolean hasPolicy() {
        return policy != null;
    }
}
