package io.acra.core.product.finding;

import io.acra.core.domain.finding.AuthorizationRiskAssessment;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingLifecycleService;
import io.acra.core.domain.finding.FindingLifecycleState;
import io.acra.core.domain.finding.ReviewedFinding;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

public final class FindingReviewWorkspace {
    private final String projectId;
    private final FindingLifecycleService lifecycle = new FindingLifecycleService();
    private final TreeMap<String, FindingReviewCase> cases = new TreeMap<>();

    public FindingReviewWorkspace(String projectId) {
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalArgumentException("projectId required");
        }
        this.projectId = projectId;
    }

    public synchronized String projectId() {
        return projectId;
    }

    public synchronized ReviewedFinding open(
            FindingCandidate candidate,
            AuthorizationRiskAssessment risk,
            Instant at) {
        requireProject(candidate);
        ReviewedFinding finding = lifecycle.open(candidate, risk, at);
        FindingReviewCase next = new FindingReviewCase(candidate, risk, finding);
        FindingReviewCase existing = cases.get(finding.findingId());
        if (existing != null) {
            if (!existing.candidate().equals(candidate) || !existing.risk().equals(risk)) {
                throw new IllegalArgumentException("finding identity already exists with different source state");
            }
            return existing.finding();
        }
        cases.put(finding.findingId(), next);
        return finding;
    }

    public synchronized ReviewedFinding transition(
            String findingId,
            FindingLifecycleState target,
            Instant at,
            String reviewerReference,
            String reason,
            List<String> evidenceIds) {
        FindingReviewCase existing = requireCase(findingId);
        ReviewedFinding nextFinding = lifecycle.transition(
                existing.finding(), target, at, reviewerReference, reason, evidenceIds);
        cases.put(findingId, new FindingReviewCase(existing.candidate(), existing.risk(), nextFinding));
        return nextFinding;
    }

    public synchronized FindingReviewCase reviewCase(String findingId) {
        return requireCase(findingId);
    }

    public synchronized FindingReviewSnapshot snapshot() {
        List<FindingReviewCase> ordered = new ArrayList<>(cases.values());
        ordered.sort(Comparator.comparing(value -> value.finding().findingId()));
        return new FindingReviewSnapshot(projectId, List.copyOf(ordered));
    }

    private void requireProject(FindingCandidate candidate) {
        if (candidate == null) throw new IllegalArgumentException("candidate required");
        if (!projectId.equals(candidate.projectId())) {
            throw new IllegalArgumentException("candidate project mismatch");
        }
    }

    private FindingReviewCase requireCase(String findingId) {
        if (findingId == null || findingId.isBlank()) {
            throw new IllegalArgumentException("findingId required");
        }
        FindingReviewCase value = cases.get(findingId);
        if (value == null) throw new IllegalArgumentException("unknown findingId");
        return value;
    }
}
