package io.acra.core.product.finding;

import io.acra.core.domain.finding.AuthorizationRiskAssessment;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.ReviewedFinding;

public record FindingReviewCase(
        FindingCandidate candidate,
        AuthorizationRiskAssessment risk,
        ReviewedFinding finding) {

    public FindingReviewCase {
        if (candidate == null) throw new IllegalArgumentException("candidate required");
        if (risk == null) throw new IllegalArgumentException("risk required");
        if (finding == null) throw new IllegalArgumentException("finding required");
        if (!candidate.candidateId().equals(risk.candidateId())) {
            throw new IllegalArgumentException("candidate/risk identity mismatch");
        }
        if (!candidate.candidateId().equals(finding.candidateId())) {
            throw new IllegalArgumentException("candidate/finding identity mismatch");
        }
        if (!candidate.projectId().equals(finding.projectId())) {
            throw new IllegalArgumentException("candidate/finding project mismatch");
        }
        if (!candidate.fingerprint().equals(finding.fingerprint())) {
            throw new IllegalArgumentException("candidate/finding fingerprint mismatch");
        }
        if (risk.severity() != finding.severity()) {
            throw new IllegalArgumentException("risk/finding severity mismatch");
        }
        if (risk.confidence() != finding.confidence()) {
            throw new IllegalArgumentException("risk/finding confidence mismatch");
        }
    }
}
