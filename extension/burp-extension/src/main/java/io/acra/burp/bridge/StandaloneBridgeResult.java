package io.acra.burp.bridge;

public record StandaloneBridgeResult(
        boolean success,
        String message,
        int observations,
        int uniqueEndpoints,
        int inventorySize
) {
    public StandaloneBridgeResult {
        message = message == null ? "" : message;
        if (observations < 0 || uniqueEndpoints < 0 || inventorySize < 0) {
            throw new IllegalArgumentException("bridge counts must be non-negative");
        }
    }
}
