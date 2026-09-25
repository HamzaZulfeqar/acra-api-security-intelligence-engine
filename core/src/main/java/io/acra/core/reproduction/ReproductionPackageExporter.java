package io.acra.core.reproduction;

import io.acra.core.security.TokenFingerprint;
import io.acra.core.security.UniversalRedactor;
import io.acra.core.serialization.DomainSerializer;

public final class ReproductionPackageExporter {
    private final DomainSerializer serializer = new DomainSerializer();
    private final UniversalRedactor redactor = new UniversalRedactor();

    public ReproductionExportArtifact json(ReproductionPackage value) {
        if (value == null) throw new IllegalArgumentException("reproduction package required");
        String content = redactor.redactText(serializer.serialize(value));
        return artifact(
                "JSON",
                "application/json",
                value.packageId() + ".json",
                content);
    }

    private ReproductionExportArtifact artifact(
            String format,
            String mediaType,
            String fileName,
            String content) {
        String safe = redactor.redactText(content);
        return new ReproductionExportArtifact(
                format,
                mediaType,
                fileName,
                TokenFingerprint.sha256(safe),
                safe);
    }
}
