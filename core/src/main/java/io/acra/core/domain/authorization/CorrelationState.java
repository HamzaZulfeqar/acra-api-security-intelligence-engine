package io.acra.core.domain.authorization;

public enum CorrelationState {
    CORROBORATED,
    CONSISTENT,
    DUPLICATE,
    CONFLICTING,
    INSUFFICIENT,
    INCONCLUSIVE
}
