package io.acra.standalone.service;

import io.acra.core.active.analysis.AuthorizationOutcome;
import io.acra.core.active.analysis.DifferentialClassification;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.AuthorizationImpactProfile;
import io.acra.core.domain.finding.AuthorizationRiskAssessment;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.domain.finding.FindingLifecycleService;
import io.acra.core.domain.finding.FindingLifecycleState;
import io.acra.core.domain.finding.ReviewedFinding;
import io.acra.core.domain.testing.TestState;
import io.acra.core.engine.AuthorizationSeverityEvaluator;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.security.UniversalRedactor;
import io.acra.standalone.model.AuthorizationExpectationRecord;
import io.acra.standalone.model.ControlledExecutionRecord;
import io.acra.standalone.model.EvidenceArtifactRecord;
import io.acra.standalone.model.StandaloneFindingReviewRecord;
import io.acra.standalone.model.StoredFindingReviewRecord;
import io.acra.standalone.store.ControlledExecutionStore;
import io.acra.standalone.store.EvidenceArchiveStore;
import io.acra.standalone.store.LocalWorkspaceStore;
import io.acra.standalone.store.StandaloneFindingReviewStore;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class StandaloneFindingLifecycleService {
    private final LocalWorkspaceStore workspace;
    private final SecurityContextService contextService;
    private final ControlledExecutionStore executionStore;
    private final EvidenceArchiveStore evidenceStore;
    private final StandaloneFindingReviewStore findingStore;
    private final FindingLifecycleService lifecycle = new FindingLifecycleService();
    private final AuthorizationSeverityEvaluator severityEvaluator = new AuthorizationSeverityEvaluator();
    private final UniversalRedactor redactor = new UniversalRedactor();

    public StandaloneFindingLifecycleService(LocalWorkspaceStore workspace) {
        this.workspace = java.util.Objects.requireNonNull(workspace);
        this.contextService = new SecurityContextService(workspace);
        this.executionStore = new ControlledExecutionStore(workspace);
        this.evidenceStore = new EvidenceArchiveStore(workspace);
        this.findingStore = new StandaloneFindingReviewStore(workspace);
    }

    public List<ControlledExecutionRecord> eligibleExecutions(UUID projectId) throws IOException {
        List<ControlledExecutionRecord> eligible = new ArrayList<>();
        for (ControlledExecutionRecord execution : executionStore.list(projectId)) {
            if (!eligibleViolation(execution)) continue;
            try {
                validateSourceEvidence(projectId, execution);
                expectation(projectId, execution);
                eligible.add(execution);
            } catch (IllegalArgumentException ignored) {
                // Fail closed: malformed/stale execution lineage is not eligible for review intake.
            }
        }
        eligible.sort(Comparator
                .comparing(ControlledExecutionRecord::createdAt)
                .thenComparing(record -> record.runId().toString()));
        return List.copyOf(eligible);
    }

    public StandaloneFindingReviewRecord openFromExecution(UUID projectId, UUID runId) throws IOException {
        ControlledExecutionRecord execution = execution(projectId, runId);
        if (!eligibleViolation(execution)) {
            throw new IllegalArgumentException(
                    "execution is not an evidence-backed DENY-to-ALLOW unexpected authorization violation");
        }

        AuthorizationExpectationRecord expectation = expectation(projectId, execution);
        validateSourceEvidence(projectId, execution);

        FindingCandidate candidate = candidate(projectId, execution, expectation);
        AuthorizationRiskAssessment risk = severityEvaluator.evaluate(candidate, AuthorizationImpactProfile.none());

        StoredFindingReviewRecord existing = findingStore.findBySourceRun(projectId, runId);
        if (existing != null) {
            validateStoredSource(existing, candidate, risk);
            return new StandaloneFindingReviewRecord(runId, candidate, risk, existing.finding());
        }

        ReviewedFinding finding = lifecycle.open(candidate, risk, execution.createdAt());
        findingStore.save(projectId, new StoredFindingReviewRecord(runId, finding));
        return new StandaloneFindingReviewRecord(runId, candidate, risk, finding);
    }

    public StandaloneFindingReviewRecord transition(
            UUID projectId,
            String findingId,
            String targetState,
            String reviewerReference,
            String reason,
            List<UUID> reviewEvidenceIds
    ) throws IOException {
        StoredFindingReviewRecord stored = findingStore.find(projectId, findingId);
        ControlledExecutionRecord execution = execution(projectId, stored.sourceRunId());
        AuthorizationExpectationRecord expectation = expectation(projectId, execution);
        validateSourceEvidence(projectId, execution);

        FindingCandidate candidate = candidate(projectId, execution, expectation);
        AuthorizationRiskAssessment risk = severityEvaluator.evaluate(candidate, AuthorizationImpactProfile.none());
        validateStoredSource(stored, candidate, risk);

        FindingLifecycleState target = parseState(targetState);
        rejectSecretBearing(reviewerReference, "reviewerReference");
        rejectSecretBearing(reason, "reason");

        List<String> evidenceIds = validateReviewEvidence(
                projectId,
                execution.targetId(),
                reviewEvidenceIds);

        ReviewedFinding next = lifecycle.transition(
                stored.finding(),
                target,
                Instant.now(),
                reviewerReference,
                reason,
                evidenceIds);

        findingStore.save(projectId, new StoredFindingReviewRecord(stored.sourceRunId(), next));
        return new StandaloneFindingReviewRecord(stored.sourceRunId(), candidate, risk, next);
    }

    public List<StandaloneFindingReviewRecord> findings(UUID projectId) throws IOException {
        List<StandaloneFindingReviewRecord> out = new ArrayList<>();
        for (StoredFindingReviewRecord stored : findingStore.list(projectId)) {
            ControlledExecutionRecord execution = execution(projectId, stored.sourceRunId());
            AuthorizationExpectationRecord expectation = expectation(projectId, execution);
            validateSourceEvidence(projectId, execution);

            FindingCandidate candidate = candidate(projectId, execution, expectation);
            AuthorizationRiskAssessment risk = severityEvaluator.evaluate(candidate, AuthorizationImpactProfile.none());
            validateStoredSource(stored, candidate, risk);
            out.add(new StandaloneFindingReviewRecord(stored.sourceRunId(), candidate, risk, stored.finding()));
        }
        out.sort(Comparator.comparing(record -> record.finding().findingId()));
        return List.copyOf(out);
    }

    public StandaloneFindingReviewRecord finding(UUID projectId, String findingId) throws IOException {
        return findings(projectId).stream()
                .filter(record -> record.finding().findingId().equals(findingId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown finding"));
    }

    private ControlledExecutionRecord execution(UUID projectId, UUID runId) throws IOException {
        if (runId == null) throw new IllegalArgumentException("runId is required");
        return executionStore.list(projectId).stream()
                .filter(record -> record.runId().equals(runId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown controlled execution"));
    }

    private AuthorizationExpectationRecord expectation(
            UUID projectId,
            ControlledExecutionRecord execution
    ) throws IOException {
        AuthorizationExpectationRecord expectation = contextService.expectations(projectId).stream()
                .filter(record -> record.id().equals(execution.expectationId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("source authorization expectation is unavailable"));
        if (!expectation.targetId().equals(execution.targetId())) {
            throw new IllegalArgumentException("execution/expectation target mismatch");
        }
        if (!expectation.endpoint().equals(execution.endpoint())) {
            throw new IllegalArgumentException("execution/expectation endpoint mismatch");
        }
        return expectation;
    }

    private void validateSourceEvidence(UUID projectId, ControlledExecutionRecord execution) throws IOException {
        if (execution.coreEvidenceObjectCount() < 1) {
            throw new IllegalArgumentException("controlled execution has no Core evidence chain");
        }

        EvidenceArtifactRecord artifact = evidenceStore.findArtifact(projectId, execution.evidenceArtifactId());
        if (!artifact.targetId().equals(execution.targetId())) {
            throw new IllegalArgumentException("execution evidence target mismatch");
        }
        if (!"ACTIVE_EXECUTION".equals(artifact.evidenceType())) {
            throw new IllegalArgumentException("execution evidence is not an ACTIVE_EXECUTION artifact");
        }
    }

    private FindingCandidate candidate(
            UUID projectId,
            ControlledExecutionRecord execution,
            AuthorizationExpectationRecord expectation
    ) {
        String dimension = "ROUTE_EQUIVALENCE_AUTHORIZATION";
        String candidateMaterial = projectId + "|" + execution.runId() + "|" + execution.expectationId()
                + "|" + execution.testId() + "|" + execution.executionId();
        String candidateId = "s11-active-fc-" + TokenFingerprint.sha256(candidateMaterial).substring(0, 24);

        FindingFingerprint fingerprint = FindingFingerprint.of(
                execution.endpoint(),
                expectation.resourceId(),
                expectation.principalId(),
                expectation.tenantId(),
                dimension,
                "EXPECTED_DENY_OBSERVED_ALLOW");

        return new FindingCandidate(
                candidateId,
                FindingCandidateState.CANDIDATE,
                projectId.toString(),
                List.of(execution.testId()),
                List.of(execution.executionId()),
                execution.observationId().isBlank() ? List.of() : List.of(execution.observationId()),
                List.of("controlled-route-equivalence:" + execution.runId()),
                List.of(dimension),
                execution.endpoint(),
                expectation.resourceId(),
                expectation.principalId(),
                expectation.tenantId(),
                AuthorizationDecision.DENY,
                AuthorizationDecision.ALLOW,
                List.of(execution.evidenceArtifactId().toString()),
                List.of(),
                List.of("expectation:" + expectation.id()),
                "HIGH",
                "Completed controlled LAB route-equivalence execution produced expected DENY, observed ALLOW and "
                        + "UNEXPECTED_CHANGE with persisted active-execution evidence. Human review is required.",
                fingerprint);
    }

    private static boolean eligibleViolation(ControlledExecutionRecord execution) {
        return execution.state() == TestState.COMPLETED
                && execution.expectedDecision() == AuthorizationDecision.DENY
                && execution.observedDecision() == AuthorizationOutcome.ALLOW
                && execution.differentialClassification() == DifferentialClassification.UNEXPECTED_CHANGE
                && execution.coreEvidenceObjectCount() > 0;
    }

    private void validateStoredSource(
            StoredFindingReviewRecord stored,
            FindingCandidate candidate,
            AuthorizationRiskAssessment risk
    ) {
        ReviewedFinding finding = stored.finding();
        if (!finding.candidateId().equals(candidate.candidateId())) {
            throw new IllegalArgumentException("persisted finding candidate source drift");
        }
        if (!finding.projectId().equals(candidate.projectId())) {
            throw new IllegalArgumentException("persisted finding project source drift");
        }
        if (!finding.fingerprint().equals(candidate.fingerprint())) {
            throw new IllegalArgumentException("persisted finding fingerprint source drift");
        }
        if (finding.severity() != risk.severity()) {
            throw new IllegalArgumentException("persisted finding severity source drift");
        }
        if (finding.confidence() != risk.confidence()) {
            throw new IllegalArgumentException("persisted finding confidence source drift");
        }
    }

    private List<String> validateReviewEvidence(
            UUID projectId,
            UUID targetId,
            List<UUID> reviewEvidenceIds
    ) throws IOException {
        if (reviewEvidenceIds == null || reviewEvidenceIds.isEmpty()) {
            throw new IllegalArgumentException("review evidence is required");
        }

        List<String> out = new ArrayList<>();
        for (UUID evidenceId : reviewEvidenceIds) {
            if (evidenceId == null) throw new IllegalArgumentException("review evidence id is required");
            EvidenceArtifactRecord artifact = evidenceStore.findArtifact(projectId, evidenceId);
            if (!artifact.targetId().equals(targetId)) {
                throw new IllegalArgumentException("review evidence target mismatch");
            }
            out.add(evidenceId.toString());
        }
        return out.stream().distinct().sorted().toList();
    }

    private FindingLifecycleState parseState(String value) {
        String normalized = value == null ? "" : value.strip().toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) throw new IllegalArgumentException("targetState is required");
        try {
            return FindingLifecycleState.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("unsupported finding lifecycle state", ex);
        }
    }

    private void rejectSecretBearing(String value, String field) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) throw new IllegalArgumentException(field + " is required");
        if (!redactor.redactText(normalized).equals(normalized)) {
            throw new IllegalArgumentException(field + " must not contain credential or secret material");
        }
    }
}
