package io.acra.core.active.safety;

public final class BudgetExceededException extends IllegalStateException {
    private static final long serialVersionUID = 1L;

    public BudgetExceededException(String message) {
        super(message);
    }
}
