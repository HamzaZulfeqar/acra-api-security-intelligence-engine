package io.acra.core.engine;

import io.acra.core.active.evidence.EvidenceReferenceValidation;
import io.acra.core.active.evidence.EvidenceReferenceValidator;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.domain.workflow.WorkflowAuthorizationResolution;
import io.acra.core.domain.workflow.WorkflowTransitionAssessment;
import io.acra.core.domain.workflow.WorkflowTransitionAssessmentState;
import io.acra.core.security.TokenFingerprint;
import java.util.Set;
import java.util.TreeSet;

public final class S7WorkflowFindingCandidateEvaluator {
    private final EvidenceReferenceValidator evidenceValidator;

    public S7WorkflowFindingCandidateEvaluator(EvidenceReferenceValidator evidenceValidator) {
        if (evidenceValidator == null) throw new IllegalArgumentException("evidenceValidator required");
        this.evidenceValidator = evidenceValidator;
    }

    public FindingCandidate evaluate(
            WorkflowAuthorizationResolution resolution,
            WorkflowTransitionAssessment assessment,
            S7WorkflowAnalysisRequest request) {
        if (resolution == null) throw new IllegalArgumentException("resolution required");
        if (assessment == null) throw new IllegalArgumentException("assessment required");
        if (request == null) throw new IllegalArgumentException("request required");

        EvidenceReferenceValidation evidence = evidenceValidator.validateEvidenceReferences(
                resolution.evidenceIds(), request.executionId(), request.testId(), request.projectId());
        EvidenceReferenceValidation observation = evidenceValidator.validateObservation(
                request.observationId(), request.executionId(), request.testId(), request.projectId());

        Set<String> contradictions = new TreeSet<>();
        contradictions.addAll(evidence.reasons());
        contradictions.addAll(observation.reasons());
        if (assessment.state() == WorkflowTransitionAssessmentState.CONFLICTING) {
            contradictions.addAll(assessment.reasons());
        }

        Set<String> dimensions = new TreeSet<>();
        dimensions.add("WORKFLOW");
        if (!resolution.matchedBindingIds().isEmpty()
                || resolution.reasons().stream().anyMatch(value -> value.contains("TOKEN_BINDING"))) {
            dimensions.add("TOKEN_BINDING");
        }
        if (!resolution.delegationIds().isEmpty()
                || resolution.reasons().stream().anyMatch(value -> value.contains("DELEGATION"))) {
            dimensions.add("DELEGATION");
        }
        if (resolution.reasons().stream().anyMatch(value -> value.contains("ROLE_SEPARATION"))) {
            dimensions.add("SEPARATION_OF_DUTIES");
        }
        if (resolution.reasons().stream().anyMatch(value -> value.contains("APPROVAL"))) {
            dimensions.add("APPROVAL");
        }

        boolean provenanceVerified = evidence.valid() && observation.valid();
        FindingCandidateState state;
        String confidence;
        String rationale;

        if (!provenanceVerified
                || assessment.state() == WorkflowTransitionAssessmentState.CONFLICTING
                || assessment.state() == WorkflowTransitionAssessmentState.INCONCLUSIVE) {
            state = FindingCandidateState.INCONCLUSIVE;
            confidence = "INSUFFICIENT";
            rationale = "Workflow authorization evidence, provenance, or policy resolution is insufficient";
        } else if (assessment.violationCandidate()) {
            state = FindingCandidateState.CANDIDATE;
            confidence = "HIGH";
            rationale = "Verified workflow policy expected DENY while the controlled observation recorded ALLOW";
        } else {
            state = FindingCandidateState.REJECTED;
            confidence = "MEDIUM";
            rationale = "Verified workflow authorization evidence did not establish a policy mismatch";
        }

        String material = request.projectId() + "|" + request.testId() + "|" + resolution.resolutionId()
                + "|" + assessment.assessmentId() + "|" + state;
        String candidateId = "fc-s7-" + TokenFingerprint.sha256(material).substring(0, 24);
        FindingFingerprint fingerprint = FindingFingerprint.of(
                request.endpoint(),
                request.workflowRequest().resourceId(),
                request.workflowRequest().principalId(),
                request.workflowRequest().tenantId(),
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
                request.workflowRequest().resourceId(),
                request.workflowRequest().principalId(),
                request.workflowRequest().tenantId(),
                resolution.expectedDecision(),
                resolution.observedDecision(),
                resolution.evidenceIds(),
                contradictions.stream().toList(),
                java.util.List.of(resolution.policyFingerprint()),
                confidence,
                rationale + "; candidate is not an automatically confirmed vulnerability",
                fingerprint);
    }
}
