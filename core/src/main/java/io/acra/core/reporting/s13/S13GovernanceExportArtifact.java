package io.acra.core.reporting.s13;

import io.acra.core.domain.common.Validation;

public record S13GovernanceExportArtifact(
        String format,
        String mediaType,
        String fileName,
        String sha256,
        String content) {

    public S13GovernanceExportArtifact {
        format = Validation.requireNonBlank(format, "format");
        mediaType = Validation.requireNonBlank(mediaType, "mediaType");
        fileName = Validation.requireNonBlank(fileName, "fileName");
        sha256 = Validation.requireNonBlank(sha256, "sha256");
        content = content == null ? "" : content;
    }
}
