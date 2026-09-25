package io.acra.core.tests.sprint10;

import io.acra.core.active.analysis.AuthorizationOutcome;
import io.acra.core.active.analysis.DifferentialClassification;
import io.acra.core.active.analysis.ExpectedDecisionResolution;
import io.acra.core.active.analysis.ExpectedDecisionSource;
import io.acra.core.active.analysis.MultiWayDifferential;
import io.acra.core.active.evidence.EvidenceStage;
import io.acra.core.active.evidence.ExecutionEvidenceStore;
import io.acra.core.active.evidence.ExecutionFingerprint;
import io.acra.core.active.evidence.Observation;
import io.acra.core.active.evidence.ResponseSnapshot;
import io.acra.core.analysis.semantic.ResponseSemanticAnalyzer;
import io.acra.core.batch.BatchItemObservation;
import io.acra.core.batch.BatchItemPolicy;
import io.acra.core.batch.S10BatchAuthorizationAnalyzer;
import io.acra.core.domain.authorization.Action;
import io.acra.core.domain.authorization.ActionType;
import io.acra.core.domain.authorization.AuthorizationContext;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.ContextStatus;
import io.acra.core.domain.authorization.PolicyValidationState;
import io.acra.core.domain.common.Confidence;
import io.acra.core.domain.evidence.EvidenceSource;
import io.acra.core.domain.http.HttpProtocol;
import io.acra.core.domain.http.HttpResponse;
import io.acra.core.domain.identity.AuthenticationType;
import io.acra.core.domain.identity.Principal;
import io.acra.core.domain.identity.Role;
import io.acra.core.domain.resource.Resource;
import io.acra.core.domain.tenant.Tenant;
import io.acra.core.domain.workflow.WorkflowState;
import io.acra.core.reference.IndirectReferencePolicy;
import io.acra.core.reference.IndirectReferenceResolution;
import io.acra.core.reference.IndirectReferenceSource;
import io.acra.core.reference.S10IndirectReferenceAnalyzer;
import io.acra.core.recon.SecurityContextFingerprint;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.serialization.DomainSerializer;
import io.acra.core.tests.TestSupport;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class Sprint10BatchIndirectFoundationTestSuite {
    private static final String PROJECT = "acra-s10";
    private static final String EXECUTION = "execution-s10";
    private static final String TEST = "test-s10";
    private static final String SOURCE_OBSERVATION = "observation-s10";
    private static final String BATCH_ENDPOINT = "/api/v1/s10/documents/batch";
    private static final String INDIRECT_ENDPOINT = "/api/v1/s10/links/resolve";

    private Sprint10BatchIndirectFoundationTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT10_BATCH_INDIRECT_FOUNDATION PASS assertions=" + assertions);
    }

    public static int run() {
        ExecutionEvidenceStore store = store();
        AuthorizationContext actor = context();
        int assertions = 0;

        S10BatchAuthorizationAnalyzer batchAnalyzer = new S10BatchAuthorizationAnalyzer(store);
        List<BatchItemPolicy> batchPolicies = List.of(
                batchPolicy("batch-policy-a", "resource-a", "READ", AuthorizationDecision.ALLOW, "e-batch-a"),
                batchPolicy("batch-policy-b", "resource-b", "READ", AuthorizationDecision.DENY, "e-batch-b"),
                batchPolicy("batch-policy-c", "resource-c", "DELETE", AuthorizationDecision.DENY, "e-batch-c"));
        List<BatchItemObservation> batchObservations = List.of(
                batchObservation("item-observation-a", "batch-1", "item-a", "resource-a", "READ",
                        AuthorizationDecision.ALLOW, "e-batch-a"),
                batchObservation("item-observation-b", "batch-1", "item-b", "resource-b", "READ",
                        AuthorizationDecision.DENY, "e-batch-b"),
                batchObservation("item-observation-c", "batch-1", "item-c", "resource-c", "DELETE",
                        AuthorizationDecision.ALLOW, "e-batch-c"));

        var batch = batchAnalyzer.analyze(actor, batchPolicies, batchObservations, PROJECT);
        TestSupport.assertEquals(3, batch.assessments().size(),
                "batch analysis preserves every item-level authorization decision");
        assertions++;
        TestSupport.assertEquals(1L, batch.candidateCount(),
                "one DENY-to-ALLOW batch item becomes a review candidate");
        assertions++;
        TestSupport.assertTrue(batch.mixedObservedDecisions(),
                "mixed ALLOW/DENY item outcomes remain explicit");
        assertions++;
        TestSupport.assertTrue(batch.complete(),
                "fully policy-backed batch analysis can be complete");
        assertions++;
        TestSupport.assertEquals(PolicyValidationState.ALLOWED, batch.assessments().get(0).state(),
                "allowed batch item remains independently allowed");
        assertions++;
        TestSupport.assertEquals(PolicyValidationState.DENIED, batch.assessments().get(1).state(),
                "denied batch item remains independently denied");
        assertions++;
        TestSupport.assertTrue(batch.assessments().get(2).violationCandidate(),
                "batch-level success cannot erase a denied item's authorization mismatch");
        assertions++;

        BatchItemObservation missingPolicyObservation = batchObservation(
                "item-observation-missing", "batch-2", "item-z", "resource-z", "READ",
                AuthorizationDecision.ALLOW, "e-batch-z");
        var missingBatchPolicy = batchAnalyzer.analyze(
                actor, batchPolicies, List.of(missingPolicyObservation), PROJECT);
        TestSupport.assertEquals(1L, missingBatchPolicy.inconclusiveCount(),
                "missing batch item policy fails closed");
        assertions++;
        TestSupport.assertContains(String.join(",", missingBatchPolicy.reasons()), "BATCH_ITEM_POLICY_NOT_FOUND",
                "missing batch policy reason remains explicit");
        assertions++;

        BatchItemPolicy ambiguousOne = batchPolicy(
                "batch-ambiguous-one", "resource-z", "READ", AuthorizationDecision.DENY, "e-batch-z");
        BatchItemPolicy ambiguousTwo = batchPolicy(
                "batch-ambiguous-two", "resource-z", "READ", AuthorizationDecision.ALLOW, "e-batch-z");
        var ambiguousBatch = batchAnalyzer.analyze(
                actor, List.of(ambiguousTwo, ambiguousOne), List.of(missingPolicyObservation), PROJECT);
        TestSupport.assertEquals(PolicyValidationState.CONFLICTING,
                ambiguousBatch.assessments().getFirst().state(),
                "ambiguous batch policy remains conflicting");
        assertions++;
        TestSupport.assertEquals(0L, ambiguousBatch.candidateCount(),
                "ambiguous batch policy cannot become a candidate");
        assertions++;

        var crossProjectBatch = batchAnalyzer.analyze(
                actor, batchPolicies, List.of(batchObservations.getFirst()), "other-project");
        TestSupport.assertEquals(1L, crossProjectBatch.inconclusiveCount(),
                "cross-project batch provenance fails closed");
        assertions++;

        var batchDeterministicA = batchAnalyzer.analyze(actor, batchPolicies, batchObservations, PROJECT);
        var batchDeterministicB = batchAnalyzer.analyze(
                actor,
                List.of(batchPolicies.get(2), batchPolicies.get(0), batchPolicies.get(1)),
                List.of(batchObservations.get(2), batchObservations.get(0), batchObservations.get(1)),
                PROJECT);
        TestSupport.assertEquals(batchDeterministicA.analysisId(), batchDeterministicB.analysisId(),
                "batch analysis identity is deterministic across input ordering");
        assertions++;

        S10IndirectReferenceAnalyzer indirectAnalyzer = new S10IndirectReferenceAnalyzer(store);
        String rawAlias = "share-link-alpha";
        String fingerprint = TokenFingerprint.sha256(rawAlias);
        IndirectReferenceResolution indirect = indirectResolution(
                "resolution-alpha", fingerprint, "resource-b", AuthorizationDecision.ALLOW, "e-indirect-a");
        IndirectReferencePolicy indirectPolicy = indirectPolicy(
                "indirect-policy-b", "resource-b", AuthorizationDecision.DENY, "e-indirect-a");

        var indirectResult = indirectAnalyzer.analyze(
                actor, List.of(indirectPolicy), List.of(indirect), PROJECT);
        TestSupport.assertEquals(1L, indirectResult.candidateCount(),
                "resolved target DENY plus observed ALLOW becomes an indirect-reference candidate");
        assertions++;
        TestSupport.assertEquals("resource-b",
                indirectResult.assessments().getFirst().resolvedResourceId(),
                "indirect authorization is bound to the resolved resource");
        assertions++;
        TestSupport.assertTrue(indirectResult.complete(),
                "single verified resolution and policy can be complete");
        assertions++;
        TestSupport.assertNotContains(new DomainSerializer().serialize(indirect), rawAlias,
                "raw indirect reference values are not stored");
        assertions++;

        var missingIndirectPolicy = indirectAnalyzer.analyze(
                actor, List.of(), List.of(indirect), PROJECT);
        TestSupport.assertEquals(1L, missingIndirectPolicy.inconclusiveCount(),
                "missing resolved-resource policy fails closed");
        assertions++;

        IndirectReferenceResolution conflictingResolution = indirectResolution(
                "resolution-alpha-conflict", fingerprint, "resource-c",
                AuthorizationDecision.ALLOW, "e-indirect-b");
        var resolutionConflict = indirectAnalyzer.analyze(
                actor,
                List.of(
                        indirectPolicy,
                        indirectPolicy("indirect-policy-c", "resource-c",
                                AuthorizationDecision.DENY, "e-indirect-b")),
                List.of(indirect, conflictingResolution),
                PROJECT);
        TestSupport.assertTrue(resolutionConflict.resolutionConflict(),
                "one indirect fingerprint resolving to multiple resources remains explicit conflict");
        assertions++;
        TestSupport.assertEquals(2L, resolutionConflict.inconclusiveCount(),
                "conflicting indirect resolution cannot be authorized deterministically");
        assertions++;
        TestSupport.assertEquals(0L, resolutionConflict.candidateCount(),
                "conflicting indirect resolution cannot be promoted");
        assertions++;

        var crossProjectIndirect = indirectAnalyzer.analyze(
                actor, List.of(indirectPolicy), List.of(indirect), "other-project");
        TestSupport.assertEquals(1L, crossProjectIndirect.inconclusiveCount(),
                "cross-project indirect-reference provenance fails closed");
        assertions++;

        String fingerprintTwo = TokenFingerprint.sha256("share-link-beta");
        IndirectReferenceResolution indirectTwo = indirectResolution(
                "resolution-beta", fingerprintTwo, "resource-a",
                AuthorizationDecision.ALLOW, "e-indirect-b");
        IndirectReferencePolicy policyTwo = indirectPolicy(
                "indirect-policy-a", "resource-a", AuthorizationDecision.ALLOW, "e-indirect-b");
        var indirectDeterministicA = indirectAnalyzer.analyze(
                actor, List.of(indirectPolicy, policyTwo), List.of(indirect, indirectTwo), PROJECT);
        var indirectDeterministicB = indirectAnalyzer.analyze(
                actor, List.of(policyTwo, indirectPolicy), List.of(indirectTwo, indirect), PROJECT);
        TestSupport.assertEquals(indirectDeterministicA.analysisId(), indirectDeterministicB.analysisId(),
                "indirect analysis identity is deterministic across input ordering");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> new IndirectReferenceResolution(
                        "bad-resolution", SOURCE_OBSERVATION, EXECUTION, TEST, INDIRECT_ENDPOINT,
                        "raw-alias-value", "ALIAS", "resource-a", "READ", AuthorizationDecision.ALLOW,
                        IndirectReferenceSource.OBSERVED, List.of("e-indirect-a")),
                "indirect reference model requires a SHA-256 fingerprint instead of raw reference material");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> batchObservation(
                        "item-secret", "batch-secret", "Authorization: Bearer secret-value",
                        "resource-a", "READ", AuthorizationDecision.ALLOW, "e-batch-a"),
                "secret-bearing batch item metadata is rejected");
        assertions++;

        return assertions;
    }

    private static BatchItemPolicy batchPolicy(
            String reference, String resource, String action,
            AuthorizationDecision expected, String evidence) {
        return new BatchItemPolicy(
                reference, "explicit-s10-fixture", BATCH_ENDPOINT, resource, action,
                "viewer", "tenant-a", expected, List.of(evidence));
    }

    private static BatchItemObservation batchObservation(
            String itemObservationId, String batchId, String itemKey,
            String resource, String action, AuthorizationDecision observed, String evidence) {
        return new BatchItemObservation(
                itemObservationId, SOURCE_OBSERVATION, EXECUTION, TEST, batchId, itemKey,
                BATCH_ENDPOINT, resource, action, observed, List.of(evidence));
    }

    private static IndirectReferencePolicy indirectPolicy(
            String reference, String resource, AuthorizationDecision expected, String evidence) {
        return new IndirectReferencePolicy(
                reference, "explicit-s10-fixture", INDIRECT_ENDPOINT, resource, "READ",
                "viewer", "tenant-a", expected, List.of(evidence));
    }

    private static IndirectReferenceResolution indirectResolution(
            String resolutionId, String fingerprint, String resource,
            AuthorizationDecision observed, String evidence) {
        return new IndirectReferenceResolution(
                resolutionId, SOURCE_OBSERVATION, EXECUTION, TEST, INDIRECT_ENDPOINT,
                fingerprint, "ALIAS", resource, "READ", observed,
                IndirectReferenceSource.OBSERVED, List.of(evidence));
    }

    private static ExecutionEvidenceStore store() {
        ExecutionEvidenceStore store = new ExecutionEvidenceStore(PROJECT);
        for (String evidence : List.of(
                "e-batch-a", "e-batch-b", "e-batch-c", "e-batch-z",
                "e-indirect-a", "e-indirect-b")) {
            store.append(EXECUTION, TEST, EvidenceStage.TEST, evidence, "s10-foundation-evidence");
        }
        store.append(EXECUTION, TEST, EvidenceStage.OBSERVATION, SOURCE_OBSERVATION, sourceObservation());
        return store;
    }

    private static Observation sourceObservation() {
        ResponseSnapshot response = responseSnapshot();
        SecurityContextFingerprint fingerprint = new SecurityContextFingerprint(
                "user-a", "viewer", "tenant-a", "batch-and-reference", "user-a",
                "READ", "ACTIVE", "POST", "/api/v1/s10", "token-fingerprint",
                AuthorizationDecision.UNKNOWN, AuthorizationDecision.UNKNOWN,
                List.of("e-batch-a", "e-batch-b", "e-batch-c", "e-batch-z", "e-indirect-a", "e-indirect-b"));
        return new Observation(
                SOURCE_OBSERVATION,
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
                fingerprint,
                fingerprint,
                fingerprint.evidenceIds(),
                1.0,
                new ExecutionFingerprint(
                        EXECUTION, TEST, "request-s10", "response-s10",
                        "configuration-s10", "environment-s10"),
                Instant.parse("2026-09-24T13:30:00Z"));
    }

    private static ResponseSnapshot responseSnapshot() {
        HttpResponse response = new HttpResponse(
                200, List.of(), "{}".getBytes(), "application/json",
                HttpProtocol.HTTP_1_1, new byte[0]);
        return new ResponseSnapshot(
                "response-s10", "request-s10", response, Map.of(), Duration.ZERO,
                Instant.parse("2026-09-24T13:30:00Z"),
                new ResponseSemanticAnalyzer().fingerprint(response), "");
    }

    private static AuthorizationContext context() {
        Principal principal = new Principal("user-a", "User A", AuthenticationType.UNKNOWN, Confidence.unknown());
        Role role = new Role("viewer", "viewer", EvidenceSource.UNKNOWN, Confidence.unknown());
        Tenant tenant = new Tenant("tenant-a", "Tenant A", EvidenceSource.UNKNOWN, Confidence.unknown());
        Resource resource = new Resource(
                "resource-a", "document", null, "user-a", "tenant-a", "ACTIVE", Confidence.unknown());
        Action action = new Action(ActionType.READ, EvidenceSource.UNKNOWN, Confidence.unknown(), "READ");
        return new AuthorizationContext(
                principal, role, tenant, resource, "user-a", action,
                new WorkflowState("ACTIVE", Confidence.unknown()),
                AuthorizationDecision.UNKNOWN, AuthorizationDecision.UNKNOWN,
                List.of(), ContextStatus.RESOLVED);
    }
}
