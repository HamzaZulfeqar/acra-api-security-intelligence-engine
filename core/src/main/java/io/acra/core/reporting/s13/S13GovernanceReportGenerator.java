package io.acra.core.reporting.s13;

import io.acra.core.product.finding.FindingGovernanceQueue;
import io.acra.core.product.finding.FindingGovernanceSnapshot;
import io.acra.core.security.TokenFingerprint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class S13GovernanceReportGenerator {
    public static final String VERSION = "s13-governance-report-v1";

    public S13GovernanceReport generate(FindingGovernanceSnapshot snapshot, Instant generatedAt) {
        if (snapshot == null) throw new IllegalArgumentException("governance snapshot required");
        if (generatedAt == null) throw new IllegalArgumentException("generatedAt required");

        List<S13GovernanceFindingProjection> findings = snapshot.findings().stream()
                .map(value -> new S13GovernanceFindingProjection(
                        value.findingId(),
                        value.sourceCandidateId(),
                        value.sourceCandidateFingerprint(),
                        value.riskAssessmentId(),
                        value.severity(),
                        value.confidence(),
                        value.state(),
                        value.confirmed(),
                        value.evidenceIds(),
                        value.events().size(),
                        value.fingerprint()))
                .sorted(Comparator.comparing(S13GovernanceFindingProjection::findingId))
                .toList();

        List<S13GovernanceEventProjection> eventRows = new ArrayList<>();
        snapshot.findings().forEach(finding -> finding.events().forEach(event ->
                eventRows.add(new S13GovernanceEventProjection(
                        finding.findingId(),
                        event.eventId(),
                        event.sequence(),
                        event.fromState(),
                        event.toState(),
                        event.action(),
                        event.reviewerReference(),
                        event.decisionReference(),
                        event.evidenceIds(),
                        event.fingerprint()))));
        List<S13GovernanceEventProjection> events = eventRows.stream()
                .sorted(Comparator.comparing(S13GovernanceEventProjection::findingId)
                        .thenComparingInt(S13GovernanceEventProjection::sequence))
                .toList();

        S13GovernanceReportSummary summary = new S13GovernanceReportSummary(
                snapshot.findingCount(),
                snapshot.queueCount(FindingGovernanceQueue.REVIEW_REQUIRED),
                snapshot.queueCount(FindingGovernanceQueue.CONFIRMED),
                snapshot.queueCount(FindingGovernanceQueue.REMEDIATION),
                snapshot.queueCount(FindingGovernanceQueue.RETEST),
                snapshot.queueCount(FindingGovernanceQueue.TERMINAL),
                snapshot.confirmedHistoryCount(),
                events.size());

        S13GovernanceReportStatus status = snapshot.findingCount() == 0
                ? S13GovernanceReportStatus.NO_GOVERNED_FINDINGS
                : S13GovernanceReportStatus.READY_FOR_REVIEW;

        List<String> limitations = List.of(
                "Governance report state is derived from explicit lifecycle events; severity/confidence do not confirm findings.",
                "Report generation performs no lifecycle transition, active replay, network action or Burp publication.",
                "Real Burp desktop runtime validation remains separate and unverified.");

        String reportId = "s13-governance-"
                + TokenFingerprint.sha256(canonical(summary, findings, events)).substring(0, 24);

        return new S13GovernanceReport(
                reportId,
                VERSION,
                status,
                generatedAt,
                summary,
                findings,
                events,
                limitations);
    }

    private static String canonical(
            S13GovernanceReportSummary summary,
            List<S13GovernanceFindingProjection> findings,
            List<S13GovernanceEventProjection> events) {
        StringBuilder out = new StringBuilder(VERSION).append('|').append(summary).append('\n');
        for (S13GovernanceFindingProjection finding : findings) {
            out.append(finding.findingId()).append('|')
                    .append(finding.sourceCandidateId()).append('|')
                    .append(finding.state()).append('|')
                    .append(finding.severity()).append('|')
                    .append(finding.confidence()).append('|')
                    .append(finding.confirmedHistory()).append('|')
                    .append(finding.evidenceIds()).append('|')
                    .append(finding.findingFingerprint()).append('\n');
        }
        for (S13GovernanceEventProjection event : events) {
            out.append(event.findingId()).append('|')
                    .append(event.eventId()).append('|')
                    .append(event.sequence()).append('|')
                    .append(event.fromState()).append('|')
                    .append(event.toState()).append('|')
                    .append(event.action()).append('|')
                    .append(event.reviewerReference()).append('|')
                    .append(event.decisionReference()).append('|')
                    .append(event.evidenceIds()).append('|')
                    .append(event.eventFingerprint()).append('\n');
        }
        return out.toString();
    }
}
