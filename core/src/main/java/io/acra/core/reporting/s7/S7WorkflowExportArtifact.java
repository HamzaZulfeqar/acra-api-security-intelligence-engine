package io.acra.core.reporting.s7;

public record S7WorkflowExportArtifact(
        String format,
        String mediaType,
        String fileName,
        String sha256,
        String content) {

    public S7WorkflowExportArtifact {
        if (format == null || format.isBlank()) throw new IllegalArgumentException("format required");
        if (mediaType == null || mediaType.isBlank()) throw new IllegalArgumentException("mediaType required");
        if (fileName == null || fileName.isBlank()) throw new IllegalArgumentException("fileName required");
        if (sha256 == null || sha256.isBlank()) throw new IllegalArgumentException("sha256 required");
        content = content == null ? "" : content;
    }
}
