package io.acra.core.session;

import io.acra.core.active.evidence.EvidenceReferenceValidation;
import io.acra.core.security.TokenFingerprint;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class S10SessionSecurityAssessmentEvaluator {

    public SessionSecurityAssessment evaluate(
            SessionCorrelationResult correlation,
            EvidenceReferenceValidation provenance) {

        if (correlation == null) throw new IllegalArgumentException("correlation required");

        List<String> reasons = new ArrayList<>(correlation.reasons());
        if (provenance == null || !provenance.valid()) {
            if (provenance == null) reasons.add("SESSION_PROVENANCE_VALIDATION_MISSING");
            else reasons.addAll(provenance.reasons());
            return assessment(
                    correlation,
                    SessionSecurityAssessmentState.INCONCLUSIVE,
                    false,
                    "Session context cannot be assessed because provenance is incomplete or invalid",
                    reasons);
        }

        return switch (correlation.state()) {
            case BASELINE -> assessment(
                    correlation,
                    SessionSecurityAssessmentState.BASELINE,
                    correlation.currentIdentityVerified(),
                    "First supported session observation establishes context only",
                    reasons);
            case STABLE -> assessment(
                    correlation,
                    SessionSecurityAssessmentState.STABLE,
                    correlation.previousIdentityVerified() && correlation.currentIdentityVerified(),
                    "Verified session context is unchanged",
                    reasons);
            case TOKEN_ROTATED -> assessment(
                    correlation,
                    SessionSecurityAssessmentState.SAFE_ROTATION,
                    correlation.previousIdentityVerified() && correlation.currentIdentityVerified(),
                    "Token fingerprint changed while verified principal, role, tenant and scope remained stable",
                    reasons);
            case CONTEXT_DRIFT -> {
                boolean verified = correlation.previousIdentityVerified()
                        && correlation.currentIdentityVerified()
                        && !correlation.driftDimensions().isEmpty();
                yield assessment(
                        correlation,
                        verified ? SessionSecurityAssessmentState.CANDIDATE
                                : SessionSecurityAssessmentState.INCONCLUSIVE,
                        verified,
                        verified
                                ? "Evidence-backed verified session context changed; analyst review is required and this is not an automatically confirmed vulnerability"
                                : "Session context changed but verified identity evidence is insufficient",
                        reasons);
            }
            case UNVERIFIED_IDENTITY, INCONCLUSIVE -> assessment(
                    correlation,
                    SessionSecurityAssessmentState.INCONCLUSIVE,
                    false,
                    "Unverified or incomparable session context cannot be promoted",
                    reasons);
        };
    }

    private SessionSecurityAssessment assessment(
            SessionCorrelationResult correlation,
            SessionSecurityAssessmentState state,
            boolean verified,
            String rationale,
            List<String> reasons) {

        String material = correlation.correlationId() + "|" + state.name() + "|" + verified;
        String assessmentId = "s10-session-assessment-"
                + TokenFingerprint.sha256(material).substring(0, 24);
        return new SessionSecurityAssessment(
                assessmentId,
                correlation.sessionId(),
                correlation.previousObservationId(),
                correlation.currentObservationId(),
                state,
                correlation.tokenRotated(),
                verified,
                correlation.driftDimensions(),
                correlation.evidenceIds(),
                rationale,
                List.copyOf(new LinkedHashSet<>(reasons)));
    }
}
