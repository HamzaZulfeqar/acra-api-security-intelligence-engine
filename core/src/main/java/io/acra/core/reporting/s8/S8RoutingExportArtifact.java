package io.acra.core.reporting.s8;

public record S8RoutingExportArtifact(
        String format,
        String mediaType,
        String fileName,
        String sha256,
        String content) {

    public S8RoutingExportArtifact {
        if (format == null || format.isBlank()) throw new IllegalArgumentException("format required");
        if (mediaType == null || mediaType.isBlank()) throw new IllegalArgumentException("mediaType required");
        if (fileName == null || fileName.isBlank()) throw new IllegalArgumentException("fileName required");
        if (sha256 == null || sha256.isBlank()) throw new IllegalArgumentException("sha256 required");
        content = content == null ? "" : content;
    }
}
