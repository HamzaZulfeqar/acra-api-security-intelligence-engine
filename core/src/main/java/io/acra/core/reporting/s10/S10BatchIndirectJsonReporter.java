package io.acra.core.reporting.s10;

import io.acra.core.plugin.Reporter;
import java.util.Map;

public final class S10BatchIndirectJsonReporter implements Reporter {
    private final S10BatchIndirectReportExporter exporter = new S10BatchIndirectReportExporter();

    @Override
    public String id() {
        return "s10-batch-indirect-json-v1";
    }

    @Override
    public String render(Map<String, Object> reportModel) {
        if (reportModel == null) throw new IllegalArgumentException("reportModel required");
        Object value = reportModel.get("report");
        if (!(value instanceof S10BatchIndirectReport report)) {
            throw new IllegalArgumentException("reportModel.report must be S10BatchIndirectReport");
        }
        return exporter.json(report).content();
    }
}
