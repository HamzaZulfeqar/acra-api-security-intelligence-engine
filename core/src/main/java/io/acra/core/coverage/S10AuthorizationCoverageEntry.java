package io.acra.core.coverage;

import io.acra.core.batch.BatchItemAuthorizationAssessment;
import io.acra.core.batch.BatchItemObservation;
import io.acra.core.batch.BatchItemPolicy;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.reference.IndirectReferenceAuthorizationAssessment;
import io.acra.core.reference.IndirectReferencePolicy;
import io.acra.core.reference.IndirectReferenceResolution;
import io.acra.core.security.TokenFingerprint;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

public record S10AuthorizationCoverageEntry(
        String coverageId,
        S10CoverageFamily family,
        String policyReference,
        String policySource,
        String endpoint,
        String resourceId,
        String action,
        String roleId,
        String tenantId,
        AuthorizationDecision expectedDecision,
        List<String> policyEvidenceIds,
        List<String> observationIds,
        List<String> assessmentIds,
        List<String> findingCandidateIds,
        List<FindingCandidateState> findingStates) {

    public S10AuthorizationCoverageEntry {
        coverageId = required(coverageId, "coverageId");
        if (family == null) throw new IllegalArgumentException("family required");
        policyReference = required(policyReference, "policyReference");
        policySource = required(policySource, "policySource");
        endpoint = required(endpoint, "endpoint");
        resourceId = required(resourceId, "resourceId");
        action = required(action, "action");
        roleId = normalized(roleId);
        tenantId = normalized(tenantId);
        expectedDecision = expectedDecision == null ? AuthorizationDecision.UNKNOWN : expectedDecision;
        policyEvidenceIds = sorted(policyEvidenceIds);
        observationIds = sorted(observationIds);
        assessmentIds = sorted(assessmentIds);
        findingCandidateIds = sorted(findingCandidateIds);
        findingStates = List.copyOf(findingStates == null ? List.of() : findingStates).stream()
                .distinct()
                .sorted(Comparator.comparing(Enum::name))
                .toList();
        if (assessmentIds.size() != findingCandidateIds.size() && !assessmentIds.isEmpty()) {
            throw new IllegalArgumentException("every assessed S10 coverage context requires a finding projection");
        }
    }

    public static S10AuthorizationCoverageEntry from(BatchItemPolicy policy) {
        if (policy == null) throw new IllegalArgumentException("batch policy required");
        return fresh(
                S10CoverageFamily.BATCH_ITEM,
                policy.policyReference(),
                policy.policySource(),
                policy.endpoint(),
                policy.resourceId(),
                policy.action(),
                policy.roleId(),
                policy.tenantId(),
                policy.expectedDecision(),
                policy.evidenceIds());
    }

    public static S10AuthorizationCoverageEntry from(IndirectReferencePolicy policy) {
        if (policy == null) throw new IllegalArgumentException("indirect policy required");
        return fresh(
                S10CoverageFamily.INDIRECT_REFERENCE,
                policy.policyReference(),
                policy.policySource(),
                policy.endpoint(),
                policy.resolvedResourceId(),
                policy.action(),
                policy.roleId(),
                policy.tenantId(),
                policy.expectedDecision(),
                policy.evidenceIds());
    }

    private static S10AuthorizationCoverageEntry fresh(
            S10CoverageFamily family,
            String policyReference,
            String policySource,
            String endpoint,
            String resourceId,
            String action,
            String roleId,
            String tenantId,
            AuthorizationDecision expectedDecision,
            List<String> evidenceIds) {
        String material = String.join("|",
                family.name(), policyReference, endpoint, resourceId, action,
                normalized(roleId), normalized(tenantId));
        return new S10AuthorizationCoverageEntry(
                "s10-coverage-" + TokenFingerprint.sha256(material).substring(0, 24),
                family,
                policyReference,
                policySource,
                endpoint,
                resourceId,
                action,
                roleId,
                tenantId,
                expectedDecision,
                evidenceIds,
                List.of(),
                List.of(),
                List.of(),
                List.of());
    }

    public boolean observed() {
        return !observationIds.isEmpty();
    }

    public boolean assessed() {
        return !assessmentIds.isEmpty();
    }

    public S10CoverageDisposition disposition() {
        if (!observed()) return S10CoverageDisposition.UNOBSERVED;
        if (!assessed()) return S10CoverageDisposition.OBSERVED_UNASSESSED;
        if (findingStates.contains(FindingCandidateState.CANDIDATE)) return S10CoverageDisposition.CANDIDATE;
        if (findingStates.contains(FindingCandidateState.INCONCLUSIVE)) return S10CoverageDisposition.INCONCLUSIVE;
        return S10CoverageDisposition.REJECTED;
    }

    public S10AuthorizationCoverageEntry carryLifecycleFrom(S10AuthorizationCoverageEntry existing) {
        if (existing == null) return this;
        if (!coverageId.equals(existing.coverageId())) {
            throw new IllegalArgumentException("coverage identity mismatch");
        }
        if (family != existing.family()
                || expectedDecision != existing.expectedDecision()
                || !policyReference.equals(existing.policyReference())
                || !policySource.equals(existing.policySource())) {
            throw new IllegalArgumentException("S10 policy drift for existing coverage identity");
        }
        return new S10AuthorizationCoverageEntry(
                coverageId, family, policyReference, policySource, endpoint, resourceId, action, roleId, tenantId,
                expectedDecision, union(policyEvidenceIds, existing.policyEvidenceIds()),
                existing.observationIds(), existing.assessmentIds(), existing.findingCandidateIds(),
                existing.findingStates());
    }

    public S10AuthorizationCoverageEntry withObservation(BatchItemObservation observation) {
        if (family != S10CoverageFamily.BATCH_ITEM) {
            throw new IllegalArgumentException("batch observation cannot update indirect coverage");
        }
        requireBatchObservationMatch(observation);
        return copyWithObservation(observation.itemObservationId());
    }

    public S10AuthorizationCoverageEntry withObservation(IndirectReferenceResolution resolution) {
        if (family != S10CoverageFamily.INDIRECT_REFERENCE) {
            throw new IllegalArgumentException("indirect resolution cannot update batch coverage");
        }
        requireIndirectResolutionMatch(resolution);
        return copyWithObservation(resolution.resolutionId());
    }

    public S10AuthorizationCoverageEntry withAssessment(
            BatchItemObservation observation,
            BatchItemAuthorizationAssessment assessment,
            FindingCandidate finding) {
        if (family != S10CoverageFamily.BATCH_ITEM) {
            throw new IllegalArgumentException("batch assessment cannot update indirect coverage");
        }
        requireBatchObservationMatch(observation);
        if (!observationIds.contains(observation.itemObservationId())) {
            throw new IllegalArgumentException("batch observation must be recorded before assessment");
        }
        if (assessment == null || finding == null) {
            throw new IllegalArgumentException("assessment and finding required");
        }
        if (!endpoint.equals(assessment.endpoint())
                || !resourceId.equals(assessment.resourceId())
                || !action.equals(assessment.action())
                || !policyReference.equals(assessment.policyReference())
                || expectedDecision != assessment.expectedDecision()
                || observation.observedDecision() != assessment.observedDecision()) {
            throw new IllegalArgumentException("batch assessment does not match coverage context");
        }
        requireFindingMatch(finding, assessment.assessmentId(), assessment.expectedDecision(),
                assessment.observedDecision(), "BATCH_AUTHORIZATION");
        return copyWithAssessment(assessment.assessmentId(), finding);
    }

    public S10AuthorizationCoverageEntry withAssessment(
            IndirectReferenceResolution resolution,
            IndirectReferenceAuthorizationAssessment assessment,
            FindingCandidate finding) {
        if (family != S10CoverageFamily.INDIRECT_REFERENCE) {
            throw new IllegalArgumentException("indirect assessment cannot update batch coverage");
        }
        requireIndirectResolutionMatch(resolution);
        if (!observationIds.contains(resolution.resolutionId())) {
            throw new IllegalArgumentException("indirect resolution must be recorded before assessment");
        }
        if (assessment == null || finding == null) {
            throw new IllegalArgumentException("assessment and finding required");
        }
        if (!endpoint.equals(assessment.endpoint())
                || !resourceId.equals(assessment.resolvedResourceId())
                || !action.equals(assessment.action())
                || !policyReference.equals(assessment.policyReference())
                || expectedDecision != assessment.expectedDecision()
                || resolution.observedDecision() != assessment.observedDecision()) {
            throw new IllegalArgumentException("indirect assessment does not match coverage context");
        }
        requireFindingMatch(finding, assessment.assessmentId(), assessment.expectedDecision(),
                assessment.observedDecision(), "INDIRECT_REFERENCE_AUTHORIZATION");
        return copyWithAssessment(assessment.assessmentId(), finding);
    }

    private S10AuthorizationCoverageEntry copyWithObservation(String observationId) {
        return new S10AuthorizationCoverageEntry(
                coverageId, family, policyReference, policySource, endpoint, resourceId, action, roleId, tenantId,
                expectedDecision, policyEvidenceIds, union(observationIds, List.of(observationId)),
                assessmentIds, findingCandidateIds, findingStates);
    }

    private S10AuthorizationCoverageEntry copyWithAssessment(String assessmentId, FindingCandidate finding) {
        return new S10AuthorizationCoverageEntry(
                coverageId, family, policyReference, policySource, endpoint, resourceId, action, roleId, tenantId,
                expectedDecision, policyEvidenceIds, observationIds,
                union(assessmentIds, List.of(assessmentId)),
                union(findingCandidateIds, List.of(finding.candidateId())),
                unionStates(findingStates, finding.state()));
    }

    private void requireBatchObservationMatch(BatchItemObservation observation) {
        if (observation == null) throw new IllegalArgumentException("batch observation required");
        if (!endpoint.equals(observation.endpoint())
                || !resourceId.equals(observation.resourceId())
                || !action.equals(observation.action())) {
            throw new IllegalArgumentException("batch observation does not match coverage context");
        }
    }

    private void requireIndirectResolutionMatch(IndirectReferenceResolution resolution) {
        if (resolution == null) throw new IllegalArgumentException("indirect resolution required");
        if (!endpoint.equals(resolution.endpoint())
                || !resourceId.equals(resolution.resolvedResourceId())
                || !action.equals(resolution.action())) {
            throw new IllegalArgumentException("indirect resolution does not match coverage context");
        }
    }

    private void requireFindingMatch(
            FindingCandidate finding,
            String assessmentId,
            AuthorizationDecision expected,
            AuthorizationDecision observed,
            String dimension) {
        if (!finding.assessmentIds().contains(assessmentId)
                || !finding.policyReferences().contains(policyReference)
                || !finding.dimensions().contains(dimension)
                || !finding.endpoint().equals(endpoint)
                || !finding.resourceId().equals(resourceId)
                || finding.expectedDecision() != expected
                || finding.observedDecision() != observed) {
            throw new IllegalArgumentException("finding projection does not match S10 coverage context");
        }
    }

    private static List<FindingCandidateState> unionStates(
            List<FindingCandidateState> current, FindingCandidateState state) {
        List<FindingCandidateState> values = new ArrayList<>(current == null ? List.of() : current);
        if (state != null) values.add(state);
        return values.stream().distinct().sorted(Comparator.comparing(Enum::name)).toList();
    }

    private static List<String> union(List<String> left, List<String> right) {
        TreeSet<String> values = new TreeSet<>();
        if (left != null) values.addAll(left);
        if (right != null) values.addAll(right);
        values.removeIf(value -> value == null || value.isBlank());
        return List.copyOf(values);
    }

    private static List<String> sorted(List<String> values) {
        return union(List.of(), values);
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " required");
        return value.strip();
    }

    private static String normalized(String value) {
        return value == null || value.isBlank() ? "UNKNOWN" : value.strip();
    }
}
