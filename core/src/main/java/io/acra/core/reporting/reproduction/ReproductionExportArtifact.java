package io.acra.core.reporting.reproduction;

public record ReproductionExportArtifact(
        String format,
        String mediaType,
        String fileName,
        String sha256,
        String content) {

    public ReproductionExportArtifact {
        if (format == null || format.isBlank()
                || mediaType == null || mediaType.isBlank()
                || fileName == null || fileName.isBlank()
                || sha256 == null || sha256.isBlank()
                || content == null) {
            throw new IllegalArgumentException("complete reproduction export artifact required");
        }
    }
}
