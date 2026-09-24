package io.acra.core.reporting.s11;

public record S11ResearchExportArtifact(
        String format,
        String mediaType,
        String fileName,
        String sha256,
        String content) {

    public S11ResearchExportArtifact {
        if (format == null || format.isBlank()
                || mediaType == null || mediaType.isBlank()
                || fileName == null || fileName.isBlank()
                || sha256 == null || sha256.isBlank()
                || content == null) {
            throw new IllegalArgumentException("complete export artifact required");
        }
    }
}
