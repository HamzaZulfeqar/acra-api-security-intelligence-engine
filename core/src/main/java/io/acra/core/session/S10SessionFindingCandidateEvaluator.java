package io.acra.core.session;

import io.acra.core.active.evidence.EvidenceReferenceValidation;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.security.TokenFingerprint;
import java.util.Set;
import java.util.TreeSet;

public final class S10SessionFindingCandidateEvaluator {
    private final S10SessionEvidenceValidator evidenceValidator;

    public S10SessionFindingCandidateEvaluator(S10SessionEvidenceValidator evidenceValidator) {
        if (evidenceValidator == null) throw new IllegalArgumentException("evidenceValidator required");
        this.evidenceValidator = evidenceValidator;
    }

    public FindingCandidate evaluate(
            SessionSecurityAssessment assessment,
            SessionCorrelationResult correlation,
            S10SessionFindingRequest request) {

        if (assessment == null) throw new IllegalArgumentException("assessment required");
        if (correlation == null) throw new IllegalArgumentException("correlation required");
        if (request == null) throw new IllegalArgumentException("request required");

        EvidenceReferenceValidation provenance = evidenceValidator.validate(request.evidenceBinding());
        Set<String> contradictions = new TreeSet<>(provenance.reasons());

        boolean consistent = assessment.currentObservationId().equals(request.observationId())
                && assessment.sessionId().equals(correlation.sessionId())
                && assessment.previousObservationId().equals(correlation.previousObservationId())
                && assessment.currentObservationId().equals(correlation.currentObservationId())
                && assessment.tokenRotated() == correlation.tokenRotated()
                && assessment.driftDimensions().equals(correlation.driftDimensions());

        if (!consistent) contradictions.add("SESSION_FINDING_REQUEST_MISMATCH");

        FindingCandidateState state;
        String confidence;
        String rationale;

        if (!provenance.valid()
                || !consistent
                || assessment.state() == SessionSecurityAssessmentState.INCONCLUSIVE) {
            state = FindingCandidateState.INCONCLUSIVE;
            confidence = "INSUFFICIENT";
            rationale = "Session context evidence, provenance, or assessment attribution is insufficient";
        } else if (assessment.reviewCandidate()) {
            state = FindingCandidateState.CANDIDATE;
            confidence = "HIGH";
            rationale = "Verified session context changed across correlated observations and requires analyst review";
        } else {
            state = FindingCandidateState.REJECTED;
            confidence = "MEDIUM";
            rationale = "Verified session evidence did not establish review-worthy context drift";
        }

        Set<String> dimensions = new TreeSet<>();
        dimensions.add("AUTHENTICATION_SESSION");
        if (correlation.tokenRotated()) dimensions.add("TOKEN_ROTATION");
        for (SessionContextDimension dimension : correlation.driftDimensions()) {
            dimensions.add("SESSION_CONTEXT_" + dimension.name());
        }

        String material = request.projectId() + "|" + request.testId() + "|" + assessment.assessmentId()
                + "|" + request.observationId() + "|" + state.name();
        String candidateId = "fc-s10-" + TokenFingerprint.sha256(material).substring(0, 24);

        FindingFingerprint fingerprint = FindingFingerprint.of(
                request.endpoint(),
                "auth-context:" + correlation.sessionId(),
                request.principalId(),
                request.tenantId(),
                String.join("+", dimensions),
                state.name());

        return new FindingCandidate(
                candidateId,
                state,
                request.projectId(),
                java.util.List.of(request.testId()),
                java.util.List.of(request.executionId()),
                java.util.List.of(request.observationId()),
                java.util.List.of(assessment.assessmentId()),
                dimensions.stream().toList(),
                request.endpoint(),
                "auth-context:" + correlation.sessionId(),
                request.principalId(),
                request.tenantId(),
                AuthorizationDecision.UNKNOWN,
                AuthorizationDecision.UNKNOWN,
                request.evidenceIds(),
                contradictions.stream().toList(),
                java.util.List.of(request.ruleReference()),
                confidence,
                rationale + "; candidate is not an automatically confirmed vulnerability",
                fingerprint);
    }
}
