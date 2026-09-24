package io.acra.core.product.batchindirect;

import io.acra.core.batch.BatchItemAuthorizationAssessment;
import io.acra.core.batch.BatchItemObservation;
import io.acra.core.batch.BatchItemPolicy;
import io.acra.core.coverage.S10AuthorizationCoverageEntry;
import io.acra.core.coverage.S10AuthorizationCoverageSummary;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.reference.IndirectReferenceAuthorizationAssessment;
import io.acra.core.reference.IndirectReferencePolicy;
import io.acra.core.reference.IndirectReferenceResolution;
import java.util.List;

public record S10BatchIndirectProductSnapshot(
        List<BatchItemPolicy> batchPolicies,
        List<IndirectReferencePolicy> indirectPolicies,
        List<BatchItemObservation> batchObservations,
        List<IndirectReferenceResolution> indirectResolutions,
        List<BatchItemAuthorizationAssessment> batchAssessments,
        List<IndirectReferenceAuthorizationAssessment> indirectAssessments,
        List<FindingCandidate> candidates,
        List<S10AuthorizationCoverageEntry> coverageEntries,
        S10AuthorizationCoverageSummary coverageSummary) {

    public S10BatchIndirectProductSnapshot {
        batchPolicies = List.copyOf(batchPolicies == null ? List.of() : batchPolicies);
        indirectPolicies = List.copyOf(indirectPolicies == null ? List.of() : indirectPolicies);
        batchObservations = List.copyOf(batchObservations == null ? List.of() : batchObservations);
        indirectResolutions = List.copyOf(indirectResolutions == null ? List.of() : indirectResolutions);
        batchAssessments = List.copyOf(batchAssessments == null ? List.of() : batchAssessments);
        indirectAssessments = List.copyOf(indirectAssessments == null ? List.of() : indirectAssessments);
        candidates = List.copyOf(candidates == null ? List.of() : candidates);
        coverageEntries = List.copyOf(coverageEntries == null ? List.of() : coverageEntries);
        coverageSummary = coverageSummary == null
                ? S10AuthorizationCoverageSummary.fromEntries(List.of())
                : coverageSummary;
    }

    public int policyCount() {
        return batchPolicies.size() + indirectPolicies.size();
    }

    public int observationCount() {
        return batchObservations.size() + indirectResolutions.size();
    }

    public int assessmentCount() {
        return batchAssessments.size() + indirectAssessments.size();
    }

    public long candidateCount() {
        return candidates.stream().filter(value -> value.state() == FindingCandidateState.CANDIDATE).count();
    }

    public long rejectedCount() {
        return candidates.stream().filter(value -> value.state() == FindingCandidateState.REJECTED).count();
    }

    public long inconclusiveCount() {
        return candidates.stream().filter(value -> value.state() == FindingCandidateState.INCONCLUSIVE).count();
    }
}
