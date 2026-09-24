package io.acra.core.reporting.s12;

public record S12ReproductionExportArtifact(
        String format,
        String mediaType,
        String fileName,
        String sha256,
        String content) {

    public S12ReproductionExportArtifact {
        format = required(format, "format");
        mediaType = required(mediaType, "mediaType");
        fileName = required(fileName, "fileName");
        sha256 = required(sha256, "sha256");
        content = required(content, "content");
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " required");
        return value.strip();
    }
}
