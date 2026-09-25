package io.acra.core.reproduction;

import io.acra.core.plugin.Reporter;
import java.util.Map;

public final class ReproductionJsonReporter implements Reporter {
    private final ReproductionPackageExporter exporter = new ReproductionPackageExporter();

    @Override
    public String id() {
        return "reproduction-json-v1";
    }

    @Override
    public String render(Map<String, Object> reportModel) {
        if (reportModel == null || !(reportModel.get("reproductionPackage") instanceof ReproductionPackage value)) {
            throw new IllegalArgumentException("reportModel.reproductionPackage required");
        }
        return exporter.json(value).content();
    }
}
