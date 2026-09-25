package io.acra.core.reporting.s7;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.PolicyResolutionState;
import io.acra.core.domain.finding.AuthorizationRiskAssessment;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.workflow.S7WorkflowAnalysisResult;
import io.acra.core.domain.workflow.WorkflowAuthorizationResolution;
import io.acra.core.domain.workflow.WorkflowCoverageSummary;
import io.acra.core.domain.workflow.WorkflowPolicySnapshot;
import io.acra.core.domain.workflow.WorkflowTransitionAssessment;
import io.acra.core.domain.workflow.WorkflowTransitionAssessmentState;
import io.acra.core.domain.workflow.WorkflowTransitionCoverageEntry;
import io.acra.core.product.workflow.S7WorkflowProductSnapshot;
import io.acra.core.security.TokenFingerprint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

public final class S7WorkflowReportGenerator {
    public static final String REPORT_VERSION = "s7-workflow-report-v1";

    public S7WorkflowReport generate(S7WorkflowProductSnapshot snapshot, Instant generatedAt) {
        if (snapshot == null || generatedAt == null) {
            throw new IllegalArgumentException("snapshot/generatedAt required");
        }
        WorkflowPolicySnapshot policy = snapshot.policy();
        List<WorkflowAuthorizationResolution> resolutions = snapshot.resolutions().stream()
                .sorted(Comparator.comparing(WorkflowAuthorizationResolution::resolutionId))
                .toList();
        List<S7WorkflowAnalysisResult> analyses = snapshot.analyses();
        List<WorkflowTransitionAssessment> assessments = analyses.stream()
                .map(S7WorkflowAnalysisResult::assessment).filter(Objects::nonNull)
                .sorted(Comparator.comparing(WorkflowTransitionAssessment::assessmentId)).toList();
        List<FindingCandidate> candidates = analyses.stream()
                .map(S7WorkflowAnalysisResult::findingCandidate).filter(Objects::nonNull)
                .sorted(Comparator.comparing(FindingCandidate::candidateId)).toList();
        List<AuthorizationRiskAssessment> risks = analyses.stream()
                .map(S7WorkflowAnalysisResult::riskAssessment).filter(Objects::nonNull)
                .sorted(Comparator.comparing(AuthorizationRiskAssessment::riskId)).toList();
        List<WorkflowTransitionCoverageEntry> coverageEntries = snapshot.coverageEntries().stream()
                .sorted(Comparator.comparing(WorkflowTransitionCoverageEntry::coverageId)).toList();

        int allow = (int) resolutions.stream()
                .filter(value -> value.expectedDecision() == AuthorizationDecision.ALLOW).count();
        int deny = (int) resolutions.stream()
                .filter(value -> value.expectedDecision() == AuthorizationDecision.DENY).count();
        int conflicts = (int) resolutions.stream()
                .filter(value -> value.state() == PolicyResolutionState.CONFLICTING).count();
        int assessmentCandidates = (int) assessments.stream()
                .filter(value -> value.state() == WorkflowTransitionAssessmentState.CANDIDATE).count();
        int findingCandidates = (int) candidates.stream()
                .filter(value -> value.state() == FindingCandidateState.CANDIDATE).count();
        WorkflowCoverageSummary coverage = snapshot.coverageSummary();

        S7WorkflowReportSummary summary = new S7WorkflowReportSummary(
                resolutions.size(), allow, deny, conflicts, assessmentCandidates, findingCandidates, risks.size(), 0,
                coverage.totalContexts(), coverage.resolvedContexts(), coverage.plannedContexts(),
                coverage.attemptedContexts(), coverage.observedContexts(), coverage.observationRatio());

        TreeSet<String> evidence = new TreeSet<>();
        if (policy != null) {
            evidence.addAll(policy.evidenceIds());
            policy.transitionRules().forEach(value -> evidence.addAll(value.evidenceIds()));
            policy.tokenBindings().forEach(value -> evidence.addAll(value.evidenceIds()));
        }
        resolutions.forEach(value -> evidence.addAll(value.evidenceIds()));
        assessments.forEach(value -> evidence.addAll(value.evidenceIds()));
        candidates.forEach(value -> evidence.addAll(value.supportingEvidenceIds()));
        coverageEntries.forEach(value -> {
            evidence.addAll(value.policyEvidenceIds());
            evidence.addAll(value.observationEvidenceIds());
        });

        String material = REPORT_VERSION + "|" + (policy == null ? "NO_POLICY" : policy.fingerprint())
                + "|" + resolutions.stream().map(WorkflowAuthorizationResolution::resolutionId).toList()
                + "|" + coverageEntries.stream().map(WorkflowTransitionCoverageEntry::coverageId).toList()
                + "|" + candidates.stream().map(FindingCandidate::candidateId).toList();

        List<String> limitations = new ArrayList<>(List.of(
                "FindingCandidate is a review candidate, not a confirmed vulnerability.",
                "Workflow coverage records policy/planning/execution/observation lifecycle, not vulnerability severity.",
                "Normal export sanitization excludes raw credentials and authentication secrets.",
                "Controlled localhost workflow validation does not establish real-world scanner accuracy."));
        if (policy == null) {
            limitations.add("Workflow policy is not loaded; workflow authorization conclusions are incomplete.");
        }

        return new S7WorkflowReport(
                "s7-report-" + TokenFingerprint.sha256(material).substring(0, 24),
                REPORT_VERSION,
                policy == null ? S7WorkflowReportStatus.POLICY_NOT_LOADED : S7WorkflowReportStatus.READY_FOR_REVIEW,
                generatedAt,
                policy,
                summary,
                resolutions,
                assessments,
                coverageEntries,
                candidates,
                risks,
                List.copyOf(evidence),
                List.copyOf(limitations));
    }
}
