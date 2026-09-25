package io.acra.core.engine;

import io.acra.core.domain.finding.AuthorizationRiskAssessment;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingLifecycleEvent;
import io.acra.core.domain.finding.FindingLifecyclePolicy;
import io.acra.core.domain.finding.FindingLifecycleState;
import io.acra.core.domain.finding.FindingLifecycleTransitionRequest;
import io.acra.core.domain.finding.GovernedFinding;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public final class FindingLifecycleService {

    public GovernedFinding create(
            FindingCandidate candidate,
            AuthorizationRiskAssessment riskAssessment) {
        if (candidate == null || riskAssessment == null) {
            throw new IllegalArgumentException("candidate/riskAssessment required");
        }
        if (candidate.state() != FindingCandidateState.CANDIDATE) {
            throw new IllegalArgumentException("only review candidates can enter finding lifecycle");
        }
        if (candidate.supportingEvidenceIds().isEmpty()) {
            throw new IllegalArgumentException("review candidate requires supporting evidence");
        }
        if (!candidate.candidateId().equals(riskAssessment.candidateId())) {
            throw new IllegalArgumentException("risk assessment candidate mismatch");
        }
        if (candidate.fingerprint() == null
                || candidate.fingerprint().fingerprint() == null
                || candidate.fingerprint().fingerprint().isBlank()) {
            throw new IllegalArgumentException("candidate fingerprint required");
        }

        return new GovernedFinding(
                "",
                candidate.candidateId(),
                candidate.fingerprint().fingerprint(),
                riskAssessment.riskId(),
                riskAssessment.severity(),
                riskAssessment.confidence(),
                FindingLifecycleState.REVIEW_REQUIRED,
                candidate.supportingEvidenceIds(),
                List.of(),
                "");
    }

    public GovernedFinding transition(
            GovernedFinding finding,
            FindingLifecycleTransitionRequest request) {
        if (finding == null || request == null) {
            throw new IllegalArgumentException("finding/transition request required");
        }

        FindingLifecycleState target = FindingLifecyclePolicy.target(
                finding.state(), request.action());

        int sequence = finding.events().size() + 1;
        FindingLifecycleEvent event = new FindingLifecycleEvent(
                "",
                sequence,
                finding.state(),
                target,
                request.action(),
                request.reviewerReference(),
                request.decisionReference(),
                request.evidenceIds(),
                "");

        List<FindingLifecycleEvent> events = new ArrayList<>(finding.events());
        events.add(event);

        TreeSet<String> evidence = new TreeSet<>(finding.evidenceIds());
        evidence.addAll(request.evidenceIds());

        return new GovernedFinding(
                finding.findingId(),
                finding.sourceCandidateId(),
                finding.sourceCandidateFingerprint(),
                finding.riskAssessmentId(),
                finding.severity(),
                finding.confidence(),
                target,
                List.copyOf(evidence),
                events,
                "");
    }
}
