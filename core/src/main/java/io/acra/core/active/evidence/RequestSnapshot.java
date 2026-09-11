package io.acra.core.active.evidence;

import io.acra.core.active.execution.BuiltRequest;
import io.acra.core.active.execution.RequestVariantKind;
import io.acra.core.domain.common.Validation;
import io.acra.core.domain.http.HttpRequest;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.serialization.DomainSerializer;
import java.time.Instant;

public record RequestSnapshot(
        String requestId,
        RequestVariantKind kind,
        HttpRequest request,
        String contextRef,
        String resourceRef,
        Instant capturedAt,
        String fingerprint) {
    public RequestSnapshot {
        requestId = Validation.requireNonBlank(requestId, "requestId");
        if (kind == null || request == null || capturedAt == null) throw new IllegalArgumentException("request snapshot fields required");
        contextRef = contextRef == null ? "UNKNOWN" : contextRef;
        resourceRef = resourceRef == null ? "UNKNOWN" : resourceRef;
        String calculated = TokenFingerprint.sha256(new DomainSerializer().serialize(request));
        if (fingerprint == null || fingerprint.isBlank()) fingerprint = calculated;
        if (!fingerprint.equals(calculated)) throw new IllegalArgumentException("request fingerprint mismatch");
    }

    public static RequestSnapshot capture(String requestId, BuiltRequest request, Instant at) {
        return new RequestSnapshot(requestId, request.kind(), request.request(), request.contextRef(), request.resourceRef(), at, "");
    }
}
