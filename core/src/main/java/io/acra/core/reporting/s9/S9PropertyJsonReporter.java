package io.acra.core.reporting.s9;

import io.acra.core.plugin.Reporter;
import java.util.Map;

public final class S9PropertyJsonReporter implements Reporter {
    private final S9PropertyReportExporter exporter = new S9PropertyReportExporter();

    @Override
    public String id() {
        return "s9-property-json-v1";
    }

    @Override
    public String render(Map<String, Object> reportModel) {
        if (reportModel == null) throw new IllegalArgumentException("reportModel required");
        Object value = reportModel.get("report");
        if (!(value instanceof S9PropertyReport report)) {
            throw new IllegalArgumentException("reportModel.report must be S9PropertyReport");
        }
        return exporter.json(report).content();
    }
}
