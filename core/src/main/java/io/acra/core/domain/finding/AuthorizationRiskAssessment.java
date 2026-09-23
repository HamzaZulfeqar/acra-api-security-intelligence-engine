package io.acra.core.domain.finding;

import java.util.List;

public record AuthorizationRiskAssessment(
        String riskId,
        String candidateId,
        FindingSeverity severity,
        FindingConfidence confidence,
        int internalRiskScore,
        String rationale,
        List<String> factors) {

    public AuthorizationRiskAssessment {
        if (severity == null) severity = FindingSeverity.INFO;
        if (confidence == null) confidence = FindingConfidence.INSUFFICIENT;
        if (internalRiskScore < 0 || internalRiskScore > 100) throw new IllegalArgumentException("internalRiskScore");
        factors = List.copyOf(factors == null ? List.of() : factors);
    }
}
