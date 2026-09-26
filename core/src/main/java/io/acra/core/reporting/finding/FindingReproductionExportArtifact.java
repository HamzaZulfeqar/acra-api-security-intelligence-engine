package io.acra.core.reporting.finding;

public record FindingReproductionExportArtifact(
        String format,
        String mediaType,
        String fileName,
        String sha256,
        String content) {

    public FindingReproductionExportArtifact {
        if (format == null || format.isBlank()) throw new IllegalArgumentException("format required");
        if (mediaType == null || mediaType.isBlank()) throw new IllegalArgumentException("mediaType required");
        if (fileName == null || fileName.isBlank()) throw new IllegalArgumentException("fileName required");
        if (sha256 == null || sha256.isBlank()) throw new IllegalArgumentException("sha256 required");
        content = content == null ? "" : content;
    }
}
