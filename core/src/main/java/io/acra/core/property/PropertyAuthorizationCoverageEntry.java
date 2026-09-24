package io.acra.core.property;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.PropertyAuthorizationAssessment;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.engine.PolicyValidationEvaluator;
import io.acra.core.security.TokenFingerprint;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

public record PropertyAuthorizationCoverageEntry(
        String coverageId,
        String policyReference,
        String policySource,
        String endpoint,
        String property,
        PolicyValidationEvaluator.PropertyOperation operation,
        String roleId,
        String tenantId,
        AuthorizationDecision expectedDecision,
        List<String> policyEvidenceIds,
        List<String> observationIds,
        List<String> assessmentIds,
        List<String> findingCandidateIds,
        List<FindingCandidateState> findingStates) {

    public PropertyAuthorizationCoverageEntry {
        coverageId = required(coverageId, "coverageId");
        policyReference = required(policyReference, "policyReference");
        policySource = required(policySource, "policySource");
        endpoint = required(endpoint, "endpoint");
        property = required(property, "property");
        if (operation == null) throw new IllegalArgumentException("operation required");
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
            throw new IllegalArgumentException("every assessed coverage context requires a finding projection");
        }
    }

    public static PropertyAuthorizationCoverageEntry from(PolicyValidationEvaluator.PropertyPolicy policy) {
        if (policy == null) throw new IllegalArgumentException("property policy required");
        String material = String.join("|",
                policy.policyReference(),
                policy.endpoint(),
                policy.property(),
                policy.operation().name(),
                policy.roleId(),
                policy.tenantId());
        return new PropertyAuthorizationCoverageEntry(
                "s9-property-coverage-" + TokenFingerprint.sha256(material).substring(0, 24),
                policy.policyReference(),
                policy.policySource(),
                policy.endpoint(),
                policy.property(),
                policy.operation(),
                policy.roleId(),
                policy.tenantId(),
                policy.expectedDecision(),
                policy.evidenceIds(),
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

    public PropertyCoverageDisposition disposition() {
        if (!observed()) return PropertyCoverageDisposition.UNOBSERVED;
        if (!assessed()) return PropertyCoverageDisposition.OBSERVED_UNASSESSED;
        if (findingStates.contains(FindingCandidateState.CANDIDATE)) return PropertyCoverageDisposition.CANDIDATE;
        if (findingStates.contains(FindingCandidateState.INCONCLUSIVE)) return PropertyCoverageDisposition.INCONCLUSIVE;
        return PropertyCoverageDisposition.REJECTED;
    }

    public PropertyAuthorizationCoverageEntry carryLifecycleFrom(PropertyAuthorizationCoverageEntry existing) {
        if (existing == null) return this;
        if (!coverageId.equals(existing.coverageId())) {
            throw new IllegalArgumentException("coverage identity mismatch");
        }
        if (expectedDecision != existing.expectedDecision()
                || !policyReference.equals(existing.policyReference())
                || !policySource.equals(existing.policySource())) {
            throw new IllegalArgumentException("property policy drift for existing coverage identity");
        }
        return new PropertyAuthorizationCoverageEntry(
                coverageId, policyReference, policySource, endpoint, property, operation, roleId, tenantId,
                expectedDecision, union(policyEvidenceIds, existing.policyEvidenceIds()),
                existing.observationIds(), existing.assessmentIds(), existing.findingCandidateIds(),
                existing.findingStates());
    }

    public PropertyAuthorizationCoverageEntry withObservation(PropertyAccessObservation observation) {
        requireObservationMatch(observation);
        return new PropertyAuthorizationCoverageEntry(
                coverageId, policyReference, policySource, endpoint, property, operation, roleId, tenantId,
                expectedDecision, policyEvidenceIds, union(observationIds, List.of(observation.observationId())),
                assessmentIds, findingCandidateIds, findingStates);
    }

    public PropertyAuthorizationCoverageEntry withAssessment(
            PropertyAccessObservation observation,
            PropertyAuthorizationAssessment assessment,
            FindingCandidate finding) {
        requireObservationMatch(observation);
        if (!observationIds.contains(observation.observationId())) {
            throw new IllegalArgumentException("property observation must be recorded before assessment");
        }
        if (assessment == null || finding == null) {
            throw new IllegalArgumentException("assessment and finding required");
        }
        if (!endpoint.equals(assessment.endpoint())
                || !property.equals(assessment.property())
                || !operation.name().equals(assessment.operation())
                || !policyReference.equals(assessment.policyReference())
                || expectedDecision != assessment.expectedDecision()
                || observation.observedDecision() != assessment.observedDecision()) {
            throw new IllegalArgumentException("property assessment does not match coverage context");
        }
        if (!finding.assessmentIds().contains(assessment.assessmentId())
                || !finding.policyReferences().contains(policyReference)
                || !finding.dimensions().contains("PROPERTY")
                || !finding.endpoint().equals(endpoint)
                || finding.expectedDecision() != assessment.expectedDecision()
                || finding.observedDecision() != assessment.observedDecision()) {
            throw new IllegalArgumentException("finding projection does not match property assessment");
        }
        return new PropertyAuthorizationCoverageEntry(
                coverageId, policyReference, policySource, endpoint, property, operation, roleId, tenantId,
                expectedDecision, policyEvidenceIds, observationIds,
                union(assessmentIds, List.of(assessment.assessmentId())),
                union(findingCandidateIds, List.of(finding.candidateId())),
                unionStates(findingStates, finding.state()));
    }

    private void requireObservationMatch(PropertyAccessObservation observation) {
        if (observation == null) throw new IllegalArgumentException("property observation required");
        if (!endpoint.equals(observation.endpoint())
                || !property.equals(observation.property())
                || operation != observation.operation()) {
            throw new IllegalArgumentException("property observation does not match coverage context");
        }
    }

    private static List<FindingCandidateState> unionStates(
            List<FindingCandidateState> current,
            FindingCandidateState state) {
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
