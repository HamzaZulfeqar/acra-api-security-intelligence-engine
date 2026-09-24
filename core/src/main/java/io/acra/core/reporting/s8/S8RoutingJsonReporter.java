package io.acra.core.reporting.s8;

import io.acra.core.plugin.Reporter;
import java.util.Map;

public final class S8RoutingJsonReporter implements Reporter {
    private final S8RoutingReportExporter exporter = new S8RoutingReportExporter();

    @Override
    public String id() {
        return "s8-routing-json-v1";
    }

    @Override
    public String render(Map<String, Object> reportModel) {
        if (reportModel == null) throw new IllegalArgumentException("reportModel required");
        Object value = reportModel.get("report");
        if (!(value instanceof S8RoutingReport report)) {
            throw new IllegalArgumentException("reportModel.report must be S8RoutingReport");
        }
        return exporter.json(report).content();
    }
}
