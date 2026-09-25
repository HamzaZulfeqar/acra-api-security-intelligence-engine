package io.acra.core.engine;

import io.acra.core.active.evidence.EvidenceReferenceValidation;
import io.acra.core.active.evidence.EvidenceReferenceValidator;
import io.acra.core.domain.authorization.PolicyValidationState;
import io.acra.core.domain.authorization.PropertyAuthorizationAssessment;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.property.S9PropertyFindingRequest;
import io.acra.core.security.TokenFingerprint;
import java.util.Set;
import java.util.TreeSet;

public final class S9PropertyFindingCandidateEvaluator {
    private final EvidenceReferenceValidator evidenceValidator;

    public S9PropertyFindingCandidateEvaluator(EvidenceReferenceValidator evidenceValidator) {
        if (evidenceValidator == null) throw new IllegalArgumentException("evidenceValidator required");
        this.evidenceValidator = evidenceValidator;
    }

    public FindingCandidate evaluate(
            PropertyAuthorizationAssessment assessment,
            S9PropertyFindingRequest request) {

        if (assessment == null) throw new IllegalArgumentException("assessment required");
        if (request == null) throw new IllegalArgumentException("request required");

        EvidenceReferenceValidation evidence = evidenceValidator.validateEvidenceReferences(
                request.evidenceIds(),
                request.executionId(),
                request.testId(),
                request.projectId());
        EvidenceReferenceValidation observation = evidenceValidator.validateObservation(
                request.observationId(),
                request.executionId(),
                request.testId(),
                request.projectId());

        Set<String> contradictions = new TreeSet<>();
        contradictions.addAll(evidence.reasons());
        contradictions.addAll(observation.reasons());
        if (assessment.state() == PolicyValidationState.CONFLICTING
                && !assessment.violationCandidate()) {
            contradictions.addAll(assessment.reasons());
        }

        Set<String> dimensions = new TreeSet<>();
        dimensions.add("PROPERTY");
        dimensions.add("PROPERTY_" + assessment.operation());

        boolean provenanceVerified = evidence.valid() && observation.valid();
        boolean requestConsistent = request.endpoint().equals(assessment.endpoint())
                && request.policyReference().equals(assessment.policyReference())
                && request.expectedDecision() == assessment.expectedDecision()
                && request.observedDecision() == assessment.observedDecision();

        if (!requestConsistent) contradictions.add("PROPERTY_FINDING_REQUEST_MISMATCH");

        FindingCandidateState state;
        String confidence;
        String rationale;

        if (!provenanceVerified
                || !requestConsistent
                || assessment.state() == PolicyValidationState.INCONCLUSIVE
                || assessment.reasons().contains("PROPERTY_POLICY_AMBIGUOUS")) {
            state = FindingCandidateState.INCONCLUSIVE;
            confidence = "INSUFFICIENT";
            rationale = "Property authorization evidence, provenance, or policy attribution is insufficient";
        } else if (assessment.violationCandidate()) {
            state = FindingCandidateState.CANDIDATE;
            confidence = "HIGH";
            rationale = "Verified property policy expected DENY while the controlled observation recorded ALLOW";
        } else {
            state = FindingCandidateState.REJECTED;
            confidence = "MEDIUM";
            rationale = "Verified property authorization evidence did not establish a DENY-to-ALLOW mismatch";
        }

        String material = request.projectId() + "|" + request.testId() + "|" + assessment.assessmentId()
                + "|" + assessment.property() + "|" + assessment.operation() + "|" + state;
        String candidateId = "fc-s9-" + TokenFingerprint.sha256(material).substring(0, 24);

        FindingFingerprint fingerprint = FindingFingerprint.of(
                request.endpoint(),
                request.resourceId() + "#" + assessment.property(),
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
                request.resourceId(),
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
