package io.acra.core.reporting.s10;

import io.acra.core.plugin.Reporter;
import java.util.Map;

public final class S10SessionJsonReporter implements Reporter {
    private final S10SessionReportExporter exporter = new S10SessionReportExporter();

    @Override
    public String id() {
        return "s10-auth-session-json-v1";
    }

    @Override
    public String render(Map<String, Object> reportModel) {
        if (reportModel == null) throw new IllegalArgumentException("reportModel required");
        Object value = reportModel.get("report");
        if (!(value instanceof S10SessionReport report)) {
            throw new IllegalArgumentException("reportModel.report must be S10SessionReport");
        }
        return exporter.json(report).content();
    }
}
