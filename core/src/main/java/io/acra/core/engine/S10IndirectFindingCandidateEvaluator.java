package io.acra.core.engine;

import io.acra.core.active.evidence.EvidenceReferenceValidation;
import io.acra.core.active.evidence.EvidenceReferenceValidator;
import io.acra.core.domain.authorization.PolicyValidationState;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.reference.IndirectReferenceAuthorizationAssessment;
import io.acra.core.reference.S10IndirectFindingRequest;
import io.acra.core.security.TokenFingerprint;
import java.util.Set;
import java.util.TreeSet;

public final class S10IndirectFindingCandidateEvaluator {
    private final EvidenceReferenceValidator evidenceValidator;

    public S10IndirectFindingCandidateEvaluator(EvidenceReferenceValidator evidenceValidator) {
        if (evidenceValidator == null) throw new IllegalArgumentException("evidenceValidator required");
        this.evidenceValidator = evidenceValidator;
    }

    public FindingCandidate evaluate(
            IndirectReferenceAuthorizationAssessment assessment,
            S10IndirectFindingRequest request) {

        if (assessment == null) throw new IllegalArgumentException("assessment required");
        if (request == null) throw new IllegalArgumentException("request required");

        EvidenceReferenceValidation evidence = evidenceValidator.validateEvidenceReferences(
                request.evidenceIds(), request.executionId(), request.testId(), request.projectId());
        EvidenceReferenceValidation observation = evidenceValidator.validateObservation(
                request.observationId(), request.executionId(), request.testId(), request.projectId());

        Set<String> contradictions = new TreeSet<>();
        contradictions.addAll(evidence.reasons());
        contradictions.addAll(observation.reasons());
        if (assessment.state() == PolicyValidationState.CONFLICTING
                && !assessment.violationCandidate()) {
            contradictions.addAll(assessment.reasons());
        }

        boolean requestConsistent = request.endpoint().equals(assessment.endpoint())
                && request.referenceFingerprint().equals(assessment.referenceFingerprint())
                && request.referenceKind().equals(assessment.referenceKind())
                && request.resolvedResourceId().equals(assessment.resolvedResourceId())
                && request.action().equals(assessment.action())
                && request.policyReference().equals(assessment.policyReference())
                && request.expectedDecision() == assessment.expectedDecision()
                && request.observedDecision() == assessment.observedDecision();

        if (!requestConsistent) contradictions.add("INDIRECT_FINDING_REQUEST_MISMATCH");

        boolean provenanceVerified = evidence.valid() && observation.valid();
        FindingCandidateState state;
        String confidence;
        String rationale;

        if (!provenanceVerified
                || !requestConsistent
                || assessment.state() == PolicyValidationState.INCONCLUSIVE
                || assessment.reasons().stream().anyMatch(value -> value.contains("AMBIGUOUS")
                        || value.contains("RESOLUTION_CONFLICT"))) {
            state = FindingCandidateState.INCONCLUSIVE;
            confidence = "INSUFFICIENT";
            rationale = "Indirect-reference evidence, provenance, resolution, or policy attribution is insufficient";
        } else if (assessment.violationCandidate()) {
            state = FindingCandidateState.CANDIDATE;
            confidence = "HIGH";
            rationale = "Verified resolved-target policy expected DENY while the controlled observation recorded ALLOW";
        } else {
            state = FindingCandidateState.REJECTED;
            confidence = "MEDIUM";
            rationale = "Verified resolved-target authorization evidence did not establish a DENY-to-ALLOW mismatch";
        }

        Set<String> dimensions = new TreeSet<>();
        dimensions.add("INDIRECT_REFERENCE_AUTHORIZATION");
        dimensions.add("RESOLVED_TARGET");

        String material = request.projectId() + "|" + request.testId() + "|" + assessment.assessmentId()
                + "|" + assessment.referenceFingerprint() + "|" + assessment.resolvedResourceId() + "|" + state;
        String candidateId = "fc-s10-indirect-" + TokenFingerprint.sha256(material).substring(0, 17);

        FindingFingerprint fingerprint = FindingFingerprint.of(
                request.endpoint(),
                request.resolvedResourceId(),
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
                request.resolvedResourceId(),
                request.principalId(),
                request.tenantId(),
                assessment.expectedDecision(),
                assessment.observedDecision(),
                request.evidenceIds(),
                contradictions.stream().toList(),
                java.util.List.of(assessment.policyReference()),
                confidence,
                rationale + "; candidate is not an automatically confirmed vulnerability",
                fingerprint);
    }
}
