package io.acra.core.domain.common;

public final class DomainLimits {
    private DomainLimits() {}
    public static final int MAX_HEADER_COUNT = 256;
    public static final int MAX_HEADER_VALUE_CHARS = 65_536;
    public static final int MAX_BODY_BYTES = 16 * 1024 * 1024;
}
