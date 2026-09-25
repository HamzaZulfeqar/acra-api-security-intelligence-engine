package io.acra.core.reporting.s9;

import io.acra.core.domain.authorization.PropertyAuthorizationAssessment;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.engine.PolicyValidationEvaluator;
import io.acra.core.product.property.S9PropertyProductSnapshot;
import io.acra.core.property.PropertyAccessObservation;
import io.acra.core.property.PropertyAuthorizationCoverageEntry;
import io.acra.core.security.TokenFingerprint;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

public final class S9PropertyReportGenerator {
    public static final String REPORT_VERSION = "s9-property-report-v1";

    public S9PropertyReport generate(S9PropertyProductSnapshot snapshot, Instant generatedAt) {
        if (snapshot == null || generatedAt == null) {
            throw new IllegalArgumentException("snapshot/generatedAt required");
        }

        List<PolicyValidationEvaluator.PropertyPolicy> policies = snapshot.policies().stream()
                .sorted(Comparator
                        .comparing(PolicyValidationEvaluator.PropertyPolicy::policyReference)
                        .thenComparing(PolicyValidationEvaluator.PropertyPolicy::endpoint)
                        .thenComparing(PolicyValidationEvaluator.PropertyPolicy::property)
                        .thenComparing(value -> value.operation().name()))
                .toList();
        List<PropertyAccessObservation> observations = snapshot.observations().stream()
                .sorted(Comparator.comparing(PropertyAccessObservation::observationId))
                .toList();
        List<PropertyAuthorizationAssessment> assessments = snapshot.assessments().stream()
                .sorted(Comparator.comparing(PropertyAuthorizationAssessment::assessmentId))
                .toList();
        List<FindingCandidate> candidates = snapshot.candidates().stream()
                .sorted(Comparator.comparing(FindingCandidate::candidateId))
                .toList();
        List<PropertyAuthorizationCoverageEntry> coverage = snapshot.coverageEntries().stream()
                .sorted(Comparator.comparing(PropertyAuthorizationCoverageEntry::coverageId))
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

        S9PropertyReportSummary summary = new S9PropertyReportSummary(
                coverageSummary.totalPolicyContexts(),
                coverageSummary.readPolicyContexts(),
                coverageSummary.updatePolicyContexts(),
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
        candidates.forEach(value -> evidence.addAll(value.supportingEvidenceIds()));
        coverage.forEach(value -> evidence.addAll(value.policyEvidenceIds()));

        String material = REPORT_VERSION
                + "|" + policies.stream().map(S9PropertyReportGenerator::policyIdentity).toList()
                + "|" + observations.stream().map(PropertyAccessObservation::observationId).toList()
                + "|" + assessments.stream().map(PropertyAuthorizationAssessment::assessmentId).toList()
                + "|" + candidates.stream().map(FindingCandidate::candidateId).toList()
                + "|" + coverage.stream().map(PropertyAuthorizationCoverageEntry::coverageId).toList();

        List<String> limitations = List.of(
                "FindingCandidate is a review candidate, not a confirmed vulnerability.",
                "Unobserved property-policy context is missing coverage and must not be interpreted as secure.",
                "Property values are not part of the Sprint 9 property observation, workspace or report model.",
                "Normal export sanitization excludes raw credentials and authentication secrets.",
                "Controlled localhost property validation does not establish real-world scanner accuracy.",
                "Real Burp desktop runtime validation remains a separate verification lane.");

        boolean empty = policies.isEmpty() && observations.isEmpty() && assessments.isEmpty()
                && candidates.isEmpty() && coverage.isEmpty();

        return new S9PropertyReport(
                "s9-report-" + TokenFingerprint.sha256(material).substring(0, 24),
                REPORT_VERSION,
                empty ? S9PropertyReportStatus.NO_PROPERTY_EVIDENCE : S9PropertyReportStatus.READY_FOR_REVIEW,
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

    private static String policyIdentity(PolicyValidationEvaluator.PropertyPolicy policy) {
        return String.join("|",
                policy.policyReference(),
                policy.endpoint(),
                policy.property(),
                policy.operation().name(),
                policy.roleId(),
                policy.tenantId(),
                policy.expectedDecision().name());
    }
}
