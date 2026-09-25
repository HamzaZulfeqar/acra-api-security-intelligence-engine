package io.acra.core.serialization;

import io.acra.core.security.UniversalRedactor;

public final class CanonicalJsonDocumentWriter {
    private final CanonicalJsonWriter writer;

    public CanonicalJsonDocumentWriter() {
        this(new UniversalRedactor());
    }

    public CanonicalJsonDocumentWriter(UniversalRedactor redactor) {
        this.writer = new CanonicalJsonWriter(
                redactor == null ? new UniversalRedactor() : redactor);
    }

    public String write(Object value) {
        return writer.write(value);
    }
}
