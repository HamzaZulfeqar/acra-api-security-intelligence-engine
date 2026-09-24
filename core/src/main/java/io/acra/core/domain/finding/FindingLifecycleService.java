package io.acra.core.domain.finding;

import io.acra.core.security.TokenFingerprint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public final class FindingLifecycleService {

    public ReviewedFinding open(
            FindingCandidate candidate,
            AuthorizationRiskAssessment risk,
            Instant at) {
        if (candidate == null) throw new IllegalArgumentException("candidate required");
        if (risk == null) throw new IllegalArgumentException("risk required");
        if (at == null) throw new IllegalArgumentException("at required");
        if (candidate.state() != FindingCandidateState.CANDIDATE) {
            throw new IllegalArgumentException("only review candidates can enter finding lifecycle");
        }
        if (candidate.projectId().isBlank()) throw new IllegalArgumentException("candidate projectId required");
        if (candidate.fingerprint() == null) throw new IllegalArgumentException("candidate fingerprint required");
        if (!candidate.candidateId().equals(risk.candidateId())) {
            throw new IllegalArgumentException("risk candidateId mismatch");
        }
        if (candidate.supportingEvidenceIds().isEmpty()) {
            throw new IllegalArgumentException("candidate supporting evidence required");
        }

        String material = String.join("|",
                candidate.projectId(),
                candidate.candidateId(),
                candidate.fingerprint().fingerprint());
        String findingId = "finding-" + TokenFingerprint.sha256(material).substring(0, 24);

        return new ReviewedFinding(
                findingId,
                candidate.candidateId(),
                candidate.projectId(),
                candidate.fingerprint(),
                risk.severity(),
                risk.confidence(),
                FindingLifecycleState.NEEDS_REVIEW,
                candidate.supportingEvidenceIds(),
                List.of(),
                at,
                at);
    }

    public ReviewedFinding transition(
            ReviewedFinding current,
            FindingLifecycleState target,
            Instant at,
            String reviewerReference,
            String reason,
            List<String> evidenceIds) {
        if (current == null) throw new IllegalArgumentException("current finding required");
        if (target == null) throw new IllegalArgumentException("target state required");
        if (at == null) throw new IllegalArgumentException("at required");
        if (at.isBefore(current.updatedAt())) {
            throw new IllegalArgumentException("transition timestamp precedes current finding state");
        }
        if (current.terminal()) {
            throw new IllegalArgumentException("terminal finding state cannot transition");
        }
        if (!allowed(current.state(), target)) {
            throw new IllegalArgumentException(
                    "invalid finding transition " + current.state() + " -> " + target);
        }

        FindingReviewTransition transition = new FindingReviewTransition(
                "",
                current.state(),
                target,
                at,
                reviewerReference,
                reason,
                evidenceIds);

        TreeSet<String> evidence = new TreeSet<>(current.supportingEvidenceIds());
        evidence.addAll(transition.evidenceIds());
        List<FindingReviewTransition> history = new ArrayList<>(current.history());
        history.add(transition);

        return new ReviewedFinding(
                current.findingId(),
                current.candidateId(),
                current.projectId(),
                current.fingerprint(),
                current.severity(),
                current.confidence(),
                target,
                List.copyOf(evidence),
                List.copyOf(history),
                current.openedAt(),
                at);
    }

    private static boolean allowed(
            FindingLifecycleState current,
            FindingLifecycleState target) {
        return switch (current) {
            case NEEDS_REVIEW ->
                    target == FindingLifecycleState.VALIDATED
                            || target == FindingLifecycleState.FALSE_POSITIVE;
            case VALIDATED ->
                    target == FindingLifecycleState.CONFIRMED
                            || target == FindingLifecycleState.FALSE_POSITIVE;
            case CONFIRMED ->
                    target == FindingLifecycleState.ACCEPTED_RISK
                            || target == FindingLifecycleState.FALSE_POSITIVE;
            case FALSE_POSITIVE, ACCEPTED_RISK -> false;
        };
    }
}
