package io.acra.core.tests.sprint10;

import io.acra.core.batch.BatchItemAuthorizationAssessment;
import io.acra.core.batch.BatchItemObservation;
import io.acra.core.batch.BatchItemPolicy;
import io.acra.core.coverage.S10AuthorizationCoverageTracker;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.PolicyValidationState;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.product.batchindirect.S10BatchIndirectWorkspace;
import io.acra.core.reference.IndirectReferenceAuthorizationAssessment;
import io.acra.core.reference.IndirectReferencePolicy;
import io.acra.core.reference.IndirectReferenceResolution;
import io.acra.core.reference.IndirectReferenceSource;
import io.acra.core.reporting.s10.S10BatchIndirectJsonReporter;
import io.acra.core.reporting.s10.S10BatchIndirectReportStatus;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.tests.TestSupport;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class Sprint10BatchIndirectReportingExportTestSuite {
    private static final String BATCH_ENDPOINT = "/api/v1/s10/documents/batch-read";
    private static final String INDIRECT_ENDPOINT = "/api/v1/s10/share/{alias}";
    private static final Instant GENERATED_AT = Instant.parse("2026-09-25T00:00:00Z");

    private Sprint10BatchIndirectReportingExportTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT10_BATCH_INDIRECT_REPORTING_EXPORT PASS assertions=" + assertions);
    }

    public static int run() {
        S10BatchIndirectWorkspace workspace = fixtureWorkspace();
        var report = workspace.report(GENERATED_AT);
        int assertions = 0;

        TestSupport.assertEquals(S10BatchIndirectReportStatus.READY_FOR_REVIEW, report.status(),
                "non-empty Sprint 10 workspace produces review-ready report");
        assertions++;
        TestSupport.assertEquals(4, report.summary().policyContextCount(),
                "report preserves explicit four-context denominator");
        assertions++;
        TestSupport.assertEquals(2, report.summary().batchPolicyContextCount(),
                "report preserves two batch contexts");
        assertions++;
        TestSupport.assertEquals(2, report.summary().indirectPolicyContextCount(),
                "report preserves two indirect contexts");
        assertions++;
        TestSupport.assertEquals(3, report.summary().observedContextCount(),
                "report preserves three observed contexts");
        assertions++;
        TestSupport.assertEquals(3, report.summary().assessedContextCount(),
                "report preserves three assessed contexts");
        assertions++;
        TestSupport.assertEquals(1, report.summary().unobservedContextCount(),
                "report preserves one coverage gap");
        assertions++;
        TestSupport.assertEquals(1, report.summary().findingCandidateCount(),
                "report preserves one review candidate");
        assertions++;
        TestSupport.assertEquals(2, report.summary().rejectedControlCount(),
                "report preserves two rejected secure controls");
        assertions++;
        TestSupport.assertEquals(0, report.summary().confirmedFindingCount(),
                "Sprint 10 reporting cannot auto-confirm vulnerabilities");
        assertions++;
        TestSupport.assertEquals(4, report.policies().size(),
                "report emits four minimized policy projections");
        assertions++;
        TestSupport.assertEquals(3, report.observations().size(),
                "report emits three minimized observation projections");
        assertions++;
        TestSupport.assertEquals(3, report.assessments().size(),
                "report emits three minimized assessment projections");
        assertions++;
        TestSupport.assertEquals(4, report.coverageEntries().size(),
                "report emits all coverage contexts including unobserved");
        assertions++;

        var jsonA = workspace.exportJson(GENERATED_AT);
        var jsonB = workspace.exportJson(GENERATED_AT);
        TestSupport.assertEquals(jsonA.content(), jsonB.content(),
                "canonical Sprint 10 JSON export is deterministic");
        assertions++;
        TestSupport.assertEquals(jsonA.sha256(), jsonB.sha256(),
                "deterministic Sprint 10 JSON export produces stable digest");
        assertions++;
        TestSupport.assertEquals(TokenFingerprint.sha256(jsonA.content()), jsonA.sha256(),
                "Sprint 10 JSON SHA-256 matches exported content");
        assertions++;
        TestSupport.assertContains(jsonA.content(), "\"reportVersion\":\"s10-batch-indirect-report-v1\"",
                "canonical JSON declares stable report version");
        assertions++;
        TestSupport.assertContains(jsonA.content(), "\"confirmedFindingCount\":0",
                "canonical JSON preserves zero confirmed findings");
        assertions++;
        TestSupport.assertContains(jsonA.content(), "\"unobservedContextCount\":1",
                "canonical JSON preserves coverage gap");
        assertions++;
        TestSupport.assertContains(jsonA.content(), "\"referenceFingerprint\":",
                "canonical JSON preserves indirect reference fingerprint");
        assertions++;
        TestSupport.assertNotContains(jsonA.content(), "share-a",
                "canonical JSON excludes raw indirect alias share-a");
        assertions++;
        TestSupport.assertNotContains(jsonA.content(), "share-b",
                "canonical JSON excludes raw indirect alias share-b");
        assertions++;
        TestSupport.assertNotContains(jsonA.content(), "DummyPassword",
                "canonical JSON excludes candidate rationale secret material");
        assertions++;
        TestSupport.assertNotContains(jsonA.content(), "\"policySource\"",
                "report-specific policy projection structurally excludes policySource");
        assertions++;
        TestSupport.assertNotContains(jsonA.content(), "\"rationale\"",
                "report-specific candidate projection structurally excludes rationale");
        assertions++;

        var markdownA = workspace.exportMarkdown(GENERATED_AT);
        var markdownB = workspace.exportMarkdown(GENERATED_AT);
        TestSupport.assertEquals(markdownA.content(), markdownB.content(),
                "canonical Sprint 10 Markdown export is deterministic");
        assertions++;
        TestSupport.assertEquals(markdownA.sha256(), markdownB.sha256(),
                "deterministic Sprint 10 Markdown export produces stable digest");
        assertions++;
        TestSupport.assertContains(markdownA.content(), "Confirmed findings: 0",
                "Markdown preserves review-only boundary");
        assertions++;
        TestSupport.assertContains(markdownA.content(), "Unobserved contexts: 1",
                "Markdown preserves coverage gaps");
        assertions++;
        TestSupport.assertContains(markdownA.content(), "BATCH_ITEM",
                "Markdown keeps batch authorization family explicit");
        assertions++;
        TestSupport.assertContains(markdownA.content(), "INDIRECT_REFERENCE",
                "Markdown keeps indirect authorization family explicit");
        assertions++;
        TestSupport.assertContains(markdownA.content(), "referenceFingerprint=",
                "Markdown renders fingerprint rather than raw alias");
        assertions++;
        TestSupport.assertNotContains(markdownA.content(), "share-a",
                "Markdown excludes raw alias share-a");
        assertions++;
        TestSupport.assertNotContains(markdownA.content(), "share-b",
                "Markdown excludes raw alias share-b");
        assertions++;
        TestSupport.assertNotContains(markdownA.content(), "DummyPassword",
                "Markdown excludes candidate rationale secret material");
        assertions++;

        S10BatchIndirectJsonReporter reporter = new S10BatchIndirectJsonReporter();
        TestSupport.assertEquals("s10-batch-indirect-json-v1", reporter.id(),
                "Reporter plugin exposes stable Sprint 10 identifier");
        assertions++;
        TestSupport.assertEquals(jsonA.content(), reporter.render(Map.of("report", report)),
                "Reporter plugin uses canonical Sprint 10 JSON exporter");
        assertions++;

        var repeated = workspace.report(GENERATED_AT);
        TestSupport.assertEquals(report.reportId(), repeated.reportId(),
                "report identity is deterministic for identical workspace state");
        assertions++;

        var sameStateDifferentTime = workspace.report(GENERATED_AT.plusSeconds(60));
        TestSupport.assertEquals(report.reportId(), sameStateDifferentTime.reportId(),
                "report identity is state-derived and independent of render timestamp");
        assertions++;

        S10BatchIndirectWorkspace empty = new S10BatchIndirectWorkspace();
        TestSupport.assertEquals(S10BatchIndirectReportStatus.NO_AUTHORIZATION_EVIDENCE,
                empty.report(GENERATED_AT).status(),
                "empty workspace fails closed as no authorization evidence");
        assertions++;

        try {
            Path out = Path.of("build", "s10-foundation", "reporting");
            Files.createDirectories(out);
            Files.writeString(out.resolve("S10-BATCH-INDIRECT-REPORT.json"),
                    jsonA.content(), StandardCharsets.UTF_8);
            Files.writeString(out.resolve("S10-BATCH-INDIRECT-REPORT.json.sha256"),
                    jsonA.sha256() + "  S10-BATCH-INDIRECT-REPORT.json\n", StandardCharsets.UTF_8);
            Files.writeString(out.resolve("S10-BATCH-INDIRECT-REPORT.md"),
                    markdownA.content(), StandardCharsets.UTF_8);
            TestSupport.assertTrue(Files.isRegularFile(out.resolve("S10-BATCH-INDIRECT-REPORT.json")),
                    "canonical Sprint 10 JSON report artifact written");
            assertions++;
            TestSupport.assertTrue(Files.isRegularFile(out.resolve("S10-BATCH-INDIRECT-REPORT.md")),
                    "canonical Sprint 10 Markdown report artifact written");
            assertions++;
        } catch (java.io.IOException failure) {
            throw new IllegalStateException("unable to write Sprint 10 report artifacts", failure);
        }

        return assertions;
    }

    private static S10BatchIndirectWorkspace fixtureWorkspace() {
        S10BatchIndirectWorkspace workspace = new S10BatchIndirectWorkspace();
        S10AuthorizationCoverageTracker coverage = new S10AuthorizationCoverageTracker();

        BatchItemPolicy batchA = batchPolicy("s10-report-batch-a", "resource-a", AuthorizationDecision.ALLOW);
        BatchItemPolicy batchB = batchPolicy("s10-report-batch-b", "resource-b", AuthorizationDecision.DENY);
        IndirectReferencePolicy indirectA = indirectPolicy(
                "s10-report-indirect-a", "resource-a", AuthorizationDecision.ALLOW);
        IndirectReferencePolicy indirectB = indirectPolicy(
                "s10-report-indirect-b", "resource-b", AuthorizationDecision.DENY);

        for (BatchItemPolicy policy : List.of(batchA, batchB)) {
            workspace.recordPolicy(policy);
            coverage.recordPolicy(policy);
        }
        for (IndirectReferencePolicy policy : List.of(indirectA, indirectB)) {
            workspace.recordPolicy(policy);
            coverage.recordPolicy(policy);
        }

        BatchItemObservation batchAObservation = batchObservation(
                "s10-report-batch-obs-a", "item-a", "resource-a", AuthorizationDecision.ALLOW);
        BatchItemAuthorizationAssessment batchAAssessment = batchAssessment(
                "s10-report-batch-assessment-a", "item-a", batchA,
                AuthorizationDecision.ALLOW, PolicyValidationState.ALLOWED);
        FindingCandidate batchAFinding = finding(
                "s10-report-finding-batch-a", batchAAssessment.assessmentId(),
                "BATCH_AUTHORIZATION", batchA.endpoint(), "resource-a",
                batchAAssessment.expectedDecision(), batchAAssessment.observedDecision(),
                batchA.policyReference(), FindingCandidateState.REJECTED);
        workspace.recordObservation(batchAObservation);
        workspace.recordAssessment(batchAAssessment);
        workspace.recordCandidate(batchAFinding);
        coverage.recordObservation(batchA, batchAObservation);
        coverage.recordAssessment(batchA, batchAObservation, batchAAssessment, batchAFinding);

        BatchItemObservation batchBObservation = batchObservation(
                "s10-report-batch-obs-b", "item-b", "resource-b", AuthorizationDecision.ALLOW);
        BatchItemAuthorizationAssessment batchBAssessment = batchAssessment(
                "s10-report-batch-assessment-b", "item-b", batchB,
                AuthorizationDecision.ALLOW, PolicyValidationState.CONFLICTING);
        FindingCandidate batchBFinding = finding(
                "s10-report-finding-batch-b", batchBAssessment.assessmentId(),
                "BATCH_AUTHORIZATION", batchB.endpoint(), "resource-b",
                batchBAssessment.expectedDecision(), batchBAssessment.observedDecision(),
                batchB.policyReference(), FindingCandidateState.CANDIDATE);
        workspace.recordObservation(batchBObservation);
        workspace.recordAssessment(batchBAssessment);
        workspace.recordCandidate(batchBFinding);
        coverage.recordObservation(batchB, batchBObservation);
        coverage.recordAssessment(batchB, batchBObservation, batchBAssessment, batchBFinding);

        IndirectReferenceResolution indirectBResolution = indirectResolution(
                "s10-report-indirect-resolution-b", "share-b",
                "resource-b", AuthorizationDecision.DENY);
        IndirectReferenceAuthorizationAssessment indirectBAssessment = indirectAssessment(
                "s10-report-indirect-assessment-b", indirectB,
                "share-b", AuthorizationDecision.DENY, PolicyValidationState.DENIED);
        FindingCandidate indirectBFinding = finding(
                "s10-report-finding-indirect-b", indirectBAssessment.assessmentId(),
                "INDIRECT_REFERENCE_AUTHORIZATION", indirectB.endpoint(), "resource-b",
                indirectBAssessment.expectedDecision(), indirectBAssessment.observedDecision(),
                indirectB.policyReference(), FindingCandidateState.REJECTED);
        workspace.recordObservation(indirectBResolution);
        workspace.recordAssessment(indirectBAssessment);
        workspace.recordCandidate(indirectBFinding);
        coverage.recordObservation(indirectB, indirectBResolution);
        coverage.recordAssessment(indirectB, indirectBResolution, indirectBAssessment, indirectBFinding);

        workspace.replaceCoverage(coverage);
        return workspace;
    }

    private static BatchItemPolicy batchPolicy(
            String reference, String resourceId, AuthorizationDecision expected) {
        return new BatchItemPolicy(
                reference, "GT-S10-BATCH-INDIRECT-AUTHORIZATION", BATCH_ENDPOINT,
                resourceId, "READ", "viewer", "tenant-a", expected, List.of("evidence-" + reference));
    }

    private static IndirectReferencePolicy indirectPolicy(
            String reference, String resourceId, AuthorizationDecision expected) {
        return new IndirectReferencePolicy(
                reference, "GT-S10-BATCH-INDIRECT-AUTHORIZATION", INDIRECT_ENDPOINT,
                resourceId, "READ", "viewer", "tenant-a", expected, List.of("evidence-" + reference));
    }

    private static BatchItemObservation batchObservation(
            String id, String itemKey, String resourceId, AuthorizationDecision observed) {
        return new BatchItemObservation(
                id, "source-" + id, "s10-report-execution", "s10-report-test", "s10-report-batch",
                itemKey, BATCH_ENDPOINT, resourceId, "READ", observed, List.of("evidence-" + id));
    }

    private static BatchItemAuthorizationAssessment batchAssessment(
            String id, String itemKey, BatchItemPolicy policy,
            AuthorizationDecision observed, PolicyValidationState state) {
        return new BatchItemAuthorizationAssessment(
                id, "s10-report-batch", itemKey, policy.endpoint(), policy.resourceId(), policy.action(),
                policy.policyReference(), policy.expectedDecision(), observed, state,
                state == PolicyValidationState.CONFLICTING ? "HIGH" : "MEDIUM",
                List.of("evidence-" + id), "Sprint 10 reporting fixture", List.of());
    }

    private static IndirectReferenceResolution indirectResolution(
            String id, String rawAlias, String resourceId, AuthorizationDecision observed) {
        return new IndirectReferenceResolution(
                id, "source-" + id, "s10-report-execution", "s10-report-test", INDIRECT_ENDPOINT,
                TokenFingerprint.sha256(rawAlias), "ALIAS", resourceId, "READ", observed,
                IndirectReferenceSource.OBSERVED, List.of("evidence-" + id));
    }

    private static IndirectReferenceAuthorizationAssessment indirectAssessment(
            String id,
            IndirectReferencePolicy policy,
            String rawAlias,
            AuthorizationDecision observed,
            PolicyValidationState state) {
        return new IndirectReferenceAuthorizationAssessment(
                id, policy.endpoint(), TokenFingerprint.sha256(rawAlias), "ALIAS",
                policy.resolvedResourceId(), policy.action(), policy.policyReference(),
                policy.expectedDecision(), observed, state, "MEDIUM",
                List.of("evidence-" + id), "Sprint 10 reporting fixture", List.of());
    }

    private static FindingCandidate finding(
            String id,
            String assessmentId,
            String dimension,
            String endpoint,
            String resourceId,
            AuthorizationDecision expected,
            AuthorizationDecision observed,
            String policyReference,
            FindingCandidateState state) {
        return new FindingCandidate(
                id, state, "acra-s10-report", List.of("s10-report-test"),
                List.of("s10-report-execution"), List.of("s10-report-observation"),
                List.of(assessmentId), List.of(dimension), endpoint, resourceId,
                "user-a", "tenant-a", expected, observed,
                List.of("evidence-" + assessmentId), List.of(), List.of(policyReference),
                state == FindingCandidateState.CANDIDATE ? "HIGH" : "MEDIUM",
                "Sprint 10 report fixture; password=DummyPassword; candidate remains review-only",
                FindingFingerprint.of(endpoint, resourceId, "user-a", "tenant-a", dimension, state.name()));
    }
}
