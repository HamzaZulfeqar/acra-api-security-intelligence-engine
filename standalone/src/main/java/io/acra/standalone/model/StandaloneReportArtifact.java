package io.acra.standalone.model;

public record StandaloneReportArtifact(
        String format,
        String content,
        String sha256
) {
    public StandaloneReportArtifact {
        format = format == null ? "" : format;
        content = content == null ? "" : content;
        sha256 = sha256 == null ? "" : sha256;
    }
}
