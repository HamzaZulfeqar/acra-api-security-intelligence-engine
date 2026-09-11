package io.acra.core.domain.http;

import io.acra.core.domain.common.DomainValidationException;
import io.acra.core.domain.common.Validation;
import java.time.Instant;
import java.util.*;

public record HttpTransaction(HttpRequest request, HttpResponse response, Instant timestamp,
                              String requestId, String source, Map<String,String> metadata) {
    public HttpTransaction {
        if (request == null) throw new DomainValidationException("request is required");
        if (timestamp == null) throw new DomainValidationException("timestamp is required");
        requestId = Validation.requireNonBlank(requestId, "requestId");
        source = Validation.requireNonBlank(source, "source");
        TreeMap<String,String> copy = new TreeMap<>();
        if (metadata != null) copy.putAll(metadata);
        metadata = Collections.unmodifiableMap(copy);
    }
}
