package io.acra.core.domain.common;

public enum ConfidenceBasis {
    EXACT_OBSERVED(1.00),
    EXPLICIT_METADATA(0.95),
    STRUCTURAL_INFERENCE(0.80),
    HEURISTIC(0.60),
    WEAK_HEURISTIC(0.30),
    UNKNOWN(0.00);

    private final double defaultScore;
    ConfidenceBasis(double defaultScore) { this.defaultScore = defaultScore; }
    public double defaultScore() { return defaultScore; }
}
