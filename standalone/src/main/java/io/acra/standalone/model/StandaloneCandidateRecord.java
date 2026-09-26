package io.acra.standalone.model;

import io.acra.core.domain.finding.FindingCandidate;

import java.util.Objects;

public record StandaloneCandidateRecord(
        FindingCandidate candidate,
        CandidateReviewRecord review
) {
    public StandaloneCandidateRecord {
        Objects.requireNonNull(candidate, "candidate");
        Objects.requireNonNull(review, "review");
    }
}
