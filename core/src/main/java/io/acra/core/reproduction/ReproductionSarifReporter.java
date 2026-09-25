package io.acra.core.reproduction;

import io.acra.core.plugin.Reporter;
import java.util.Map;

public final class ReproductionSarifReporter implements Reporter {
    private final ReproductionSarifExporter exporter = new ReproductionSarifExporter();

    @Override
    public String id() {
        return "reproduction-sarif-v1";
    }

    @Override
    public String render(Map<String, Object> reportModel) {
        if (reportModel == null || !(reportModel.get("reproductionPackage") instanceof ReproductionPackage value)) {
            throw new IllegalArgumentException("reportModel.reproductionPackage required");
        }
        return exporter.sarif(value).content();
    }
}
