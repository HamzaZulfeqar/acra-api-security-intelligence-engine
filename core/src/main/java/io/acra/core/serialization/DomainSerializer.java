package io.acra.core.serialization;

import io.acra.core.security.UniversalRedactor;
import java.util.Map;

public final class DomainSerializer {
    public static final String SCHEMA_VERSION = "1.0";

    private final CanonicalJsonWriter writer;

    public DomainSerializer() {
        this(new UniversalRedactor());
    }

    public DomainSerializer(UniversalRedactor redactor) {
        this.writer = new CanonicalJsonWriter(redactor);
    }

    public String serialize(Object value) {
        return writer.write(Map.of(
                "schemaVersion", SCHEMA_VERSION,
                "type", value == null ? "null" : value.getClass().getSimpleName(),
                "data", value));
    }

    public String serializeCanonicalValue(Object value) {
        return writer.write(value);
    }
}
