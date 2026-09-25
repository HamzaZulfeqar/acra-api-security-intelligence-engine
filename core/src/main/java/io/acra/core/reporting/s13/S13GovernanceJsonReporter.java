package io.acra.core.reporting.s13;

import io.acra.core.plugin.Reporter;
import java.util.Map;

public final class S13GovernanceJsonReporter implements Reporter {
    private final S13GovernanceReportExporter exporter = new S13GovernanceReportExporter();

    @Override
    public String id() {
        return "s13-governance-json-v1";
    }

    @Override
    public String render(Map<String, Object> reportModel) {
        if (reportModel == null || !(reportModel.get("report") instanceof S13GovernanceReport report)) {
            throw new IllegalArgumentException("reportModel.report must be S13GovernanceReport");
        }
        return exporter.json(report).content();
    }
}
