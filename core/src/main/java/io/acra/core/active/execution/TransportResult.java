package io.acra.core.active.execution;

import io.acra.core.domain.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public record TransportResult(HttpResponse response, Map<String, String> cookies, Duration responseTiming) {
    public TransportResult {
        if (response == null || responseTiming == null || responseTiming.isNegative()) {
            throw new IllegalArgumentException("transport response and non-negative timing required");
        }
        TreeMap<String, String> copy = new TreeMap<>();
        if (cookies != null) copy.putAll(cookies);
        cookies = Collections.unmodifiableMap(copy);
    }
}
