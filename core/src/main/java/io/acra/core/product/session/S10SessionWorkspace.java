package io.acra.core.product.session;

import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.session.AuthenticationSessionObservation;
import io.acra.core.session.S10SessionCoverageTracker;
import io.acra.core.session.SessionCorrelationResult;
import io.acra.core.session.SessionCoverageEntry;
import io.acra.core.session.SessionCoverageSummary;
import io.acra.core.session.SessionSecurityAssessment;
import io.acra.core.reporting.s10.S10SessionExportArtifact;
import io.acra.core.reporting.s10.S10SessionReport;
import io.acra.core.reporting.s10.S10SessionReportExporter;
import io.acra.core.reporting.s10.S10SessionReportGenerator;
import java.time.Instant;
import java.util.List;
import java.util.TreeMap;

public final class S10SessionWorkspace {
    private final TreeMap<String, AuthenticationSessionObservation> observations = new TreeMap<>();
    private final TreeMap<String, SessionCorrelationResult> correlations = new TreeMap<>();
    private final TreeMap<String, SessionSecurityAssessment> assessments = new TreeMap<>();
    private final TreeMap<String, FindingCandidate> candidates = new TreeMap<>();
    private final TreeMap<String, SessionCoverageEntry> coverage = new TreeMap<>();
    private final S10SessionReportGenerator reportGenerator = new S10SessionReportGenerator();
    private final S10SessionReportExporter reportExporter = new S10SessionReportExporter();

    public synchronized void recordObservation(AuthenticationSessionObservation observation) {
        if (observation == null) throw new IllegalArgumentException("session observation required");
        observations.put(observation.observationId(), observation);
    }

    public synchronized void recordCorrelation(SessionCorrelationResult correlation) {
        if (correlation == null) throw new IllegalArgumentException("session correlation required");
        correlations.put(correlation.correlationId(), correlation);
    }

    public synchronized void recordAssessment(SessionSecurityAssessment assessment) {
        if (assessment == null) throw new IllegalArgumentException("session assessment required");
        assessments.put(assessment.assessmentId(), assessment);
    }

    public synchronized void recordCandidate(FindingCandidate candidate) {
        if (candidate == null) throw new IllegalArgumentException("finding candidate required");
        if (!candidate.dimensions().contains("AUTHENTICATION_SESSION")) {
            throw new IllegalArgumentException(
                    "session workspace accepts only AUTHENTICATION_SESSION finding projections");
        }
        candidates.put(candidate.candidateId(), candidate);
    }

    public synchronized void recordCoverage(SessionCoverageEntry entry) {
        if (entry == null) throw new IllegalArgumentException("session coverage entry required");
        coverage.put(entry.target().coverageId(), entry);
    }

    public synchronized void replaceCoverage(S10SessionCoverageTracker tracker) {
        if (tracker == null) throw new IllegalArgumentException("session coverage tracker required");
        coverage.clear();
        tracker.entries().forEach(entry -> coverage.put(entry.target().coverageId(), entry));
    }

    public synchronized void clearRuntimeState() {
        observations.clear();
        correlations.clear();
        assessments.clear();
        candidates.clear();
        TreeMap<String, SessionCoverageEntry> targets = new TreeMap<>();
        coverage.values().forEach(entry ->
                targets.put(entry.target().coverageId(), SessionCoverageEntry.from(entry.target())));
        coverage.clear();
        coverage.putAll(targets);
    }

    public synchronized S10SessionReport report(Instant at) {
        return reportGenerator.generate(snapshot(), at);
    }

    public synchronized S10SessionExportArtifact exportJson(Instant at) {
        return reportExporter.json(report(at));
    }

    public synchronized S10SessionExportArtifact exportMarkdown(Instant at) {
        return reportExporter.markdown(report(at));
    }

    public synchronized S10SessionProductSnapshot snapshot() {
        List<SessionCoverageEntry> coverageEntries = List.copyOf(coverage.values());
        return new S10SessionProductSnapshot(
                List.copyOf(observations.values()),
                List.copyOf(correlations.values()),
                List.copyOf(assessments.values()),
                List.copyOf(candidates.values()),
                coverageEntries,
                SessionCoverageSummary.fromEntries(coverageEntries));
    }
}
