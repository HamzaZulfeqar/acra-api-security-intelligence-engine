package io.acra.core.serialization;

import io.acra.core.security.UniversalRedactor;

public final class CanonicalJsonSerializer {
    private final CanonicalJsonWriter writer;

    public CanonicalJsonSerializer() {
        this(new UniversalRedactor());
    }

    public CanonicalJsonSerializer(UniversalRedactor redactor) {
        this.writer = new CanonicalJsonWriter(redactor);
    }

    public String serialize(Object value) {
        return writer.write(value);
    }
}
