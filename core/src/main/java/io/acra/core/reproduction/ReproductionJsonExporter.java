package io.acra.core.reproduction;

import io.acra.core.security.UniversalRedactor;
import io.acra.core.serialization.DomainSerializer;

public final class ReproductionJsonExporter {
    private final DomainSerializer serializer = new DomainSerializer();
    private final UniversalRedactor redactor = new UniversalRedactor();

    public ReproductionExportArtifact export(ReproductionPackage reproductionPackage) {
        if (reproductionPackage == null) throw new IllegalArgumentException("reproductionPackage required");
        String content = redactor.redactText(serializer.serialize(reproductionPackage));
        return new ReproductionExportArtifact(
                ReproductionExportTarget.JSON,
                "application/json",
                reproductionPackage.packageId() + ".json",
                "",
                content);
    }
}
