package io.acra.core.active.model;

import io.acra.core.domain.common.Validation;
import io.acra.core.domain.http.HttpRequest;

public record RequestDefinition(String definitionId, HttpRequest request, String contextRef, String resourceRef) {
    public RequestDefinition {
        definitionId = Validation.requireNonBlank(definitionId, "definitionId");
        if (request == null) throw new IllegalArgumentException("request required");
        contextRef = contextRef == null ? "UNKNOWN" : contextRef;
        resourceRef = resourceRef == null ? "UNKNOWN" : resourceRef;
    }
}
