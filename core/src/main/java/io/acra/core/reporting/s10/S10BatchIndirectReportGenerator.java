package io.acra.core.reporting.s10;

import io.acra.core.coverage.S10AuthorizationCoverageEntry;
import io.acra.core.coverage.S10CoverageFamily;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.product.batchindirect.S10BatchIndirectProductSnapshot;
import io.acra.core.security.TokenFingerprint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

public final class S10BatchIndirectReportGenerator {
    public static final String REPORT_VERSION = "s10-batch-indirect-report-v1";

    public S10BatchIndirectReport generate(
            S10BatchIndirectProductSnapshot snapshot,
            Instant generatedAt) {
        if (snapshot == null || generatedAt == null) {
            throw new IllegalArgumentException("snapshot/generatedAt required");
        }

        List<S10ReportPolicyContext> policies = new ArrayList<>();
        snapshot.batchPolicies().forEach(value -> policies.add(new S10ReportPolicyContext(
                S10CoverageFamily.BATCH_ITEM, value.policyReference(), value.endpoint(), value.resourceId(),
                value.action(), value.roleId(), value.tenantId(), value.expectedDecision(), value.evidenceIds())));
        snapshot.indirectPolicies().forEach(value -> policies.add(new S10ReportPolicyContext(
                S10CoverageFamily.INDIRECT_REFERENCE, value.policyReference(), value.endpoint(),
                value.resolvedResourceId(), value.action(), value.roleId(), value.tenantId(),
                value.expectedDecision(), value.evidenceIds())));
        policies = policies.stream().sorted(Comparator
                .comparing((S10ReportPolicyContext value) -> value.family().name())
                .thenComparing(S10ReportPolicyContext::policyReference)
                .thenComparing(S10ReportPolicyContext::endpoint)
                .thenComparing(S10ReportPolicyContext::resourceId)).toList();

        List<S10ReportObservation> observations = new ArrayList<>();
        snapshot.batchObservations().forEach(value -> observations.add(new S10ReportObservation(
                S10CoverageFamily.BATCH_ITEM, value.itemObservationId(), value.executionId(), value.testId(),
                value.endpoint(), value.resourceId(), value.action(), value.observedDecision(), "",
                value.evidenceIds())));
        snapshot.indirectResolutions().forEach(value -> observations.add(new S10ReportObservation(
                S10CoverageFamily.INDIRECT_REFERENCE, value.resolutionId(), value.executionId(), value.testId(),
                value.endpoint(), value.resolvedResourceId(), value.action(), value.observedDecision(),
                value.referenceFingerprint(), value.evidenceIds())));
        observations = observations.stream()
                .sorted(Comparator.comparing(S10ReportObservation::observationId))
                .toList();

        List<S10ReportAssessment> assessments = new ArrayList<>();
        snapshot.batchAssessments().forEach(value -> assessments.add(new S10ReportAssessment(
                S10CoverageFamily.BATCH_ITEM, value.assessmentId(), value.endpoint(), value.resourceId(),
                value.action(), value.policyReference(), value.expectedDecision(), value.observedDecision(),
                value.state(), value.confidence(), value.evidenceIds(), value.reasons())));
        snapshot.indirectAssessments().forEach(value -> assessments.add(new S10ReportAssessment(
                S10CoverageFamily.INDIRECT_REFERENCE, value.assessmentId(), value.endpoint(),
                value.resolvedResourceId(), value.action(), value.policyReference(), value.expectedDecision(),
                value.observedDecision(), value.state(), value.confidence(), value.evidenceIds(), value.reasons())));
        assessments = assessments.stream()
                .sorted(Comparator.comparing(S10ReportAssessment::assessmentId))
                .toList();

        List<S10ReportFindingCandidate> candidates = snapshot.candidates().stream()
                .sorted(Comparator.comparing(FindingCandidate::candidateId))
                .map(value -> new S10ReportFindingCandidate(
                        value.candidateId(), value.state(), value.endpoint(), value.resourceId(),
                        value.expectedDecision(), value.observedDecision(), value.confidence(),
                        value.policyReferences(), value.dimensions(), value.supportingEvidenceIds()))
                .toList();

        List<S10ReportCoverageContext> coverage = snapshot.coverageEntries().stream()
                .sorted(Comparator.comparing(S10AuthorizationCoverageEntry::coverageId))
                .map(value -> new S10ReportCoverageContext(
                        value.coverageId(), value.family(), value.policyReference(), value.endpoint(),
                        value.resourceId(), value.action(), value.roleId(), value.tenantId(),
                        value.expectedDecision(), value.disposition(), value.observationIds(),
                        value.assessmentIds(), value.findingCandidateIds(), value.findingStates()))
                .toList();

        var coverageSummary = snapshot.coverageSummary();
        int candidateCount = (int) candidates.stream()
                .filter(value -> value.state() == FindingCandidateState.CANDIDATE).count();
        int rejectedCount = (int) candidates.stream()
                .filter(value -> value.state() == FindingCandidateState.REJECTED).count();
        int inconclusiveCount = (int) candidates.stream()
                .filter(value -> value.state() == FindingCandidateState.INCONCLUSIVE).count();

        S10BatchIndirectReportSummary summary = new S10BatchIndirectReportSummary(
                coverageSummary.totalPolicyContexts(),
                coverageSummary.batchPolicyContexts(),
                coverageSummary.indirectPolicyContexts(),
                coverageSummary.observedContexts(),
                coverageSummary.assessedContexts(),
                coverageSummary.unobservedContexts(),
                coverageSummary.observedUnassessedContexts(),
                assessments.size(),
                candidateCount,
                rejectedCount,
                inconclusiveCount,
                0);

        TreeSet<String> evidence = new TreeSet<>();
        policies.forEach(value -> evidence.addAll(value.evidenceIds()));
        observations.forEach(value -> evidence.addAll(value.evidenceIds()));
        assessments.forEach(value -> evidence.addAll(value.evidenceIds()));
        candidates.forEach(value -> evidence.addAll(value.evidenceIds()));
        snapshot.coverageEntries().forEach(value -> evidence.addAll(value.policyEvidenceIds()));

        String material = REPORT_VERSION
                + "|" + policies.stream().map(S10BatchIndirectReportGenerator::policyIdentity).toList()
                + "|" + observations.stream().map(S10ReportObservation::observationId).toList()
                + "|" + assessments.stream().map(S10ReportAssessment::assessmentId).toList()
                + "|" + candidates.stream().map(S10ReportFindingCandidate::candidateId).toList()
                + "|" + coverage.stream().map(S10ReportCoverageContext::coverageId).toList();

        List<String> limitations = List.of(
                "Finding candidates are review candidates, not confirmed vulnerabilities.",
                "Aggregate HTTP success is not evidence of per-item batch authorization.",
                "Unobserved policy context is missing coverage and must not be interpreted as secure.",
                "Raw indirect-reference aliases are excluded from the Sprint 10 report schema.",
                "Indirect references are represented only by SHA-256 fingerprints and resolved resource identifiers.",
                "Controlled localhost validation does not establish real-world scanner accuracy.",
                "Real Burp desktop runtime validation remains a separate verification lane.");

        boolean empty = policies.isEmpty() && observations.isEmpty() && assessments.isEmpty()
                && candidates.isEmpty() && coverage.isEmpty();

        return new S10BatchIndirectReport(
                "s10-report-" + TokenFingerprint.sha256(material).substring(0, 24),
                REPORT_VERSION,
                empty ? S10BatchIndirectReportStatus.NO_AUTHORIZATION_EVIDENCE
                        : S10BatchIndirectReportStatus.READY_FOR_REVIEW,
                generatedAt,
                summary,
                policies,
                observations,
                assessments,
                candidates,
                coverage,
                List.copyOf(evidence),
                limitations);
    }

    private static String policyIdentity(S10ReportPolicyContext policy) {
        return String.join("|",
                policy.family().name(),
                policy.policyReference(),
                policy.endpoint(),
                policy.resourceId(),
                policy.action(),
                policy.roleId(),
                policy.tenantId(),
                policy.expectedDecision().name());
    }
}
