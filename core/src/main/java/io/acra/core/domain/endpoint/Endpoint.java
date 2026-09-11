package io.acra.core.domain.endpoint;

import io.acra.core.domain.common.Validation;
import io.acra.core.domain.http.HttpMethod;
import java.util.*;

public record Endpoint(String endpointId, HttpMethod method, String rawPath, String normalizedPath,
                       String routeTemplate, String host, String version, List<String> observations) {
    public Endpoint {
        endpointId = Validation.requireNonBlank(endpointId, "endpointId");
        if (method == null) throw new IllegalArgumentException("endpoint method required");
        rawPath = Validation.requireNonBlank(rawPath, "rawPath");
        normalizedPath = Validation.requireNonBlank(normalizedPath, "normalizedPath");
        routeTemplate = Validation.requireNonBlank(routeTemplate, "routeTemplate");
        host = Validation.requireNonBlank(host, "host");
        version = version == null ? "" : version;
        observations = List.copyOf(observations == null ? List.of() : observations);
    }
}
