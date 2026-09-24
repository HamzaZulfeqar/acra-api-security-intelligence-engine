package io.acra.core.product.session;

import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.session.AuthenticationSessionObservation;
import io.acra.core.session.SessionCorrelationResult;
import io.acra.core.session.SessionCoverageEntry;
import io.acra.core.session.SessionCoverageSummary;
import io.acra.core.session.SessionSecurityAssessment;
import java.util.List;

public record S10SessionProductSnapshot(
        List<AuthenticationSessionObservation> observations,
        List<SessionCorrelationResult> correlations,
        List<SessionSecurityAssessment> assessments,
        List<FindingCandidate> candidates,
        List<SessionCoverageEntry> coverageEntries,
        SessionCoverageSummary coverageSummary) {

    public S10SessionProductSnapshot {
        observations = List.copyOf(observations == null ? List.of() : observations);
        correlations = List.copyOf(correlations == null ? List.of() : correlations);
        assessments = List.copyOf(assessments == null ? List.of() : assessments);
        candidates = List.copyOf(candidates == null ? List.of() : candidates);
        coverageEntries = List.copyOf(coverageEntries == null ? List.of() : coverageEntries);
        coverageSummary = coverageSummary == null
                ? SessionCoverageSummary.fromEntries(List.of())
                : coverageSummary;
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
