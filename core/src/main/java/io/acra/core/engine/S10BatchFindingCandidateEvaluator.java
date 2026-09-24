package io.acra.core.engine;

import io.acra.core.active.evidence.EvidenceReferenceValidation;
import io.acra.core.active.evidence.EvidenceReferenceValidator;
import io.acra.core.batch.BatchItemAuthorizationAssessment;
import io.acra.core.batch.S10BatchFindingRequest;
import io.acra.core.domain.authorization.PolicyValidationState;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.security.TokenFingerprint;
import java.util.Set;
import java.util.TreeSet;

public final class S10BatchFindingCandidateEvaluator {
    private final EvidenceReferenceValidator evidenceValidator;

    public S10BatchFindingCandidateEvaluator(EvidenceReferenceValidator evidenceValidator) {
        if (evidenceValidator == null) throw new IllegalArgumentException("evidenceValidator required");
        this.evidenceValidator = evidenceValidator;
    }

    public FindingCandidate evaluate(
            BatchItemAuthorizationAssessment assessment,
            S10BatchFindingRequest request) {

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
                && request.batchId().equals(assessment.batchId())
                && request.itemKey().equals(assessment.itemKey())
                && request.resourceId().equals(assessment.resourceId())
                && request.action().equals(assessment.action())
                && request.policyReference().equals(assessment.policyReference())
                && request.expectedDecision() == assessment.expectedDecision()
                && request.observedDecision() == assessment.observedDecision();

        if (!requestConsistent) contradictions.add("BATCH_FINDING_REQUEST_MISMATCH");

        boolean provenanceVerified = evidence.valid() && observation.valid();
        FindingCandidateState state;
        String confidence;
        String rationale;

        if (!provenanceVerified
                || !requestConsistent
                || assessment.state() == PolicyValidationState.INCONCLUSIVE
                || assessment.reasons().stream().anyMatch(value -> value.contains("AMBIGUOUS"))) {
            state = FindingCandidateState.INCONCLUSIVE;
            confidence = "INSUFFICIENT";
            rationale = "Batch-item authorization evidence, provenance, or policy attribution is insufficient";
        } else if (assessment.violationCandidate()) {
            state = FindingCandidateState.CANDIDATE;
            confidence = "HIGH";
            rationale = "Verified batch-item policy expected DENY while the controlled observation recorded ALLOW";
        } else {
            state = FindingCandidateState.REJECTED;
            confidence = "MEDIUM";
            rationale = "Verified batch-item authorization evidence did not establish a DENY-to-ALLOW mismatch";
        }

        Set<String> dimensions = new TreeSet<>();
        dimensions.add("BATCH_AUTHORIZATION");
        dimensions.add("BATCH_ITEM");

        String material = request.projectId() + "|" + request.testId() + "|" + assessment.assessmentId()
                + "|" + assessment.batchId() + "|" + assessment.itemKey() + "|" + state;
        String candidateId = "fc-s10-batch-" + TokenFingerprint.sha256(material).substring(0, 20);

        FindingFingerprint fingerprint = FindingFingerprint.of(
                request.endpoint(),
                request.resourceId(),
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
