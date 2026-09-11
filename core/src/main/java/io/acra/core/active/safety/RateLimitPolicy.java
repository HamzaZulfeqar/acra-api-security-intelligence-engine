package io.acra.core.active.safety;

public record RateLimitPolicy(int requestsPerSecond, int requestsPerMinute, int burst, long minimumDelayMillis) {
    public RateLimitPolicy {
        if (requestsPerSecond < 1 || requestsPerMinute < 1 || burst < 1 || minimumDelayMillis < 0) {
            throw new IllegalArgumentException("invalid rate-limit policy");
        }
    }
}
