package io.acra.core.domain.uri;

import io.acra.core.domain.common.*;

public record PathSegment(int index, String rawValue, String decodedValue, String normalizedValue,
                          PathSegmentClassification classification, Confidence confidence) {
    public PathSegment {
        if (index < 0) throw new DomainValidationException("segment index cannot be negative");
        rawValue = rawValue == null ? "" : rawValue;
        decodedValue = decodedValue == null ? "" : decodedValue;
        normalizedValue = normalizedValue == null ? "" : normalizedValue;
        if (classification == null) classification = PathSegmentClassification.UNKNOWN;
        if (confidence == null) confidence = Confidence.unknown();
    }
}
