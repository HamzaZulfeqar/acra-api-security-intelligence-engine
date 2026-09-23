package io.acra.core.engine;

import io.acra.core.domain.authorization.EffectiveAuthorizationResolution;
import io.acra.core.domain.authorization.PolicyConflictAssessment;
import io.acra.core.domain.authorization.PolicyResolutionState;
import io.acra.core.domain.authorization.RbacAssessment;
import io.acra.core.domain.authorization.RbacAssessmentState;
import io.acra.core.domain.authorization.RoleEscalationAssessment;
import io.acra.core.domain.authorization.RoleEscalationAssessmentState;
import io.acra.core.domain.authorization.TenantIsolationAssessment;
import io.acra.core.domain.authorization.TenantIsolationAssessmentState;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.security.TokenFingerprint;
import java.util.Set;
import java.util.TreeSet;

public final class S6FindingCandidateEvaluator {

    public FindingCandidate evaluate(FindingCandidate base,
                                     EffectiveAuthorizationResolution resolution,
                                     TenantIsolationAssessment tenant,
                                     RbacAssessment rbac,
                                     RoleEscalationAssessment escalation,
                                     PolicyConflictAssessment conflict) {
        if (base == null) throw new IllegalArgumentException("base candidate required");
        if (resolution == null) throw new IllegalArgumentException("resolution required");

        Set<String> dimensions = new TreeSet<>(base.dimensions());
        Set<String> assessmentIds = new TreeSet<>(base.assessmentIds());
        Set<String> evidence = new TreeSet<>(base.supportingEvidenceIds());
        Set<String> contradictions = new TreeSet<>(base.contradictoryEvidence());
        Set<String> policies = new TreeSet<>(base.policyReferences());
        policies.add(resolution.policyFingerprint());

        boolean s6Candidate = false;
        boolean s6Inconclusive = resolution.state() == PolicyResolutionState.INCOMPLETE
                || resolution.state() == PolicyResolutionState.UNKNOWN
                || resolution.state() == PolicyResolutionState.CONFLICTING;

        if (tenant != null) {
            assessmentIds.add(tenant.assessmentId());
            evidence.addAll(tenant.evidenceIds());
            if (tenant.state() == TenantIsolationAssessmentState.CANDIDATE) {
                dimensions.add("TENANT_ISOLATION");
                s6Candidate = true;
            }
            if (tenant.state() == TenantIsolationAssessmentState.CONFLICTING
                    || tenant.state() == TenantIsolationAssessmentState.INCONCLUSIVE) s6Inconclusive = true;
        }
        if (rbac != null) {
            assessmentIds.add(rbac.assessmentId());
            evidence.addAll(rbac.evidenceIds());
            if (rbac.state() == RbacAssessmentState.CANDIDATE) {
                dimensions.add("RBAC");
                s6Candidate = true;
            }
            if (rbac.state() == RbacAssessmentState.CONFLICTING
                    || rbac.state() == RbacAssessmentState.INCONCLUSIVE) s6Inconclusive = true;
        }
        if (escalation != null) {
            assessmentIds.add(escalation.assessmentId());
            evidence.addAll(escalation.evidenceIds());
            if (escalation.state() == RoleEscalationAssessmentState.CANDIDATE) {
                dimensions.add("ROLE_ESCALATION");
                s6Candidate = true;
            }
            if (escalation.state() == RoleEscalationAssessmentState.CONFLICTING
                    || escalation.state() == RoleEscalationAssessmentState.INCONCLUSIVE) s6Inconclusive = true;
        }
        if (conflict != null) {
            assessmentIds.add(conflict.assessmentId());
            evidence.addAll(conflict.evidenceIds());
            if (conflict.conflicting()) {
                dimensions.add("POLICY_CONFLICT");
                contradictions.addAll(conflict.reasons());
            }
        }
        if (!resolution.delegationIds().isEmpty()) dimensions.add("DELEGATION");

        FindingCandidateState state;
        if (base.state() == FindingCandidateState.CANDIDATE || s6Candidate) {
            state = FindingCandidateState.CANDIDATE;
        } else if (base.state() == FindingCandidateState.INCONCLUSIVE || s6Inconclusive) {
            state = FindingCandidateState.INCONCLUSIVE;
        } else {
            state = FindingCandidateState.REJECTED;
        }

        String confidence = state == FindingCandidateState.CANDIDATE
                && resolution.state() != PolicyResolutionState.CONFLICTING
                && !evidence.isEmpty() ? "HIGH"
                : state == FindingCandidateState.CANDIDATE ? "MEDIUM" : base.confidence();

        String material = base.candidateId() + "|" + resolution.resolutionId() + "|" + dimensions + "|" + state;
        String candidateId = "fc-s6-" + TokenFingerprint.sha256(material).substring(0, 24);
        FindingFingerprint fingerprint = FindingFingerprint.of(base.endpoint(), base.resourceId(),
                base.principalId(), resolution.tenantRelationship().name(), String.join("+", dimensions),
                state.name());

        return new FindingCandidate(candidateId, state, base.projectId(), base.testIds(), base.executionIds(),
                base.observationIds(), assessmentIds.stream().toList(), dimensions.stream().toList(),
                base.endpoint(), base.resourceId(), base.principalId(), resolution.tenantRelationship().name(),
                resolution.expectedDecision(), resolution.observedDecision(), evidence.stream().toList(),
                contradictions.stream().toList(), policies.stream().toList(), confidence,
                "S6 policy-aware candidate composed from S5 evidence and effective tenant/RBAC policy; "
                        + "candidate is not an automatically confirmed real-world vulnerability",
                fingerprint);
    }
}
