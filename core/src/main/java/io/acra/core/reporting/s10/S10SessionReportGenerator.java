package io.acra.core.reporting.s10;

import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.product.session.S10SessionProductSnapshot;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.session.AuthenticationSessionObservation;
import io.acra.core.session.SessionCorrelationResult;
import io.acra.core.session.SessionCoverageEntry;
import io.acra.core.session.SessionSecurityAssessment;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public final class S10SessionReportGenerator {
    public static final String REPORT_VERSION = "s10-auth-session-report-v1";

    public S10SessionReport generate(S10SessionProductSnapshot snapshot, Instant generatedAt) {
        if (snapshot == null || generatedAt == null) {
            throw new IllegalArgumentException("snapshot/generatedAt required");
        }

        List<AuthenticationSessionObservation> observations = snapshot.observations().stream()
                .sorted(Comparator.comparing(AuthenticationSessionObservation::observationId))
                .toList();
        List<SessionCorrelationResult> correlations = snapshot.correlations().stream()
                .sorted(Comparator.comparing(SessionCorrelationResult::correlationId))
                .toList();
        List<SessionSecurityAssessment> assessments = snapshot.assessments().stream()
                .sorted(Comparator.comparing(SessionSecurityAssessment::assessmentId))
                .toList();
        List<FindingCandidate> candidates = snapshot.candidates().stream()
                .sorted(Comparator.comparing(FindingCandidate::candidateId))
                .toList();
        List<SessionCoverageEntry> coverage = snapshot.coverageEntries().stream()
                .sorted(Comparator.comparing(value -> value.target().coverageId()))
                .toList();

        Map<String, String> observationContexts = new HashMap<>();
        for (AuthenticationSessionObservation observation : observations) {
            observationContexts.put(observation.observationId(), authContextRef(observation.sessionId()));
        }
        Map<String, String> assessmentContexts = new HashMap<>();
        for (SessionSecurityAssessment assessment : assessments) {
            assessmentContexts.put(assessment.assessmentId(), authContextRef(assessment.sessionId()));
        }

        List<S10SessionReport.ObservationView> observationViews = observations.stream()
                .map(value -> new S10SessionReport.ObservationView(
                        value.observationId(),
                        authContextRef(value.sessionId()),
                        value.principalId(),
                        value.roleId(),
                        value.tenantId(),
                        value.scopes(),
                        value.authenticationType().name(),
                        value.identityState().name(),
                        value.observedAt(),
                        value.evidenceIds()))
                .toList();

        List<S10SessionReport.CorrelationView> correlationViews = correlations.stream()
                .map(value -> new S10SessionReport.CorrelationView(
                        value.correlationId(),
                        authContextRef(value.sessionId()),
                        value.previousObservationId(),
                        value.currentObservationId(),
                        value.state().name(),
                        value.tokenRotated(),
                        value.previousIdentityVerified(),
                        value.currentIdentityVerified(),
                        value.driftDimensions().stream().map(Enum::name).toList(),
                        value.evidenceIds(),
                        value.reasons()))
                .toList();

        List<S10SessionReport.AssessmentView> assessmentViews = assessments.stream()
                .map(value -> new S10SessionReport.AssessmentView(
                        value.assessmentId(),
                        authContextRef(value.sessionId()),
                        value.state().name(),
                        value.tokenRotated(),
                        value.identityVerified(),
                        value.driftDimensions().stream().map(Enum::name).toList(),
                        value.evidenceIds(),
                        value.rationale(),
                        value.reasons()))
                .toList();

        List<S10SessionReport.CandidateView> candidateViews = candidates.stream()
                .map(value -> new S10SessionReport.CandidateView(
                        value.candidateId(),
                        candidateContext(value, assessmentContexts),
                        value.state().name(),
                        value.principalId(),
                        value.tenantRelationship(),
                        value.policyReferences(),
                        value.dimensions(),
                        value.confidence(),
                        value.supportingEvidenceIds(),
                        value.rationale()))
                .toList();

        List<S10SessionReport.CoverageView> coverageViews = coverage.stream()
                .map(value -> new S10SessionReport.CoverageView(
                        value.target().coverageId(),
                        value.target().targetId(),
                        authContextRef(value.target().sessionId()),
                        value.target().objective().name(),
                        value.target().ruleReference(),
                        value.disposition().name(),
                        value.observationIds().size(),
                        value.correlationIds().size(),
                        value.assessmentIds().size(),
                        value.findingCandidateIds().size()))
                .toList();

        var coverageSummary = snapshot.coverageSummary();
        int candidateCount = (int) candidates.stream()
                .filter(value -> value.state() == FindingCandidateState.CANDIDATE)
                .count();
        int rejectedCount = (int) candidates.stream()
                .filter(value -> value.state() == FindingCandidateState.REJECTED)
                .count();
        int inconclusiveCount = (int) candidates.stream()
                .filter(value -> value.state() == FindingCandidateState.INCONCLUSIVE)
                .count();

        S10SessionReportSummary summary = new S10SessionReportSummary(
                coverageSummary.totalTargets(),
                coverageSummary.baselineTargets(),
                coverageSummary.continuityTargets(),
                coverageSummary.rotationTargets(),
                coverageSummary.observedTargets(),
                coverageSummary.correlatedTargets(),
                coverageSummary.assessedTargets(),
                coverageSummary.unobservedTargets(),
                coverageSummary.observedUncorrelatedTargets(),
                coverageSummary.correlatedUnassessedTargets(),
                observations.size(),
                correlations.size(),
                assessments.size(),
                candidateCount,
                rejectedCount,
                inconclusiveCount,
                0);

        TreeSet<String> evidence = new TreeSet<>();
        observations.forEach(value -> evidence.addAll(value.evidenceIds()));
        correlations.forEach(value -> evidence.addAll(value.evidenceIds()));
        assessments.forEach(value -> evidence.addAll(value.evidenceIds()));
        candidates.forEach(value -> evidence.addAll(value.supportingEvidenceIds()));

        String material = REPORT_VERSION
                + "|" + observations.stream().map(AuthenticationSessionObservation::observationId).toList()
                + "|" + correlations.stream().map(SessionCorrelationResult::correlationId).toList()
                + "|" + assessments.stream().map(SessionSecurityAssessment::assessmentId).toList()
                + "|" + candidates.stream().map(FindingCandidate::candidateId).toList()
                + "|" + coverage.stream().map(value -> value.target().coverageId()).toList();

        List<String> limitations = List.of(
                "Token fingerprints are correlation handles and do not independently verify a principal.",
                "Raw authentication material, token fingerprints and raw session identifiers are excluded from the Sprint 10 report projection.",
                "FindingCandidate is a review candidate, not a confirmed vulnerability.",
                "Unobserved session coverage must not be interpreted as secure.",
                "Controlled synthetic localhost authentication/session evidence does not establish production authentication behavior.",
                "Real Burp desktop runtime validation remains a separate verification lane.");

        boolean empty = observations.isEmpty() && correlations.isEmpty() && assessments.isEmpty()
                && candidates.isEmpty() && coverage.isEmpty();

        return new S10SessionReport(
                "s10-report-" + TokenFingerprint.sha256(material).substring(0, 24),
                REPORT_VERSION,
                empty ? S10SessionReportStatus.NO_SESSION_EVIDENCE : S10SessionReportStatus.READY_FOR_REVIEW,
                generatedAt,
                summary,
                observationViews,
                correlationViews,
                assessmentViews,
                candidateViews,
                coverageViews,
                List.copyOf(evidence),
                limitations);
    }

    public static String authContextRef(String sessionId) {
        String value = sessionId == null ? "UNKNOWN" : sessionId;
        return "authctx-" + TokenFingerprint.sha256(value).substring(0, 16);
    }

    private static String candidateContext(
            FindingCandidate candidate,
            Map<String, String> assessmentContexts) {
        for (String assessmentId : candidate.assessmentIds()) {
            String value = assessmentContexts.get(assessmentId);
            if (value != null) return value;
        }
        return authContextRef("UNKNOWN");
    }
}
