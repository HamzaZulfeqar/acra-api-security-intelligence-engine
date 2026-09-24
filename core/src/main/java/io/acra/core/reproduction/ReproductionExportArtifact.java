package io.acra.core.reproduction;

import io.acra.core.security.TokenFingerprint;

public record ReproductionExportArtifact(
        ReproductionExportTarget target,
        String mediaType,
        String fileName,
        String sha256,
        String content) {

    public ReproductionExportArtifact {
        if (target == null) throw new IllegalArgumentException("target required");
        mediaType = required(mediaType, "mediaType");
        fileName = required(fileName, "fileName");
        content = content == null ? "" : content;
        String calculated = TokenFingerprint.sha256(content);
        sha256 = sha256 == null || sha256.isBlank() ? calculated : sha256.strip();
        if (!sha256.equals(calculated)) throw new IllegalArgumentException("artifact sha256 mismatch");
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " required");
        return value.strip();
    }
}
