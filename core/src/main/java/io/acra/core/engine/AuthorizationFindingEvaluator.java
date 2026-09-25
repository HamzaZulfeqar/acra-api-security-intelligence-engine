package io.acra.core.engine;

import io.acra.core.domain.authorization.AuthorizationContextAssessment;
import io.acra.core.domain.authorization.AuthorizationCorrelationEnvelope;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.BflaAssessment;
import io.acra.core.domain.authorization.BflaAssessmentStatus;
import io.acra.core.domain.authorization.BolaAssessment;
import io.acra.core.domain.authorization.BolaAssessmentStatus;
import io.acra.core.domain.authorization.CorrelationState;
import io.acra.core.domain.authorization.PropertyAuthorizationAssessment;
import io.acra.core.domain.authorization.TenantAuthorizationAssessment;
import io.acra.core.domain.authorization.WorkflowAuthorizationAssessment;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.security.TokenFingerprint;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public final class AuthorizationFindingEvaluator {

    public FindingCandidate evaluate(AuthorizationContextAssessment contextAssessment, BolaAssessment bola,
            BflaAssessment bfla, TenantAuthorizationAssessment tenant, WorkflowAuthorizationAssessment workflow,
            PropertyAuthorizationAssessment property, AuthorizationCorrelationEnvelope correlation,
            String endpoint) {

        Set<String> dimensions = new TreeSet<>();
        Set<String> assessmentIds = new TreeSet<>();
        Set<String> evidence = new TreeSet<>();
        Set<String> policyReferences = new TreeSet<>();
        int signals = 0;

        if (bola != null) {
            assessmentIds.add(bola.assessmentId());
            evidence.addAll(bola.evidenceIds());
            if (bola.status() == BolaAssessmentStatus.BOLA_CANDIDATE) {
                dimensions.add("BOLA");
                signals++;
            }
        }
        if (bfla != null) {
            assessmentIds.add(bfla.assessmentId());
            evidence.addAll(bfla.evidenceIds());
            if (bfla.status() == BflaAssessmentStatus.BFLA_CANDIDATE) {
                dimensions.add("BFLA");
                signals++;
            }
        }
        if (tenant != null) {
            assessmentIds.add(tenant.assessmentId());
            evidence.addAll(tenant.evidenceIds());
            policyReferences.add(tenant.policyReference());
            if (tenant.violationCandidate()) {
                dimensions.add("TENANT");
                signals++;
            }
        }
        if (workflow != null) {
            assessmentIds.add(workflow.assessmentId());
            evidence.addAll(workflow.evidenceIds());
            policyReferences.add(workflow.policyReference());
            if (workflow.violationCandidate()) {
                dimensions.add("WORKFLOW");
                signals++;
            }
        }
        if (property != null) {
            assessmentIds.add(property.assessmentId());
            evidence.addAll(property.evidenceIds());
            policyReferences.add(property.policyReference());
            if (property.violationCandidate()) {
                dimensions.add("PROPERTY");
                signals++;
            }
        }

        FindingCandidateState state;
        String rationale;
        boolean blocked = contextAssessment == null || !contextAssessment.evidenceVerified()
                || contextAssessment.resolutionStatus() != io.acra.core.domain.authorization.ContextStatus.RESOLVED
                || correlation == null || correlation.aggregate() == null
                || correlation.aggregate().state() == CorrelationState.CONFLICTING
                || correlation.aggregate().state() == CorrelationState.INSUFFICIENT
                || correlation.aggregate().state() == CorrelationState.INCONCLUSIVE;
        if (blocked) {
            state = FindingCandidateState.INCONCLUSIVE;
            rationale = "Verified context, evidence, or correlation is insufficient for a finding candidate";
        } else if (signals > 0) {
            state = FindingCandidateState.CANDIDATE;
            rationale = "One or more verified authorization assessments observed ALLOW where supplied policy expected DENY";
        } else {
            state = FindingCandidateState.REJECTED;
            rationale = "Verified assessments did not establish an authorization-policy mismatch";
        }

        AuthorizationDecision expected = contextAssessment == null || contextAssessment.normalizedContext() == null
                ? AuthorizationDecision.UNKNOWN : contextAssessment.normalizedContext().expectedDecision();
        AuthorizationDecision observed = contextAssessment == null || contextAssessment.normalizedContext() == null
                ? AuthorizationDecision.UNKNOWN : contextAssessment.normalizedContext().observedDecision();
        String principal = contextAssessment == null || contextAssessment.normalizedContext() == null
                || contextAssessment.normalizedContext().principal() == null ? ""
                : contextAssessment.normalizedContext().principal().principalId();
        String resource = contextAssessment == null || contextAssessment.normalizedContext() == null
                || contextAssessment.normalizedContext().resource() == null ? ""
                : contextAssessment.normalizedContext().resource().resourceId();
        String tenantRelationship = tenantRelationship(contextAssessment);

        List<String> tests = contextAssessment == null ? List.of() : List.of(contextAssessment.testId());
        List<String> executions = contextAssessment == null ? List.of() : List.of(contextAssessment.executionId());
        List<String> observations = contextAssessment == null ? List.of() : List.of(contextAssessment.observationId());
        List<String> conflicts = correlation == null || correlation.aggregate() == null
                ? List.of() : correlation.aggregate().conflicts();
        String confidence = state == FindingCandidateState.INCONCLUSIVE ? "INSUFFICIENT"
                : state == FindingCandidateState.CANDIDATE && correlation.independentlyCorroborated() && signals > 1
                    ? "HIGH" : "MEDIUM";
        String projectId = contextAssessment == null ? "" : contextAssessment.projectId();
        String material = String.join("|", projectId, String.valueOf(tests), String.valueOf(assessmentIds),
                state.name(), String.valueOf(dimensions));
        String candidateId = "fc-" + TokenFingerprint.sha256(material).substring(0, 24);
        FindingFingerprint fingerprint = FindingFingerprint.of(endpoint, resource, principal, tenantRelationship,
                String.join("+", dimensions), state.name());

        return new FindingCandidate(candidateId, state, projectId, tests, executions, observations,
                List.copyOf(assessmentIds), List.copyOf(dimensions), endpoint, resource, principal,
                tenantRelationship, expected, observed, List.copyOf(evidence), conflicts,
                List.copyOf(policyReferences), confidence, rationale, fingerprint);
    }

    private String tenantRelationship(AuthorizationContextAssessment assessment) {
        if (assessment == null || assessment.normalizedContext() == null
                || assessment.normalizedContext().tenant() == null
                || assessment.normalizedContext().resource() == null) return "UNKNOWN";
        String subject = assessment.normalizedContext().tenant().tenantId();
        String resource = assessment.normalizedContext().resource().tenantId();
        if (subject == null || resource == null || subject.isBlank() || resource.isBlank()) return "UNKNOWN";
        return subject.equals(resource) ? "SAME_TENANT" : "CROSS_TENANT";
    }
}
