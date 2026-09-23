package io.acra.core.engine;

import io.acra.core.domain.finding.AuthorizationImpact;
import io.acra.core.domain.finding.AuthorizationSeverity;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.ImpactLevel;
import io.acra.core.domain.finding.SeverityLevel;
import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic authorization severity from explicit impact metadata.
 * Severity is independent from FindingCandidate confidence and endpoint test priority.
 */
public final class AuthorizationSeverityEvaluator {
    public AuthorizationSeverity evaluate(FindingCandidate candidate, AuthorizationImpact impact) {
        if (candidate == null || candidate.state() != FindingCandidateState.SUPPORTED) {
            return AuthorizationSeverity.unknown("Supported finding candidate required before severity evaluation");
        }
        if (impact == null || impact.resourceImpact() == ImpactLevel.UNKNOWN) {
            return AuthorizationSeverity.unknown("Explicit resource impact is required; endpoint priority is not severity");
        }

        int score = switch (impact.resourceImpact()) {
            case LOW -> 1;
            case MEDIUM -> 2;
            case HIGH -> 3;
            case CRITICAL -> 4;
            case UNKNOWN -> 0;
        };
        List<String> factors = new ArrayList<>();
        factors.add("resourceImpact=" + impact.resourceImpact().name());

        if (impact.writeCapable()) {
            score += 1;
            factors.add("writeCapable");
        }
        if (impact.destructive()) {
            score += 2;
            factors.add("destructive");
        }
        if (impact.crossTenant()) {
            score += 1;
            factors.add("crossTenant");
        }
        if (impact.privilegedFunction()) {
            score += 1;
            factors.add("privilegedFunction");
        }
        if (impact.sensitiveProperty()) {
            score += 1;
            factors.add("sensitiveProperty");
        }
        if (impact.bulkExposure()) {
            score += 1;
            factors.add("bulkExposure");
        }

        SeverityLevel level;
        if (impact.resourceImpact() == ImpactLevel.CRITICAL || score >= 7) level = SeverityLevel.CRITICAL;
        else if (impact.resourceImpact() == ImpactLevel.HIGH || score >= 5) level = SeverityLevel.HIGH;
        else if (impact.resourceImpact() == ImpactLevel.MEDIUM || score >= 3) level = SeverityLevel.MEDIUM;
        else if (score >= 1) level = SeverityLevel.LOW;
        else level = SeverityLevel.INFORMATIONAL;

        return new AuthorizationSeverity(level, score,
                "Severity derived from explicit authorization-impact metadata; confidence is evaluated separately",
                List.copyOf(factors));
    }
}
