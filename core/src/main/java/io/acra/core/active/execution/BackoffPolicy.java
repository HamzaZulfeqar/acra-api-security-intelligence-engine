package io.acra.core.active.execution;

import io.acra.core.domain.http.HttpHeader;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class BackoffPolicy {
    private final int maxRetries;
    private final Duration maximumDelay;

    public BackoffPolicy(int maxRetries, Duration maximumDelay) {
        if (maxRetries < 0 || maximumDelay == null || maximumDelay.isNegative() || maximumDelay.isZero()) {
            throw new IllegalArgumentException("invalid backoff policy");
        }
        this.maxRetries = maxRetries;
        this.maximumDelay = maximumDelay;
    }

    public BackoffDecision forResponse(int status, List<HttpHeader> headers, int attempt) {
        if (status != 429 && status != 503) return BackoffDecision.none();
        Duration delay = retryAfter(headers);
        if (delay.isZero()) delay = Duration.ofMillis(Math.min(maximumDelay.toMillis(), 250L * (1L << Math.min(attempt, 10))));
        if (delay.compareTo(maximumDelay) > 0) delay = maximumDelay;
        boolean retry = attempt < maxRetries;
        return new BackoffDecision(
                retry ? Set.of(BackoffAction.PAUSE, BackoffAction.DELAY, BackoffAction.REDUCE_CONCURRENCY, BackoffAction.RETRY)
                        : Set.of(BackoffAction.PAUSE, BackoffAction.DELAY, BackoffAction.REDUCE_CONCURRENCY),
                delay, retry, "operational HTTP " + status);
    }

    public BackoffDecision forTemporaryFailure(int attempt, String reason) {
        Duration delay = Duration.ofMillis(Math.min(maximumDelay.toMillis(), 250L * (1L << Math.min(attempt, 10))));
        boolean retry = attempt < maxRetries;
        return new BackoffDecision(retry ? Set.of(BackoffAction.DELAY, BackoffAction.RETRY) : Set.of(BackoffAction.PAUSE),
                delay, retry, reason == null ? "temporary connection failure" : reason);
    }

    private static Duration retryAfter(List<HttpHeader> headers) {
        String value = headers == null ? null : headers.stream()
                .filter(header -> header.name().equalsIgnoreCase("Retry-After"))
                .map(HttpHeader::value).findFirst().orElse(null);
        if (value == null || value.isBlank()) return Duration.ZERO;
        try {
            long seconds = Long.parseLong(value.trim());
            return Duration.ofSeconds(Math.max(0, seconds));
        } catch (NumberFormatException ignored) {
            try {
                ZonedDateTime at = ZonedDateTime.parse(value.trim(), DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.US));
                return Duration.between(ZonedDateTime.now(at.getZone()), at).isNegative()
                        ? Duration.ZERO : Duration.between(ZonedDateTime.now(at.getZone()), at);
            } catch (RuntimeException invalidDate) {
                return Duration.ZERO;
            }
        }
    }
}
