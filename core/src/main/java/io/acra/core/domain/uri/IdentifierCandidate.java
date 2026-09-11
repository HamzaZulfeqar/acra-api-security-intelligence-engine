package io.acra.core.domain.uri;

import io.acra.core.domain.common.*;

public record IdentifierCandidate(String value, IdentifierLocation location, IdentifierType type,
                                  Confidence confidence, String detectionReason, String source) {
    public IdentifierCandidate {
        value = Validation.requireNonBlank(value, "identifier value");
        if (location == null) location = IdentifierLocation.UNKNOWN;
        if (type == null) type = IdentifierType.UNKNOWN;
        if (confidence == null) confidence = Confidence.unknown();
        detectionReason = Validation.requireNonBlank(detectionReason, "detectionReason");
        source = Validation.requireNonBlank(source, "source");
    }
}
