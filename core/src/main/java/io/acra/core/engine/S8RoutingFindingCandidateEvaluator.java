package io.acra.core.engine;

import io.acra.core.active.evidence.EvidenceReferenceValidation;
import io.acra.core.active.evidence.EvidenceReferenceValidator;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.route.RouteAuthorizationAssessment;
import io.acra.core.route.RouteAuthorizationAssessmentState;
import io.acra.core.security.TokenFingerprint;
import java.util.Set;
import java.util.TreeSet;

public final class S8RoutingFindingCandidateEvaluator {
    private final EvidenceReferenceValidator evidenceValidator;

    public S8RoutingFindingCandidateEvaluator(EvidenceReferenceValidator evidenceValidator) {
        if (evidenceValidator == null) throw new IllegalArgumentException("evidenceValidator required");
        this.evidenceValidator = evidenceValidator;
    }

    public FindingCandidate evaluate(RouteAuthorizationAssessment assessment, S8RoutingAnalysisRequest request) {
        if (assessment == null) throw new IllegalArgumentException("assessment required");
        if (request == null) throw new IllegalArgumentException("request required");

        EvidenceReferenceValidation evidence = evidenceValidator.validateEvidenceReferences(
                request.evidenceIds(), request.executionId(), request.testId(), request.projectId());
        EvidenceReferenceValidation observation = evidenceValidator.validateObservation(
                request.observationId(), request.executionId(), request.testId(), request.projectId());

        Set<String> contradictions = new TreeSet<>();
        contradictions.addAll(evidence.reasons());
        contradictions.addAll(observation.reasons());

        Set<String> dimensions = new TreeSet<>();
        dimensions.add("ROUTING_NORMALIZATION");
        if (request.transition().pathDivergence()
                != io.acra.core.route.RouteNormalizationDivergenceKind.NONE) {
            dimensions.add("PATH_REPRESENTATION");
        }
        if (request.transition().methodChanged()) dimensions.add("HTTP_METHOD");
        if (request.transition().hostChanged()) dimensions.add("HOST_ROUTING");
        if (request.transition().apiVersionChanged()) dimensions.add("API_VERSION");
        if (request.transition().authorizationChanged()) dimensions.add("AUTHORIZATION_BOUNDARY");

        boolean provenanceVerified = evidence.valid() && observation.valid();
        FindingCandidateState state;
        String confidence;
        String rationale;

        if (!provenanceVerified || assessment.state() == RouteAuthorizationAssessmentState.INCONCLUSIVE) {
            state = FindingCandidateState.INCONCLUSIVE;
            confidence = "INSUFFICIENT";
            rationale = "Routing differential evidence/provenance or attribution is incomplete";
        } else if (assessment.violationCandidate()) {
            state = FindingCandidateState.CANDIDATE;
            confidence = "HIGH";
            rationale = "Verified routing change produced ALLOW where explicit policy expected DENY";
        } else {
            state = FindingCandidateState.REJECTED;
            confidence = "MEDIUM";
            rationale = "Verified routing evidence did not establish a DENY-to-ALLOW authorization mismatch";
        }

        String material = request.projectId() + "|" + request.testId() + "|" + assessment.assessmentId()
                + "|" + state + "|" + request.transition().fromStage() + "|" + request.transition().toStage();
        String candidateId = "fc-s8-" + TokenFingerprint.sha256(material).substring(0, 24);
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
                request.expectedDecision(),
                request.observedDecision(),
                request.evidenceIds(),
                contradictions.stream().toList(),
                java.util.List.of(request.policyReference()),
                confidence,
                rationale + "; candidate is not an automatically confirmed vulnerability",
                fingerprint);
    }
}
