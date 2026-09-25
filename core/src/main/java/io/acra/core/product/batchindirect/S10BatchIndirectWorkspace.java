package io.acra.core.product.batchindirect;

import io.acra.core.batch.BatchItemAuthorizationAssessment;
import io.acra.core.batch.BatchItemObservation;
import io.acra.core.batch.BatchItemPolicy;
import io.acra.core.coverage.S10AuthorizationCoverageEntry;
import io.acra.core.coverage.S10AuthorizationCoverageSummary;
import io.acra.core.coverage.S10AuthorizationCoverageTracker;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.reference.IndirectReferenceAuthorizationAssessment;
import io.acra.core.reference.IndirectReferencePolicy;
import io.acra.core.reference.IndirectReferenceResolution;
import io.acra.core.reporting.s10.S10BatchIndirectExportArtifact;
import io.acra.core.reporting.s10.S10BatchIndirectReport;
import io.acra.core.reporting.s10.S10BatchIndirectReportExporter;
import io.acra.core.reporting.s10.S10BatchIndirectReportGenerator;
import java.time.Instant;
import java.util.List;
import java.util.TreeMap;

public final class S10BatchIndirectWorkspace {
    private final TreeMap<String, BatchItemPolicy> batchPolicies = new TreeMap<>();
    private final TreeMap<String, IndirectReferencePolicy> indirectPolicies = new TreeMap<>();
    private final TreeMap<String, BatchItemObservation> batchObservations = new TreeMap<>();
    private final TreeMap<String, IndirectReferenceResolution> indirectResolutions = new TreeMap<>();
    private final TreeMap<String, BatchItemAuthorizationAssessment> batchAssessments = new TreeMap<>();
    private final TreeMap<String, IndirectReferenceAuthorizationAssessment> indirectAssessments = new TreeMap<>();
    private final TreeMap<String, FindingCandidate> candidates = new TreeMap<>();
    private final TreeMap<String, S10AuthorizationCoverageEntry> coverage = new TreeMap<>();
    private final S10BatchIndirectReportGenerator reportGenerator = new S10BatchIndirectReportGenerator();
    private final S10BatchIndirectReportExporter reportExporter = new S10BatchIndirectReportExporter();

    public synchronized void recordPolicy(BatchItemPolicy policy) {
        if (policy == null) throw new IllegalArgumentException("batch policy required");
        S10AuthorizationCoverageEntry identity = S10AuthorizationCoverageEntry.from(policy);
        batchPolicies.put(identity.coverageId(), policy);
    }

    public synchronized void recordPolicy(IndirectReferencePolicy policy) {
        if (policy == null) throw new IllegalArgumentException("indirect policy required");
        S10AuthorizationCoverageEntry identity = S10AuthorizationCoverageEntry.from(policy);
        indirectPolicies.put(identity.coverageId(), policy);
    }

    public synchronized void recordObservation(BatchItemObservation observation) {
        if (observation == null) throw new IllegalArgumentException("batch observation required");
        batchObservations.put(observation.itemObservationId(), observation);
    }

    public synchronized void recordObservation(IndirectReferenceResolution resolution) {
        if (resolution == null) throw new IllegalArgumentException("indirect resolution required");
        indirectResolutions.put(resolution.resolutionId(), resolution);
    }

    public synchronized void recordAssessment(BatchItemAuthorizationAssessment assessment) {
        if (assessment == null) throw new IllegalArgumentException("batch assessment required");
        batchAssessments.put(assessment.assessmentId(), assessment);
    }

    public synchronized void recordAssessment(IndirectReferenceAuthorizationAssessment assessment) {
        if (assessment == null) throw new IllegalArgumentException("indirect assessment required");
        indirectAssessments.put(assessment.assessmentId(), assessment);
    }

    public synchronized void recordCandidate(FindingCandidate candidate) {
        if (candidate == null) throw new IllegalArgumentException("finding candidate required");
        boolean batch = candidate.dimensions().contains("BATCH_AUTHORIZATION");
        boolean indirect = candidate.dimensions().contains("INDIRECT_REFERENCE_AUTHORIZATION");
        if (batch == indirect) {
            throw new IllegalArgumentException(
                    "Sprint 10 workspace accepts exactly one batch or indirect authorization dimension");
        }
        candidates.put(candidate.candidateId(), candidate);
    }

    public synchronized void recordCoverage(S10AuthorizationCoverageEntry entry) {
        if (entry == null) throw new IllegalArgumentException("S10 coverage entry required");
        coverage.put(entry.coverageId(), entry);
    }

    public synchronized void replaceCoverage(S10AuthorizationCoverageTracker tracker) {
        if (tracker == null) throw new IllegalArgumentException("S10 coverage tracker required");
        coverage.clear();
        tracker.entries().forEach(entry -> coverage.put(entry.coverageId(), entry));
    }

    public synchronized void clearRuntimeState() {
        batchObservations.clear();
        indirectResolutions.clear();
        batchAssessments.clear();
        indirectAssessments.clear();
        candidates.clear();
        coverage.clear();
        for (BatchItemPolicy policy : batchPolicies.values()) {
            S10AuthorizationCoverageEntry entry = S10AuthorizationCoverageEntry.from(policy);
            coverage.put(entry.coverageId(), entry);
        }
        for (IndirectReferencePolicy policy : indirectPolicies.values()) {
            S10AuthorizationCoverageEntry entry = S10AuthorizationCoverageEntry.from(policy);
            coverage.put(entry.coverageId(), entry);
        }
    }

    public synchronized S10BatchIndirectReport report(Instant at) {
        return reportGenerator.generate(snapshot(), at);
    }

    public synchronized S10BatchIndirectExportArtifact exportJson(Instant at) {
        return reportExporter.json(report(at));
    }

    public synchronized S10BatchIndirectExportArtifact exportMarkdown(Instant at) {
        return reportExporter.markdown(report(at));
    }

    public synchronized S10BatchIndirectProductSnapshot snapshot() {
        List<S10AuthorizationCoverageEntry> coverageEntries = List.copyOf(coverage.values());
        return new S10BatchIndirectProductSnapshot(
                List.copyOf(batchPolicies.values()),
                List.copyOf(indirectPolicies.values()),
                List.copyOf(batchObservations.values()),
                List.copyOf(indirectResolutions.values()),
                List.copyOf(batchAssessments.values()),
                List.copyOf(indirectAssessments.values()),
                List.copyOf(candidates.values()),
                coverageEntries,
                S10AuthorizationCoverageSummary.fromEntries(coverageEntries));
    }
}
