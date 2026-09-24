package io.acra.core.tests.sprint10;

import io.acra.core.active.analysis.AuthorizationOutcome;
import io.acra.core.active.analysis.DifferentialClassification;
import io.acra.core.active.analysis.ExpectedDecisionResolution;
import io.acra.core.active.analysis.ExpectedDecisionSource;
import io.acra.core.active.analysis.MultiWayDifferential;
import io.acra.core.active.evidence.EvidenceReferenceValidator;
import io.acra.core.active.evidence.EvidenceStage;
import io.acra.core.active.evidence.ExecutionEvidenceStore;
import io.acra.core.active.evidence.ExecutionFingerprint;
import io.acra.core.active.evidence.Observation;
import io.acra.core.active.evidence.ResponseSnapshot;
import io.acra.core.analysis.semantic.ResponseSemanticAnalyzer;
import io.acra.core.batch.BatchItemAuthorizationAssessment;
import io.acra.core.batch.S10BatchFindingRequest;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.PolicyValidationState;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.http.HttpProtocol;
import io.acra.core.domain.http.HttpResponse;
import io.acra.core.engine.S10BatchFindingCandidateEvaluator;
import io.acra.core.engine.S10IndirectFindingCandidateEvaluator;
import io.acra.core.recon.SecurityContextFingerprint;
import io.acra.core.reference.IndirectReferenceAuthorizationAssessment;
import io.acra.core.reference.S10IndirectFindingRequest;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.tests.TestSupport;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class Sprint10FindingCandidateProjectionTestSuite {
    private static final String PROJECT = "acra-s10";
    private static final String EXECUTION = "execution-s10-phase4";
    private static final String TEST = "test-s10-phase4";
    private static final String OBSERVATION = "observation-s10-phase4";
    private static final String BATCH_ENDPOINT = "/api/v1/s10/documents/batch-read";
    private static final String INDIRECT_ENDPOINT = "/api/v1/s10/share/{alias}";
    private static final String INDIRECT_FINGERPRINT = TokenFingerprint.sha256("share-b");

    private Sprint10FindingCandidateProjectionTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT10_FINDING_CANDIDATE_PROJECTION PASS assertions=" + assertions);
    }

    public static int run() {
        ExecutionEvidenceStore store = evidenceStore();
        EvidenceReferenceValidator validator = new EvidenceReferenceValidator(store);
        S10BatchFindingCandidateEvaluator batchEvaluator = new S10BatchFindingCandidateEvaluator(validator);
        S10IndirectFindingCandidateEvaluator indirectEvaluator = new S10IndirectFindingCandidateEvaluator(validator);
        int assertions = 0;

        BatchItemAuthorizationAssessment secureBatch = batchAssessment(
                "assessment-batch-secure", AuthorizationDecision.DENY, PolicyValidationState.DENIED);
        var secureBatchCandidate = batchEvaluator.evaluate(
                secureBatch, batchRequest(secureBatch, PROJECT, "resource-b"));
        TestSupport.assertEquals(FindingCandidateState.REJECTED, secureBatchCandidate.state(),
                "verified secure batch item projects to REJECTED");
        assertions++;
        TestSupport.assertEquals(0L,
                secureBatchCandidate.contradictoryEvidence().stream().filter(value -> value.contains("MISMATCH")).count(),
                "consistent secure batch projection has no request mismatch");
        assertions++;

        BatchItemAuthorizationAssessment vulnerableBatch = batchAssessment(
                "assessment-batch-vulnerable", AuthorizationDecision.ALLOW, PolicyValidationState.CONFLICTING);
        var vulnerableBatchCandidate = batchEvaluator.evaluate(
                vulnerableBatch, batchRequest(vulnerableBatch, PROJECT, "resource-b"));
        TestSupport.assertEquals(FindingCandidateState.CANDIDATE, vulnerableBatchCandidate.state(),
                "verified batch DENY-to-ALLOW mismatch projects to review candidate");
        assertions++;
        TestSupport.assertEquals("HIGH", vulnerableBatchCandidate.confidence(),
                "verified batch candidate has high internal evidence confidence");
        assertions++;
        TestSupport.assertContains(String.join(",", vulnerableBatchCandidate.dimensions()), "BATCH_AUTHORIZATION",
                "batch candidate retains batch authorization dimension");
        assertions++;
        TestSupport.assertContains(vulnerableBatchCandidate.rationale(), "not an automatically confirmed",
                "batch candidate preserves non-confirmation boundary");
        assertions++;

        var mismatchedBatch = batchEvaluator.evaluate(
                vulnerableBatch, batchRequest(vulnerableBatch, PROJECT, "resource-a"));
        TestSupport.assertEquals(FindingCandidateState.INCONCLUSIVE, mismatchedBatch.state(),
                "batch projection request mismatch fails closed");
        assertions++;
        TestSupport.assertContains(String.join(",", mismatchedBatch.contradictoryEvidence()),
                "BATCH_FINDING_REQUEST_MISMATCH",
                "batch mismatch reason is explicit");
        assertions++;

        var crossProjectBatch = batchEvaluator.evaluate(
                vulnerableBatch, batchRequest(vulnerableBatch, "other-project", "resource-b"));
        TestSupport.assertEquals(FindingCandidateState.INCONCLUSIVE, crossProjectBatch.state(),
                "cross-project batch projection fails closed");
        assertions++;

        var deterministicBatch = batchEvaluator.evaluate(
                vulnerableBatch, batchRequest(vulnerableBatch, PROJECT, "resource-b"));
        TestSupport.assertEquals(vulnerableBatchCandidate.candidateId(), deterministicBatch.candidateId(),
                "batch candidate identifier is deterministic");
        assertions++;

        IndirectReferenceAuthorizationAssessment secureIndirect = indirectAssessment(
                "assessment-indirect-secure", AuthorizationDecision.DENY, PolicyValidationState.DENIED);
        var secureIndirectCandidate = indirectEvaluator.evaluate(
                secureIndirect, indirectRequest(secureIndirect, PROJECT, INDIRECT_FINGERPRINT));
        TestSupport.assertEquals(FindingCandidateState.REJECTED, secureIndirectCandidate.state(),
                "verified secure resolved target projects to REJECTED");
        assertions++;
        TestSupport.assertEquals("resource-b", secureIndirectCandidate.resourceId(),
                "indirect candidate is fingerprinted around the resolved target resource");
        assertions++;

        IndirectReferenceAuthorizationAssessment vulnerableIndirect = indirectAssessment(
                "assessment-indirect-vulnerable", AuthorizationDecision.ALLOW, PolicyValidationState.CONFLICTING);
        var vulnerableIndirectCandidate = indirectEvaluator.evaluate(
                vulnerableIndirect, indirectRequest(vulnerableIndirect, PROJECT, INDIRECT_FINGERPRINT));
        TestSupport.assertEquals(FindingCandidateState.CANDIDATE, vulnerableIndirectCandidate.state(),
                "verified resolved-target DENY-to-ALLOW mismatch projects to review candidate");
        assertions++;
        TestSupport.assertContains(String.join(",", vulnerableIndirectCandidate.dimensions()),
                "INDIRECT_REFERENCE_AUTHORIZATION",
                "indirect candidate retains resolved-reference authorization dimension");
        assertions++;
        TestSupport.assertContains(vulnerableIndirectCandidate.rationale(), "not an automatically confirmed",
                "indirect candidate preserves non-confirmation boundary");
        assertions++;

        var mismatchedIndirect = indirectEvaluator.evaluate(
                vulnerableIndirect,
                indirectRequest(vulnerableIndirect, PROJECT, TokenFingerprint.sha256("share-a")));
        TestSupport.assertEquals(FindingCandidateState.INCONCLUSIVE, mismatchedIndirect.state(),
                "indirect fingerprint mismatch fails closed");
        assertions++;
        TestSupport.assertContains(String.join(",", mismatchedIndirect.contradictoryEvidence()),
                "INDIRECT_FINDING_REQUEST_MISMATCH",
                "indirect mismatch reason is explicit");
        assertions++;

        var crossProjectIndirect = indirectEvaluator.evaluate(
                vulnerableIndirect, indirectRequest(vulnerableIndirect, "other-project", INDIRECT_FINGERPRINT));
        TestSupport.assertEquals(FindingCandidateState.INCONCLUSIVE, crossProjectIndirect.state(),
                "cross-project indirect projection fails closed");
        assertions++;

        var deterministicIndirect = indirectEvaluator.evaluate(
                vulnerableIndirect, indirectRequest(vulnerableIndirect, PROJECT, INDIRECT_FINGERPRINT));
        TestSupport.assertEquals(vulnerableIndirectCandidate.candidateId(), deterministicIndirect.candidateId(),
                "indirect candidate identifier is deterministic");
        assertions++;

        TestSupport.assertNotContains(
                new io.acra.core.serialization.DomainSerializer().serialize(vulnerableIndirectCandidate),
                "share-b",
                "projected indirect candidate contains no raw alias material");
        assertions++;

        TestSupport.assertTrue(vulnerableBatchCandidate.state() != FindingCandidateState.REJECTED
                        && vulnerableBatchCandidate.state() != FindingCandidateState.INCONCLUSIVE,
                "batch violation reaches only the existing review candidate state");
        assertions++;
        TestSupport.assertTrue(vulnerableIndirectCandidate.state() != FindingCandidateState.REJECTED
                        && vulnerableIndirectCandidate.state() != FindingCandidateState.INCONCLUSIVE,
                "indirect violation reaches only the existing review candidate state");
        assertions++;

        return assertions;
    }

    private static BatchItemAuthorizationAssessment batchAssessment(
            String id, AuthorizationDecision observed, PolicyValidationState state) {
        return new BatchItemAuthorizationAssessment(
                id,
                "s10-controlled-batch",
                "item-b",
                BATCH_ENDPOINT,
                "resource-b",
                "READ",
                "s10-batch-policy-b",
                AuthorizationDecision.DENY,
                observed,
                state,
                state == PolicyValidationState.CONFLICTING ? "HIGH" : "MEDIUM",
                List.of("e-s10-phase4"),
                state == PolicyValidationState.CONFLICTING
                        ? "controlled batch mismatch" : "controlled batch secure result",
                List.of());
    }

    private static S10BatchFindingRequest batchRequest(
            BatchItemAuthorizationAssessment assessment,
            String projectId,
            String resourceId) {
        return new S10BatchFindingRequest(
                projectId,
                TEST,
                EXECUTION,
                OBSERVATION,
                BATCH_ENDPOINT,
                assessment.batchId(),
                assessment.itemKey(),
                resourceId,
                assessment.action(),
                "user-a",
                "tenant-a",
                assessment.policyReference(),
                assessment.expectedDecision(),
                assessment.observedDecision(),
                List.of("e-s10-phase4"));
    }

    private static IndirectReferenceAuthorizationAssessment indirectAssessment(
            String id, AuthorizationDecision observed, PolicyValidationState state) {
        return new IndirectReferenceAuthorizationAssessment(
                id,
                INDIRECT_ENDPOINT,
                INDIRECT_FINGERPRINT,
                "ALIAS",
                "resource-b",
                "READ",
                "s10-indirect-policy-resource-b",
                AuthorizationDecision.DENY,
                observed,
                state,
                state == PolicyValidationState.CONFLICTING ? "HIGH" : "MEDIUM",
                List.of("e-s10-phase4"),
                state == PolicyValidationState.CONFLICTING
                        ? "controlled indirect mismatch" : "controlled indirect secure result",
                List.of());
    }

    private static S10IndirectFindingRequest indirectRequest(
            IndirectReferenceAuthorizationAssessment assessment,
            String projectId,
            String fingerprint) {
        return new S10IndirectFindingRequest(
                projectId,
                TEST,
                EXECUTION,
                OBSERVATION,
                INDIRECT_ENDPOINT,
                fingerprint,
                assessment.referenceKind(),
                assessment.resolvedResourceId(),
                assessment.action(),
                "user-a",
                "tenant-a",
                assessment.policyReference(),
                assessment.expectedDecision(),
                assessment.observedDecision(),
                List.of("e-s10-phase4"));
    }

    private static ExecutionEvidenceStore evidenceStore() {
        ExecutionEvidenceStore store = new ExecutionEvidenceStore(PROJECT);
        store.append(EXECUTION, TEST, EvidenceStage.TEST, "e-s10-phase4", "s10-phase4-evidence");
        store.append(EXECUTION, TEST, EvidenceStage.OBSERVATION, OBSERVATION, observation());
        return store;
    }

    private static Observation observation() {
        ResponseSnapshot response = responseSnapshot();
        return new Observation(
                OBSERVATION,
                TEST,
                response,
                response,
                response,
                response,
                new ExpectedDecisionResolution(
                        AuthorizationDecision.UNKNOWN,
                        ExpectedDecisionSource.UNKNOWN,
                        "",
                        List.of(),
                        0.0,
                        false),
                AuthorizationOutcome.UNKNOWN,
                new MultiWayDifferential(
                        AuthorizationOutcome.UNKNOWN,
                        AuthorizationOutcome.UNKNOWN,
                        AuthorizationOutcome.UNKNOWN,
                        AuthorizationOutcome.UNKNOWN,
                        List.of(),
                        DifferentialClassification.INCONCLUSIVE,
                        List.of()),
                context(),
                context(),
                List.of("e-s10-phase4"),
                1.0,
                new ExecutionFingerprint(
                        EXECUTION,
                        TEST,
                        "request-s10-phase4",
                        "response-s10-phase4",
                        "configuration-s10-phase4",
                        "environment-s10-phase4"),
                Instant.parse("2026-09-24T19:40:00Z"));
    }

    private static ResponseSnapshot responseSnapshot() {
        HttpResponse response = new HttpResponse(
                200,
                List.of(),
                "{}".getBytes(),
                "application/json",
                HttpProtocol.HTTP_1_1,
                new byte[0]);
        return new ResponseSnapshot(
                "response-s10-phase4",
                "request-s10-phase4",
                response,
                Map.of(),
                Duration.ZERO,
                Instant.parse("2026-09-24T19:40:00Z"),
                new ResponseSemanticAnalyzer().fingerprint(response),
                "");
    }

    private static SecurityContextFingerprint context() {
        return new SecurityContextFingerprint(
                "user-a",
                "viewer",
                "tenant-a",
                "resource-b",
                "user-b",
                "READ",
                "ACTIVE",
                "GET",
                "/api/v1/s10",
                "token-fingerprint",
                AuthorizationDecision.UNKNOWN,
                AuthorizationDecision.UNKNOWN,
                List.of("e-s10-phase4"));
    }
}
