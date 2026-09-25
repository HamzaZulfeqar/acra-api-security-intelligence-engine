package io.acra.core.reporting.finding;

import io.acra.core.security.TokenFingerprint;
import io.acra.core.security.UniversalRedactor;
import io.acra.core.serialization.DomainSerializer;

public final class FindingReproductionJsonExporter {
    private final DomainSerializer serializer = new DomainSerializer();
    private final UniversalRedactor redactor = new UniversalRedactor();

    public FindingReproductionExportArtifact json(FindingReproductionPackage reproduction) {
        if (reproduction == null) throw new IllegalArgumentException("reproduction required");
        String content = redactor.redactText(serializer.serialize(reproduction));
        String sha256 = TokenFingerprint.sha256(content);
        return new FindingReproductionExportArtifact(
                "JSON",
                "application/json",
                reproduction.reproductionId() + ".json",
                sha256,
                content);
    }
}
