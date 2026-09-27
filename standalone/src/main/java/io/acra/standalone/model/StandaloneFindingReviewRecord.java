package io.acra.standalone.model;

import io.acra.core.domain.finding.AuthorizationRiskAssessment;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.ReviewedFinding;

import java.util.Objects;
import java.util.UUID;

public record StandaloneFindingReviewRecord(
        UUID sourceRunId,
        FindingCandidate candidate,
        AuthorizationRiskAssessment risk,
        ReviewedFinding finding
) {
    public StandaloneFindingReviewRecord {
        Objects.requireNonNull(sourceRunId, "sourceRunId");
        Objects.requireNonNull(candidate, "candidate");
        Objects.requireNonNull(risk, "risk");
        Objects.requireNonNull(finding, "finding");
        if (!candidate.candidateId().equals(risk.candidateId())) {
            throw new IllegalArgumentException("candidate/risk identity mismatch");
        }
        if (!candidate.candidateId().equals(finding.candidateId())) {
            throw new IllegalArgumentException("candidate/finding identity mismatch");
        }
        if (!candidate.projectId().equals(finding.projectId())) {
            throw new IllegalArgumentException("candidate/finding project mismatch");
        }
    }
}
