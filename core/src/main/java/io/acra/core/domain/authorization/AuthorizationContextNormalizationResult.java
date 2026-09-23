package io.acra.core.domain.authorization;

import io.acra.core.security.UniversalRedactor;
import java.util.List;

/** Result of binding an S4 observation to an S5 authorization context. */
public record AuthorizationContextNormalizationResult(
        AuthorizationContext context,
        boolean valid,
        List<String> reasons) {
    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public AuthorizationContextNormalizationResult {
        reasons = List.copyOf(reasons == null ? List.<String>of() : reasons).stream()
                .map(value -> REDACTOR.redactText(value == null ? "" : value))
                .distinct()
                .sorted()
                .toList();
        if (valid && context == null) throw new IllegalArgumentException("valid normalization requires context");
    }

    public static AuthorizationContextNormalizationResult accepted(AuthorizationContext context) {
        return new AuthorizationContextNormalizationResult(context, true, List.of());
    }

    public static AuthorizationContextNormalizationResult rejected(List<String> reasons) {
        return new AuthorizationContextNormalizationResult(null, false, reasons);
    }
}
