package io.acra.core.active.analysis;

public enum ExpectedDecisionSource {
    ACRA_LAB_GROUND_TRUTH(0),
    EXPLICIT_CONFIGURED_POLICY(1),
    EXPLICIT_TEST_DEFINITION(2),
    AUTHORIZATION_MATRIX(3),
    VALIDATED_API_METADATA(4),
    INFERRED_POLICY(5),
    UNKNOWN(6);

    private final int precedence;

    ExpectedDecisionSource(int precedence) {
        this.precedence = precedence;
    }

    public int precedence() {
        return precedence;
    }
}
