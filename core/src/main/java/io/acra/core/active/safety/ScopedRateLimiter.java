package io.acra.core.active.safety;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ScopedRateLimiter {
    private static final class State {
        private final Deque<Long> second = new ArrayDeque<>();
        private final Deque<Long> minute = new ArrayDeque<>();
        private long last = Long.MIN_VALUE;
    }

    private final Map<String, RateLimitPolicy> policies = new HashMap<>();
    private final Map<String, State> states = new HashMap<>();

    public synchronized void configure(String key, RateLimitPolicy policy) {
        if (key == null || key.isBlank() || policy == null) throw new IllegalArgumentException("rate-limit key/policy");
        policies.put(key, policy);
        states.computeIfAbsent(key, ignored -> new State());
    }

    public synchronized RateLimitResult tryAcquire(List<String> keys, Instant now) {
        if (keys == null || keys.isEmpty() || now == null) return new RateLimitResult(false, 0, "rate-limit keys and time required");
        long millis = now.toEpochMilli();
        long retry = 0;
        for (String key : keys.stream().distinct().toList()) {
            RateLimitPolicy policy = policies.get(key);
            State state = states.get(key);
            if (policy == null || state == null) return new RateLimitResult(false, 0, "rate-limit scope not configured: " + key);
            trim(state.second, millis - 1000);
            trim(state.minute, millis - 60_000);
            int secondLimit = Math.max(policy.requestsPerSecond(), policy.burst());
            if (state.second.size() >= secondLimit) retry = Math.max(retry, 1000 - (millis - state.second.getFirst()));
            if (state.minute.size() >= policy.requestsPerMinute()) retry = Math.max(retry, 60_000 - (millis - state.minute.getFirst()));
            if (state.last != Long.MIN_VALUE) retry = Math.max(retry, policy.minimumDelayMillis() - (millis - state.last));
        }
        if (retry > 0) return new RateLimitResult(false, retry, "configured rate limit reached");
        for (String key : keys.stream().distinct().toList()) {
            State state = states.get(key);
            state.second.addLast(millis);
            state.minute.addLast(millis);
            state.last = millis;
        }
        return RateLimitResult.pass();
    }

    public synchronized boolean isConfigured(List<String> keys) {
        return keys != null && !keys.isEmpty() && keys.stream().distinct().allMatch(policies::containsKey);
    }

    private static void trim(Deque<Long> values, long cutoff) {
        while (!values.isEmpty() && values.getFirst() <= cutoff) values.removeFirst();
    }
}
