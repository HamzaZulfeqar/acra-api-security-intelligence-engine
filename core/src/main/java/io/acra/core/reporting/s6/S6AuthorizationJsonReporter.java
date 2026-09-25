package io.acra.core.reporting.s6;

import io.acra.core.plugin.Reporter;
import java.util.Map;

public final class S6AuthorizationJsonReporter implements Reporter {
    private final S6AuthorizationReportExporter exporter = new S6AuthorizationReportExporter();

    @Override
    public String id() {
        return "s6-authorization-json-v1";
    }

    @Override
    public String render(Map<String, Object> reportModel) {
        if (reportModel == null) throw new IllegalArgumentException("reportModel required");
        Object value = reportModel.get("report");
        if (!(value instanceof S6AuthorizationReport report)) {
            throw new IllegalArgumentException("reportModel.report must be S6AuthorizationReport");
        }
        return exporter.json(report).content();
    }
}
