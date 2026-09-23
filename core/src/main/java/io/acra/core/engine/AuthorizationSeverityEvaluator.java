package io.acra.core.engine;

import io.acra.core.domain.finding.AuthorizationImpactProfile;
import io.acra.core.domain.finding.AuthorizationRiskAssessment;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingConfidence;
import io.acra.core.domain.finding.FindingSeverity;
import io.acra.core.security.TokenFingerprint;
import java.util.ArrayList;
import java.util.List;

public final class AuthorizationSeverityEvaluator {

    public AuthorizationRiskAssessment evaluate(FindingCandidate candidate, AuthorizationImpactProfile impact) {
        AuthorizationImpactProfile profile = impact == null ? AuthorizationImpactProfile.none() : impact;
        List<String> factors = new ArrayList<>(profile.suppliedReasons());
        if (candidate == null || candidate.state() != FindingCandidateState.CANDIDATE) {
            return new AuthorizationRiskAssessment("risk-" + TokenFingerprint.sha256("no-candidate").substring(0, 24),
                    candidate == null ? "" : candidate.candidateId(), FindingSeverity.INFO,
                    FindingConfidence.INSUFFICIENT, 0, "No verified finding candidate to prioritize", factors);
        }

        int score = 20;
        if (profile.confidentialityImpact()) { score += 15; factors.add("CONFIDENTIALITY"); }
        if (profile.integrityImpact()) { score += 15; factors.add("INTEGRITY"); }
        if (profile.availabilityImpact()) { score += 10; factors.add("AVAILABILITY"); }
        if (profile.crossTenantImpact()) { score += 25; factors.add("CROSS_TENANT"); }
        if (profile.privilegedFunction()) { score += 20; factors.add("PRIVILEGED_FUNCTION"); }
        if (profile.bulkImpact()) { score += 10; factors.add("BULK_IMPACT"); }
        score = Math.min(100, score);

        FindingSeverity severity = score >= 80 ? FindingSeverity.CRITICAL
                : score >= 60 ? FindingSeverity.HIGH
                : score >= 35 ? FindingSeverity.MEDIUM : FindingSeverity.LOW;
        FindingConfidence confidence = switch (candidate.confidence()) {
            case "HIGH" -> FindingConfidence.HIGH;
            case "MEDIUM" -> FindingConfidence.MEDIUM;
            case "LOW" -> FindingConfidence.LOW;
            default -> FindingConfidence.INSUFFICIENT;
        };
        String material = candidate.candidateId() + "|" + score + "|" + severity + "|" + confidence;
        return new AuthorizationRiskAssessment("risk-" + TokenFingerprint.sha256(material).substring(0, 24),
                candidate.candidateId(), severity, confidence, score,
                "Deterministic internal prioritization from explicitly supplied impact facts; not a CVSS score",
                factors.stream().distinct().sorted().toList());
    }
}
