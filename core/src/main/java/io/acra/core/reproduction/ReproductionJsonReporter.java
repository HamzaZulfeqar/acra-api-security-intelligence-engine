package io.acra.core.reproduction;

import io.acra.core.plugin.Reporter;
import java.util.Map;

public final class ReproductionJsonReporter implements Reporter {
    private final ReproductionJsonExporter exporter = new ReproductionJsonExporter();

    @Override
    public String id() {
        return "acra-reproduction-json-v1";
    }

    @Override
    public String render(Map<String, Object> reportModel) {
        if (reportModel == null) throw new IllegalArgumentException("reportModel required");
        Object value = reportModel.get("reproductionPackage");
        if (!(value instanceof ReproductionPackage reproductionPackage)) {
            throw new IllegalArgumentException(
                    "reportModel.reproductionPackage must be ReproductionPackage");
        }
        return exporter.export(reproductionPackage).content();
    }
}
