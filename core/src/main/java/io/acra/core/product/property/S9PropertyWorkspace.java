package io.acra.core.product.property;

import io.acra.core.domain.authorization.PropertyAuthorizationAssessment;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.engine.PolicyValidationEvaluator;
import io.acra.core.property.PropertyAccessObservation;
import io.acra.core.property.PropertyAuthorizationCoverageEntry;
import io.acra.core.property.PropertyAuthorizationCoverageSummary;
import io.acra.core.property.S9PropertyCoverageTracker;
import java.util.List;
import java.util.TreeMap;

public final class S9PropertyWorkspace {
    private final TreeMap<String, PolicyValidationEvaluator.PropertyPolicy> policies = new TreeMap<>();
    private final TreeMap<String, PropertyAccessObservation> observations = new TreeMap<>();
    private final TreeMap<String, PropertyAuthorizationAssessment> assessments = new TreeMap<>();
    private final TreeMap<String, FindingCandidate> candidates = new TreeMap<>();
    private final TreeMap<String, PropertyAuthorizationCoverageEntry> coverage = new TreeMap<>();

    public synchronized void recordPolicy(PolicyValidationEvaluator.PropertyPolicy policy) {
        if (policy == null) throw new IllegalArgumentException("property policy required");
        PropertyAuthorizationCoverageEntry identity = PropertyAuthorizationCoverageEntry.from(policy);
        policies.put(identity.coverageId(), policy);
    }

    public synchronized void recordObservation(PropertyAccessObservation observation) {
        if (observation == null) throw new IllegalArgumentException("property observation required");
        observations.put(observation.observationId(), observation);
    }

    public synchronized void recordAssessment(PropertyAuthorizationAssessment assessment) {
        if (assessment == null) throw new IllegalArgumentException("property assessment required");
        assessments.put(assessment.assessmentId(), assessment);
    }

    public synchronized void recordCandidate(FindingCandidate candidate) {
        if (candidate == null) throw new IllegalArgumentException("finding candidate required");
        if (!candidate.dimensions().contains("PROPERTY")) {
            throw new IllegalArgumentException("property workspace accepts only PROPERTY finding projections");
        }
        candidates.put(candidate.candidateId(), candidate);
    }

    public synchronized void recordCoverage(PropertyAuthorizationCoverageEntry entry) {
        if (entry == null) throw new IllegalArgumentException("property coverage entry required");
        coverage.put(entry.coverageId(), entry);
    }

    public synchronized void replaceCoverage(S9PropertyCoverageTracker tracker) {
        if (tracker == null) throw new IllegalArgumentException("property coverage tracker required");
        coverage.clear();
        tracker.entries().forEach(entry -> coverage.put(entry.coverageId(), entry));
    }

    public synchronized void clearRuntimeState() {
        observations.clear();
        assessments.clear();
        candidates.clear();
        coverage.clear();
        for (PolicyValidationEvaluator.PropertyPolicy policy : policies.values()) {
            PropertyAuthorizationCoverageEntry entry = PropertyAuthorizationCoverageEntry.from(policy);
            coverage.put(entry.coverageId(), entry);
        }
    }

    public synchronized S9PropertyProductSnapshot snapshot() {
        List<PropertyAuthorizationCoverageEntry> coverageEntries = List.copyOf(coverage.values());
        return new S9PropertyProductSnapshot(
                List.copyOf(policies.values()),
                List.copyOf(observations.values()),
                List.copyOf(assessments.values()),
                List.copyOf(candidates.values()),
                coverageEntries,
                PropertyAuthorizationCoverageSummary.fromEntries(coverageEntries));
    }
}
