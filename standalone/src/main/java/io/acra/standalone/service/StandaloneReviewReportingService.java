package io.acra.standalone.service;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.security.UniversalRedactor;
import io.acra.standalone.model.AuthorizationExpectationRecord;
import io.acra.standalone.model.CandidateReviewRecord;
import io.acra.standalone.model.CandidateReviewState;
import io.acra.standalone.model.CoreAuthorizationProjectionRecord;
import io.acra.standalone.model.CoreProjectionSnapshot;
import io.acra.standalone.model.InventoryRecord;
import io.acra.standalone.model.StandaloneCandidateRecord;
import io.acra.standalone.model.StandaloneCoverageDisposition;
import io.acra.standalone.model.StandaloneCoverageRecord;
import io.acra.standalone.model.StandaloneCoverageSummary;
import io.acra.standalone.model.StandaloneReportArtifact;
import io.acra.standalone.store.CandidateReviewStore;
import io.acra.standalone.store.LocalWorkspaceStore;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class StandaloneReviewReportingService {
    private final LocalWorkspaceStore workspace;
    private final SecurityContextService contextService;
    private final StandaloneCoreProjectionService projectionService;
    private final StandaloneEvidenceService evidenceService;
    private final CandidateReviewStore reviewStore;
    private final UniversalRedactor redactor = new UniversalRedactor();

    public StandaloneReviewReportingService(LocalWorkspaceStore workspace) {
        this.workspace = java.util.Objects.requireNonNull(workspace);
        this.contextService = new SecurityContextService(workspace);
        this.projectionService = new StandaloneCoreProjectionService(workspace);
        this.evidenceService = new StandaloneEvidenceService(workspace);
        this.reviewStore = new CandidateReviewStore(workspace);
    }

    public List<StandaloneCandidateRecord> candidates(UUID projectId) throws IOException {
        CoreProjectionSnapshot projection = projectionService.project(projectId);
        Map<UUID, AuthorizationExpectationRecord> expectations = new HashMap<>();
        for (AuthorizationExpectationRecord expectation : contextService.expectations(projectId)) {
            expectations.put(expectation.id(), expectation);
        }

        List<StandaloneCandidateRecord> records = new ArrayList<>();
        for (CoreAuthorizationProjectionRecord row : projection.authorization()) {
            AuthorizationExpectationRecord expectation = expectations.get(row.expectationId());
            if (expectation == null) continue;
            FindingCandidate candidate = candidate(projectId, projection.policyFingerprint(), expectation, row);
            CandidateReviewRecord review = reviewStore.find(projectId, candidate.candidateId());
            if (review == null) {
                review = new CandidateReviewRecord(
                        projectId,
                        candidate.candidateId(),
                        CandidateReviewState.NEEDS_MORE_EVIDENCE,
                        "",
                        Instant.EPOCH);
            }
            records.add(new StandaloneCandidateRecord(candidate, review));
        }
        records.sort(Comparator.comparing(record -> record.candidate().candidateId()));
        return List.copyOf(records);
    }

    public CandidateReviewRecord updateReview(
            UUID projectId,
            String candidateId,
            String state,
            String note
    ) throws IOException {
        StandaloneCandidateRecord candidate = candidates(projectId).stream()
                .filter(record -> record.candidate().candidateId().equals(candidateId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown candidate"));

        CandidateReviewState reviewState;
        try {
            reviewState = CandidateReviewState.valueOf(required(state, "state").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("unsupported review state", ex);
        }

        if (note != null && !redactor.redactText(note).equals(note)) {
            throw new IllegalArgumentException("review note must not contain credential or secret material");
        }

        if (candidate.candidate().state() == FindingCandidateState.INCONCLUSIVE
                && reviewState == CandidateReviewState.UNDER_REVIEW) {
            // Analyst workflow state is independent from Core candidate state.
        }

        return reviewStore.save(new CandidateReviewRecord(
                projectId,
                candidateId,
                reviewState,
                note,
                Instant.now()
        ));
    }

    public List<StandaloneCoverageRecord> coverage(UUID projectId) throws IOException {
        List<InventoryRecord> inventory = workspace.listInventory(projectId);
        List<AuthorizationExpectationRecord> expectations = contextService.expectations(projectId);
        List<StandaloneCoverageRecord> records = new ArrayList<>();

        for (InventoryRecord endpoint : inventory) {
            int expectationCount = (int) expectations.stream()
                    .filter(value -> value.targetId().equals(endpoint.targetId())
                            && value.endpoint().equals(endpoint.canonicalPath()))
                    .count();

            boolean hasPassiveSource = endpoint.sourceTypes().stream()
                    .anyMatch(source -> source.equals("HAR") || source.equals("RAW_HTTP"));
            int passiveObservations = hasPassiveSource
                    ? (int) Math.max(1L, endpoint.observationCount() - (endpoint.documented() ? 1L : 0L))
                    : 0;

            StandaloneCoverageDisposition disposition;
            String reason;
            if (expectationCount == 0) {
                disposition = StandaloneCoverageDisposition.UNTESTED;
                reason = "No expected-authorization context is configured for this endpoint.";
            } else if (passiveObservations == 0) {
                disposition = StandaloneCoverageDisposition.PARTIAL;
                reason = "Authorization context exists, but no passive HTTP observation is available.";
            } else {
                disposition = StandaloneCoverageDisposition.INCONCLUSIVE;
                reason = "Context and passive HTTP evidence exist, but authenticated active execution provenance is not yet available.";
            }

            String material = endpoint.targetId() + "|" + endpoint.method() + "|" + endpoint.canonicalPath();
            records.add(new StandaloneCoverageRecord(
                    "s10-coverage-" + TokenFingerprint.sha256(material).substring(0, 20),
                    endpoint.targetId(),
                    endpoint.method().name(),
                    endpoint.canonicalPath(),
                    disposition,
                    expectationCount,
                    passiveObservations,
                    reason
            ));
        }

        records.sort(Comparator
                .comparing(StandaloneCoverageRecord::endpoint)
                .thenComparing(StandaloneCoverageRecord::method)
                .thenComparing(record -> record.targetId().toString()));
        return List.copyOf(records);
    }

    public StandaloneCoverageSummary coverageSummary(UUID projectId) throws IOException {
        return StandaloneCoverageSummary.from(coverage(projectId));
    }

    public StandaloneReportArtifact report(UUID projectId, String format) throws IOException {
        String normalized = required(format, "format").toUpperCase(Locale.ROOT);
        List<StandaloneCandidateRecord> candidates = candidates(projectId);
        List<StandaloneCoverageRecord> coverage = coverage(projectId);
        StandaloneCoverageSummary summary = StandaloneCoverageSummary.from(coverage);
        int evidenceArtifacts = evidenceService.artifacts(projectId).size();
        int httpSamples = evidenceService.samples(projectId).size();

        return switch (normalized) {
            case "JSON" -> artifact("JSON", json(projectId, candidates, coverage, summary, evidenceArtifacts, httpSamples));
            case "MARKDOWN" -> artifact("MARKDOWN", markdown(projectId, candidates, summary, evidenceArtifacts, httpSamples));
            default -> throw new IllegalArgumentException("unsupported report format");
        };
    }

    private FindingCandidate candidate(
            UUID projectId,
            String policyFingerprint,
            AuthorizationExpectationRecord expectation,
            CoreAuthorizationProjectionRecord row
    ) {
        String material = projectId + "|" + expectation.id() + "|" + policyFingerprint;
        String candidateId = "s10-fc-" + TokenFingerprint.sha256(material).substring(0, 24);
        FindingFingerprint fingerprint = FindingFingerprint.of(
                expectation.endpoint(),
                expectation.resourceId(),
                expectation.principalId(),
                expectation.tenantId(),
                "AUTHORIZATION_EXPECTATION",
                "AWAITING_EXECUTION_EVIDENCE"
        );

        return new FindingCandidate(
                candidateId,
                FindingCandidateState.INCONCLUSIVE,
                projectId.toString(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of("AUTHORIZATION_EXPECTATION"),
                expectation.endpoint(),
                expectation.resourceId(),
                expectation.principalId(),
                "CONFIGURED",
                row.resolvedDecision(),
                AuthorizationDecision.UNKNOWN,
                List.of(),
                List.of(),
                policyFingerprint.isBlank() ? List.of() : List.of(policyFingerprint),
                "INSUFFICIENT",
                "Expected authorization is resolved through ACRA Core, but no authenticated execution/observation provenance exists; review-only and not a confirmed vulnerability.",
                fingerprint
        );
    }

    private StandaloneReportArtifact artifact(String format, String content) {
        return new StandaloneReportArtifact(format, content, TokenFingerprint.sha256(content));
    }

    private String json(
            UUID projectId,
            List<StandaloneCandidateRecord> candidates,
            List<StandaloneCoverageRecord> coverage,
            StandaloneCoverageSummary summary,
            int evidenceArtifacts,
            int httpSamples
    ) {
        String candidateJson = candidates.stream()
                .map(this::candidateJson)
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
        String coverageJson = coverage.stream()
                .map(this::coverageJson)
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));

        return "{"
                + "\"schemaVersion\":\"1.0\","
                + "\"type\":\"ACRAStandaloneReport\","
                + "\"projectId\":" + q(projectId.toString()) + ","
                + "\"confirmedFindingCount\":0,"
                + "\"candidateCount\":" + candidates.size() + ","
                + "\"evidenceArtifactCount\":" + evidenceArtifacts + ","
                + "\"httpSampleCount\":" + httpSamples + ","
                + "\"coverageSummary\":{"
                    + "\"total\":" + summary.total() + ","
                    + "\"tested\":" + summary.tested() + ","
                    + "\"untested\":" + summary.untested() + ","
                    + "\"partial\":" + summary.partial() + ","
                    + "\"inconclusive\":" + summary.inconclusive() + ","
                    + "\"notApplicable\":" + summary.notApplicable()
                + "},"
                + "\"candidates\":" + candidateJson + ","
                + "\"coverage\":" + coverageJson + ","
                + "\"limitations\":[\"NO_AUTHENTICATED_ACTIVE_EXECUTION_PROVENANCE\",\"NO_AUTOMATIC_CONFIRMED_FINDINGS\"]"
                + "}";
    }

    private String candidateJson(StandaloneCandidateRecord record) {
        FindingCandidate candidate = record.candidate();
        CandidateReviewRecord review = record.review();
        return "{"
                + "\"candidateId\":" + q(candidate.candidateId()) + ","
                + "\"state\":" + q(candidate.state().name()) + ","
                + "\"reviewState\":" + q(review.state().name()) + ","
                + "\"endpoint\":" + q(candidate.endpoint()) + ","
                + "\"principalId\":" + q(candidate.principalId()) + ","
                + "\"resourceId\":" + q(candidate.resourceId()) + ","
                + "\"expectedDecision\":" + q(candidate.expectedDecision().name()) + ","
                + "\"observedDecision\":" + q(candidate.observedDecision().name()) + ","
                + "\"confidence\":" + q(candidate.confidence()) + ","
                + "\"note\":" + q(review.note())
                + "}";
    }

    private String coverageJson(StandaloneCoverageRecord record) {
        return "{"
                + "\"coverageId\":" + q(record.coverageId()) + ","
                + "\"targetId\":" + q(record.targetId().toString()) + ","
                + "\"method\":" + q(record.method()) + ","
                + "\"endpoint\":" + q(record.endpoint()) + ","
                + "\"disposition\":" + q(record.disposition().name()) + ","
                + "\"expectationCount\":" + record.expectationCount() + ","
                + "\"passiveObservationCount\":" + record.passiveObservationCount() + ","
                + "\"reason\":" + q(record.reason())
                + "}";
    }

    private String markdown(
            UUID projectId,
            List<StandaloneCandidateRecord> candidates,
            StandaloneCoverageSummary summary,
            int evidenceArtifacts,
            int httpSamples
    ) {
        StringBuilder out = new StringBuilder();
        out.append("# ACRA Standalone Assessment Report\n\n");
        out.append("- Project: ").append(projectId).append("\n");
        out.append("- Confirmed findings: **0**\n");
        out.append("- Review candidates: **").append(candidates.size()).append("**\n");
        out.append("- Evidence artifacts: **").append(evidenceArtifacts).append("**\n");
        out.append("- HTTP samples: **").append(httpSamples).append("**\n\n");
        out.append("## Coverage\n\n");
        out.append("- Total: ").append(summary.total()).append("\n");
        out.append("- Tested: ").append(summary.tested()).append("\n");
        out.append("- Untested: ").append(summary.untested()).append("\n");
        out.append("- Partial: ").append(summary.partial()).append("\n");
        out.append("- Inconclusive: ").append(summary.inconclusive()).append("\n\n");

        out.append("## Review Candidates\n\n");
        if (candidates.isEmpty()) out.append("No candidate records.\n");
        for (StandaloneCandidateRecord record : candidates) {
            FindingCandidate candidate = record.candidate();
            out.append("- ").append(candidate.candidateId()).append(" — ")
                    .append(candidate.state()).append(" / ").append(record.review().state())
                    .append(" — ").append(candidate.endpoint())
                    .append(" — expected ").append(candidate.expectedDecision())
                    .append(", observed ").append(candidate.observedDecision()).append("\n");
        }

        out.append("\n## Limitations\n\n");
        out.append("- No authenticated active-execution provenance is available in this phase.\n");
        out.append("- Candidate records are review-only and are not automatically confirmed vulnerabilities.\n");
        return out.toString();
    }

    private static String q(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }

    private static String required(String value, String field) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) throw new IllegalArgumentException(field + " is required");
        return normalized;
    }
}
