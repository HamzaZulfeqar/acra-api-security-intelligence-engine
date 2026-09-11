package io.acra.core.domain.common;

import java.util.Collection;

public final class Validation {
    private Validation() {}

    public static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) throw new DomainValidationException(field + " must not be blank");
        return value;
    }

    public static double requireConfidence(double value) {
        if (Double.isNaN(value) || value < 0.0 || value > 1.0) throw new DomainValidationException("confidence must be between 0 and 1");
        return value;
    }

    public static <T> Collection<T> requireNoNulls(Collection<T> values, String field) {
        if (values == null || values.stream().anyMatch(v -> v == null)) throw new DomainValidationException(field + " must not contain null");
        return values;
    }
}
