package io.acra.core.domain.evidence;

import io.acra.core.domain.common.*;
import io.acra.core.security.TokenFingerprint;
import java.time.Instant;

public record Evidence(String evidenceId, EvidenceSource source, String requestId, String location,
                       String extractedValue, String extractionMethod, Confidence confidence, Instant timestamp) {
    public Evidence {
        evidenceId = Validation.requireNonBlank(evidenceId, "evidenceId");
        if (source == null) source = EvidenceSource.UNKNOWN;
        requestId = Validation.requireNonBlank(requestId, "requestId");
        location = Validation.requireNonBlank(location, "location");
        extractedValue = extractedValue == null ? "" : extractedValue;
        extractionMethod = Validation.requireNonBlank(extractionMethod, "extractionMethod");
        if (confidence == null) confidence = Confidence.unknown();
        if (timestamp == null) throw new DomainValidationException("evidence timestamp is required");
    }
    public static Evidence create(EvidenceSource source, String requestId, String location, String extractedValue,
                                  String extractionMethod, Confidence confidence, Instant timestamp) {
        String idMaterial = requestId + "|" + source + "|" + location + "|" + extractedValue + "|" + extractionMethod;
        String id = "ev-" + TokenFingerprint.sha256(idMaterial).substring(0, 20);
        return new Evidence(id, source, requestId, location, extractedValue, extractionMethod, confidence, timestamp);
    }
}
