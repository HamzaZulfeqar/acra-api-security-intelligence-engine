package io.acra.core.domain.http;

import io.acra.core.domain.common.DomainValidationException;

public enum HttpMethod {
    GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS, TRACE, CONNECT;

    public static HttpMethod parse(String value) {
        if (value == null) throw new DomainValidationException("HTTP method is required");
        try { return valueOf(value.trim().toUpperCase()); }
        catch (IllegalArgumentException ex) { throw new DomainValidationException("unsupported HTTP method: " + value); }
    }
}
