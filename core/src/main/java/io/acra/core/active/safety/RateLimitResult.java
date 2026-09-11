package io.acra.core.active.safety;

public record RateLimitResult(boolean allowed, long retryAfterMillis, String reason) {
    public RateLimitResult {
        if (retryAfterMillis < 0) throw new IllegalArgumentException("retryAfterMillis");
        reason = reason == null ? "" : reason;
    }

    public static RateLimitResult pass() { return new RateLimitResult(true, 0, ""); }
}
