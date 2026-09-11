package io.acra.core.active.execution;

public record RequestSet(BuiltRequest baseline, BuiltRequest positiveControl, BuiltRequest negativeControl, BuiltRequest mutation) {
    public RequestSet {
        if (baseline == null || positiveControl == null || negativeControl == null || mutation == null) {
            throw new IllegalArgumentException("all four request variants are required");
        }
        if (baseline.kind() != RequestVariantKind.BASELINE
                || positiveControl.kind() != RequestVariantKind.POSITIVE_CONTROL
                || negativeControl.kind() != RequestVariantKind.NEGATIVE_CONTROL
                || mutation.kind() != RequestVariantKind.MUTATION) {
            throw new IllegalArgumentException("request variant kind mismatch");
        }
    }
}
