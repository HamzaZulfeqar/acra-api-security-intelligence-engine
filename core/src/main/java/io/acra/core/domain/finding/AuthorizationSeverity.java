package io.acra.core.domain.finding;

import io.acra.core.security.UniversalRedactor;
import java.util.List;

/** Deterministic severity result kept independent from evidence confidence. */
public record AuthorizationSeverity(
        SeverityLevel level,
        int score,
        String rationale,
        List<String> factors) {
    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public AuthorizationSeverity {
        level = level == null ? SeverityLevel.UNKNOWN : level;
        if (score < 0) throw new IllegalArgumentException("score must be non-negative");
        rationale = REDACTOR.redactText(rationale == null ? "" : rationale);
        factors = List.copyOf(factors == null ? List.<String>of() : factors).stream()
                .map(value -> REDACTOR.redactText(value == null ? "" : value))
                .distinct().sorted().toList();
    }

    public static AuthorizationSeverity unknown(String reason) {
        return new AuthorizationSeverity(SeverityLevel.UNKNOWN, 0, reason, List.of());
    }
}
