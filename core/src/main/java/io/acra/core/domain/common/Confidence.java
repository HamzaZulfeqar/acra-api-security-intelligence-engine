package io.acra.core.domain.common;

public record Confidence(double score, ConfidenceBasis basis) {
    public Confidence {
        Validation.requireConfidence(score);
        if (basis == null) throw new DomainValidationException("confidence basis is required");
    }
    public static Confidence of(ConfidenceBasis basis) { return new Confidence(basis.defaultScore(), basis); }
    public static Confidence unknown() { return of(ConfidenceBasis.UNKNOWN); }
}
