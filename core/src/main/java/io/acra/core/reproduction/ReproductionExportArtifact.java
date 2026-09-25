package io.acra.core.reproduction;

public record ReproductionExportArtifact(
        String format,
        String mediaType,
        String fileName,
        String sha256,
        String content) {

    public ReproductionExportArtifact {
        if (format == null || format.isBlank()) throw new IllegalArgumentException("format required");
        if (mediaType == null || mediaType.isBlank()) throw new IllegalArgumentException("mediaType required");
        if (fileName == null || fileName.isBlank()) throw new IllegalArgumentException("fileName required");
        if (sha256 == null || !sha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("sha256 required");
        }
        content = content == null ? "" : content;
    }
}
