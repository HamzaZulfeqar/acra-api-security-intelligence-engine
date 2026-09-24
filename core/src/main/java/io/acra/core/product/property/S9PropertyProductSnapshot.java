package io.acra.core.product.property;

import io.acra.core.domain.authorization.PropertyAuthorizationAssessment;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.engine.PolicyValidationEvaluator;
import io.acra.core.property.PropertyAccessObservation;
import io.acra.core.property.PropertyAuthorizationCoverageEntry;
import io.acra.core.property.PropertyAuthorizationCoverageSummary;
import java.util.List;

public record S9PropertyProductSnapshot(
        List<PolicyValidationEvaluator.PropertyPolicy> policies,
        List<PropertyAccessObservation> observations,
        List<PropertyAuthorizationAssessment> assessments,
        List<FindingCandidate> candidates,
        List<PropertyAuthorizationCoverageEntry> coverageEntries,
        PropertyAuthorizationCoverageSummary coverageSummary) {

    public S9PropertyProductSnapshot {
        policies = List.copyOf(policies == null ? List.of() : policies);
        observations = List.copyOf(observations == null ? List.of() : observations);
        assessments = List.copyOf(assessments == null ? List.of() : assessments);
        candidates = List.copyOf(candidates == null ? List.of() : candidates);
        coverageEntries = List.copyOf(coverageEntries == null ? List.of() : coverageEntries);
        coverageSummary = coverageSummary == null
                ? PropertyAuthorizationCoverageSummary.fromEntries(List.of())
                : coverageSummary;
    }

    public long candidateCount() {
        return candidates.stream()
                .filter(value -> value.state() == io.acra.core.domain.finding.FindingCandidateState.CANDIDATE)
                .count();
    }

    public long rejectedCount() {
        return candidates.stream()
                .filter(value -> value.state() == io.acra.core.domain.finding.FindingCandidateState.REJECTED)
                .count();
    }

    public long inconclusiveCount() {
        return candidates.stream()
                .filter(value -> value.state() == io.acra.core.domain.finding.FindingCandidateState.INCONCLUSIVE)
                .count();
    }
}
