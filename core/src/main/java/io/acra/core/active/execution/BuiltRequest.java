package io.acra.core.active.execution;

import io.acra.core.domain.common.Validation;
import io.acra.core.domain.http.HttpRequest;

public record BuiltRequest(RequestVariantKind kind, String definitionId, HttpRequest request, String contextRef, String resourceRef) {
    public BuiltRequest {
        if (kind == null || request == null) throw new IllegalArgumentException("request kind and request required");
        definitionId = Validation.requireNonBlank(definitionId, "definitionId");
        contextRef = contextRef == null ? "UNKNOWN" : contextRef;
        resourceRef = resourceRef == null ? "UNKNOWN" : resourceRef;
    }
}
