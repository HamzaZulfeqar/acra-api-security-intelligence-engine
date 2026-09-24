package io.acra.core.reporting.s12;

import io.acra.core.security.TokenFingerprint;
import io.acra.core.security.UniversalRedactor;
import io.acra.core.serialization.DomainSerializer;

public final class S12ReproductionJsonExporter {
    private final DomainSerializer serializer = new DomainSerializer();
    private final UniversalRedactor redactor = new UniversalRedactor();

    public S12ReproductionExportArtifact export(S12ReproductionPackage reproduction) {
        if (reproduction == null) throw new IllegalArgumentException("reproduction package required");
        String content = redactor.redactText(serializer.serialize(reproduction));
        return new S12ReproductionExportArtifact(
                "JSON",
                "application/json",
                reproduction.packageId() + ".json",
                TokenFingerprint.sha256(content),
                content);
    }
}
