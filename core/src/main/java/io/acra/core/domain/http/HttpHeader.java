package io.acra.core.domain.http;

import io.acra.core.domain.common.Validation;

public record HttpHeader(String name, String value) {
    public HttpHeader {
        name = Validation.requireNonBlank(name, "header name");
        if (value == null) value = "";
    }
}
