package io.acra.core.reporting.s7;

import io.acra.core.plugin.Reporter;
import java.util.Map;

public final class S7WorkflowJsonReporter implements Reporter {
    private final S7WorkflowReportExporter exporter = new S7WorkflowReportExporter();

    @Override
    public String id() {
        return "s7-workflow-json-v1";
    }

    @Override
    public String render(Map<String, Object> reportModel) {
        if (reportModel == null) throw new IllegalArgumentException("reportModel required");
        Object value = reportModel.get("report");
        if (!(value instanceof S7WorkflowReport report)) {
            throw new IllegalArgumentException("reportModel.report must be S7WorkflowReport");
        }
        return exporter.json(report).content();
    }
}
