package io.acra.core.domain.finding;

import io.acra.core.security.UniversalRedactor;
import java.util.List;

public record AuthorizationRiskAssessment(
        String riskId,
        String candidateId,
        FindingSeverity severity,
        FindingConfidence confidence,
        int internalRiskScore,
        String rationale,
        List<String> factors) {

    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public AuthorizationRiskAssessment {
        riskId = safe(riskId);
        candidateId = safe(candidateId);
        if (severity == null) severity = FindingSeverity.INFO;
        if (confidence == null) confidence = FindingConfidence.INSUFFICIENT;
        if (internalRiskScore < 0 || internalRiskScore > 100) throw new IllegalArgumentException("internalRiskScore");
        rationale = safe(rationale);
        factors = List.copyOf(factors == null ? List.<String>of() : factors).stream()
                .map(AuthorizationRiskAssessment::safe).distinct().sorted().toList();
    }

    private static String safe(String value) {
        return REDACTOR.redactText(value == null ? "" : value);
    }
}
