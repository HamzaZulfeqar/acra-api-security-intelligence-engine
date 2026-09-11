package io.acra.core.active.evidence;

import io.acra.core.analysis.semantic.ResponseSemanticAnalyzer;
import io.acra.core.analysis.semantic.ResponseSemanticFingerprint;
import io.acra.core.domain.common.Validation;
import io.acra.core.domain.http.HttpResponse;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.security.UniversalRedactor;
import io.acra.core.serialization.DomainSerializer;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public record ResponseSnapshot(
        String responseId,
        String requestCorrelation,
        HttpResponse response,
        Map<String, String> cookies,
        Duration responseTiming,
        Instant capturedAt,
        ResponseSemanticFingerprint semanticFingerprint,
        String fingerprint) {
    public ResponseSnapshot {
        responseId = Validation.requireNonBlank(responseId, "responseId");
        requestCorrelation = Validation.requireNonBlank(requestCorrelation, "requestCorrelation");
        if (response == null || responseTiming == null || responseTiming.isNegative() || capturedAt == null) {
            throw new IllegalArgumentException("response snapshot fields required");
        }
        TreeMap<String, String> copy = new TreeMap<>();
        if (cookies != null) cookies.keySet().forEach(key -> copy.put(key, UniversalRedactor.REDACTED));
        cookies = Collections.unmodifiableMap(copy);
        if (semanticFingerprint == null) semanticFingerprint = new ResponseSemanticAnalyzer().fingerprint(response);
        String calculated = TokenFingerprint.sha256(new DomainSerializer().serialize(response));
        if (fingerprint == null || fingerprint.isBlank()) fingerprint = calculated;
        if (!fingerprint.equals(calculated)) throw new IllegalArgumentException("response fingerprint mismatch");
    }

    public static ResponseSnapshot capture(String responseId, String requestId, HttpResponse response,
                                             Map<String, String> cookies, Duration timing, Instant at) {
        return new ResponseSnapshot(responseId, requestId, response, cookies, timing, at, null, "");
    }
}
