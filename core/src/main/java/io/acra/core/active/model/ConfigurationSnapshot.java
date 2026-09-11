package io.acra.core.active.model;

import io.acra.core.security.TokenFingerprint;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public record ConfigurationSnapshot(Map<String, String> values, String fingerprint) {
    public ConfigurationSnapshot {
        TreeMap<String, String> copy = new TreeMap<>();
        if (values != null) copy.putAll(values);
        values = Collections.unmodifiableMap(copy);
        String calculated = TokenFingerprint.sha256(canonical(copy));
        if (fingerprint == null || fingerprint.isBlank()) fingerprint = calculated;
        if (!fingerprint.equals(calculated)) throw new IllegalArgumentException("configuration fingerprint mismatch");
    }

    public static ConfigurationSnapshot of(Map<String, String> values) {
        return new ConfigurationSnapshot(values, "");
    }

    private static String canonical(Map<String, String> values) {
        StringBuilder out = new StringBuilder();
        values.forEach((key, value) -> out.append(key.length()).append(':').append(key)
                .append('=').append(value == null ? -1 : value.length()).append(':')
                .append(value == null ? "" : value).append('\n'));
        return out.toString();
    }
}
