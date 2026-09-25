package io.acra.core.reporting.finding;

import io.acra.core.plugin.Reporter;
import java.util.Map;

public final class FindingReproductionJsonReporter implements Reporter {
    private final FindingReproductionJsonExporter exporter = new FindingReproductionJsonExporter();

    @Override
    public String id() {
        return "s11-finding-reproduction-json-v1";
    }

    @Override
    public String render(Map<String, Object> reportModel) {
        if (reportModel == null) throw new IllegalArgumentException("reportModel required");
        Object value = reportModel.get("reproduction");
        if (!(value instanceof FindingReproductionPackage reproduction)) {
            throw new IllegalArgumentException(
                    "reportModel.reproduction must be FindingReproductionPackage");
        }
        return exporter.json(reproduction).content();
    }
}
