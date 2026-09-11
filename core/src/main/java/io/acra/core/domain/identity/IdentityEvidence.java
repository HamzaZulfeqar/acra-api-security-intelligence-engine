package io.acra.core.domain.identity;

import io.acra.core.domain.common.*;
import io.acra.core.domain.evidence.EvidenceSource;

public record IdentityEvidence(EvidenceSource source, String identifier, String tokenHash,
                               String extractionMethod, Confidence confidence) {
    public IdentityEvidence {
        if (source == null) source = EvidenceSource.UNKNOWN;
        identifier = identifier == null ? "" : identifier;
        tokenHash = tokenHash == null ? "" : tokenHash;
        extractionMethod = Validation.requireNonBlank(extractionMethod, "extractionMethod");
        if (confidence == null) confidence = Confidence.unknown();
    }
}
