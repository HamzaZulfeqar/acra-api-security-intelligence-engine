package io.acra.core.product.workflow;

import io.acra.core.domain.workflow.S7WorkflowAnalysisResult;
import io.acra.core.domain.workflow.WorkflowAuthorizationResolution;
import io.acra.core.domain.workflow.WorkflowPolicySnapshot;
import io.acra.core.domain.workflow.WorkflowTransitionCoverageEntry;
import io.acra.core.domain.workflow.WorkflowTransitionCoverageMatrix;
import io.acra.core.reporting.s7.S7WorkflowExportArtifact;
import io.acra.core.reporting.s7.S7WorkflowReport;
import io.acra.core.reporting.s7.S7WorkflowReportExporter;
import io.acra.core.reporting.s7.S7WorkflowReportGenerator;
import java.time.Instant;
import java.util.List;
import java.util.TreeMap;

public final class S7WorkflowWorkspace {
    private WorkflowPolicySnapshot policy;
    private final TreeMap<String, WorkflowAuthorizationResolution> resolutions = new TreeMap<>();
    private final TreeMap<String, S7WorkflowAnalysisResult> analyses = new TreeMap<>();
    private final TreeMap<String, WorkflowTransitionCoverageEntry> coverage = new TreeMap<>();
    private final S7WorkflowReportGenerator reportGenerator = new S7WorkflowReportGenerator();
    private final S7WorkflowReportExporter reportExporter = new S7WorkflowReportExporter();

    public synchronized void loadPolicy(WorkflowPolicySnapshot snapshot) {
        if (snapshot == null) throw new IllegalArgumentException("workflow policy snapshot required");
        policy = snapshot;
    }

    public synchronized void recordResolution(WorkflowAuthorizationResolution resolution) {
        if (resolution == null) throw new IllegalArgumentException("workflow resolution required");
        resolutions.put(resolution.resolutionId(), resolution);
    }

    public synchronized void recordAnalysis(S7WorkflowAnalysisResult result) {
        if (result == null || result.resolution() == null) {
            throw new IllegalArgumentException("S7 workflow analysis with resolution required");
        }
        analyses.put(result.resolution().resolutionId(), result);
        recordResolution(result.resolution());
    }

    public synchronized void replaceCoverage(WorkflowTransitionCoverageMatrix matrix) {
        if (matrix == null) throw new IllegalArgumentException("workflow coverage matrix required");
        coverage.clear();
        for (WorkflowTransitionCoverageEntry entry : matrix.entries()) {
            coverage.put(entry.coverageId(), entry);
        }
    }

    public synchronized void recordCoverage(WorkflowTransitionCoverageEntry entry) {
        if (entry == null) throw new IllegalArgumentException("workflow coverage entry required");
        coverage.put(entry.coverageId(), entry);
    }

    public synchronized void clearRuntimeState() {
        resolutions.clear();
        analyses.clear();
        coverage.clear();
    }

    public synchronized S7WorkflowReport report(Instant at) {
        return reportGenerator.generate(snapshot(), at);
    }

    public synchronized S7WorkflowExportArtifact exportJson(Instant at) {
        return reportExporter.json(report(at));
    }

    public synchronized S7WorkflowExportArtifact exportMarkdown(Instant at) {
        return reportExporter.markdown(report(at));
    }

    public synchronized S7WorkflowProductSnapshot snapshot() {
        WorkflowTransitionCoverageMatrix matrix = new WorkflowTransitionCoverageMatrix();
        coverage.values().forEach(matrix::put);
        return new S7WorkflowProductSnapshot(
                policy,
                List.copyOf(resolutions.values()),
                List.copyOf(analyses.values()),
                matrix.entries(),
                matrix.summary());
    }
}
