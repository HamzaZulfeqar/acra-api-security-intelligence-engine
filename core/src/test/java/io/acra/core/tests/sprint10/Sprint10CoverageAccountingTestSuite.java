package io.acra.core.tests.sprint10;

import io.acra.core.batch.BatchItemAuthorizationAssessment;
import io.acra.core.batch.BatchItemObservation;
import io.acra.core.batch.BatchItemPolicy;
import io.acra.core.coverage.S10AuthorizationCoverageTracker;
import io.acra.core.coverage.S10CoverageDisposition;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.PolicyValidationState;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.reference.IndirectReferenceAuthorizationAssessment;
import io.acra.core.reference.IndirectReferencePolicy;
import io.acra.core.reference.IndirectReferenceResolution;
import io.acra.core.reference.IndirectReferenceSource;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.tests.TestSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Sprint10CoverageAccountingTestSuite {
    private static final String BATCH_ENDPOINT = "/api/v1/s10/documents/batch-read";
    private static final String INDIRECT_ENDPOINT = "/api/v1/s10/share/{alias}";

    private Sprint10CoverageAccountingTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT10_COVERAGE_ACCOUNTING PASS assertions=" + assertions);
    }

    public static int run() {
        int assertions = 0;
        S10AuthorizationCoverageTracker tracker = new S10AuthorizationCoverageTracker();

        BatchItemPolicy batchOwned = batchPolicy(
                "batch-policy-a", "resource-a", AuthorizationDecision.ALLOW);
        BatchItemPolicy batchForeign = batchPolicy(
                "batch-policy-b", "resource-b", AuthorizationDecision.DENY);
        IndirectReferencePolicy indirectOwned = indirectPolicy(
                "indirect-policy-a", "resource-a", AuthorizationDecision.ALLOW);
        IndirectReferencePolicy indirectForeign = indirectPolicy(
                "indirect-policy-b", "resource-b", AuthorizationDecision.DENY);

        tracker.recordPolicy(batchOwned);
        tracker.recordPolicy(batchForeign);
        tracker.recordPolicy(indirectOwned);
        tracker.recordPolicy(indirectForeign);
        tracker.recordPolicy(batchOwned);

        TestSupport.assertEquals(4, tracker.size(),
                "duplicate registration must not duplicate the explicit S10 policy universe");
        assertions++;

        BatchItemObservation batchForeignObservation = batchObservation(
                "obs-batch-foreign", "item-b", "resource-b", AuthorizationDecision.ALLOW);
        tracker.recordObservation(batchForeign, batchForeignObservation);
        BatchItemAuthorizationAssessment batchForeignAssessment = batchAssessment(
                "assessment-batch-foreign", batchForeign, AuthorizationDecision.ALLOW,
                PolicyValidationState.CONFLICTING);
        tracker.recordAssessment(
                batchForeign,
                batchForeignObservation,
                batchForeignAssessment,
                finding("finding-batch-foreign", batchForeignAssessment.assessmentId(),
                        "BATCH_AUTHORIZATION", batchForeign.endpoint(), "resource-b",
                        batchForeignAssessment.expectedDecision(), batchForeignAssessment.observedDecision(),
                        batchForeign.policyReference(), FindingCandidateState.CANDIDATE));

        IndirectReferenceResolution indirectForeignObservation = indirectResolution(
                "resolution-indirect-foreign", "share-b", "resource-b", AuthorizationDecision.DENY);
        tracker.recordObservation(indirectForeign, indirectForeignObservation);
        IndirectReferenceAuthorizationAssessment indirectForeignAssessment = indirectAssessment(
                "assessment-indirect-foreign", indirectForeign, AuthorizationDecision.DENY,
                PolicyValidationState.DENIED);
        tracker.recordAssessment(
                indirectForeign,
                indirectForeignObservation,
                indirectForeignAssessment,
                finding("finding-indirect-foreign", indirectForeignAssessment.assessmentId(),
                        "INDIRECT_REFERENCE_AUTHORIZATION", indirectForeign.endpoint(), "resource-b",
                        indirectForeignAssessment.expectedDecision(), indirectForeignAssessment.observedDecision(),
                        indirectForeign.policyReference(), FindingCandidateState.REJECTED));

        BatchItemObservation batchOwnedObservation = batchObservation(
                "obs-batch-owned", "item-a", "resource-a", AuthorizationDecision.ALLOW);
        tracker.recordObservation(batchOwned, batchOwnedObservation);

        var first = tracker.summary();
        TestSupport.assertEquals(4, first.totalPolicyContexts(),
                "coverage denominator is the explicit batch/indirect policy universe");
        assertions++;
        TestSupport.assertEquals(2, first.batchPolicyContexts(),
                "batch-item policies remain distinct coverage contexts");
        assertions++;
        TestSupport.assertEquals(2, first.indirectPolicyContexts(),
                "indirect resolved-target policies remain distinct coverage contexts");
        assertions++;
        TestSupport.assertEquals(3, first.observedContexts(),
                "only explicit item/resolution observations count as observed coverage");
        assertions++;
        TestSupport.assertEquals(2, first.assessedContexts(),
                "only assessment plus finding projection counts as assessed coverage");
        assertions++;
        TestSupport.assertEquals(1, first.candidateContexts(),
                "one verified batch mismatch is a candidate coverage context");
        assertions++;
        TestSupport.assertEquals(1, first.rejectedContexts(),
                "one verified secure indirect control is rejected");
        assertions++;
        TestSupport.assertEquals(0, first.inconclusiveContexts(),
                "no assessed context is inconclusive yet");
        assertions++;
        TestSupport.assertEquals(1, first.unobservedContexts(),
                "unobserved indirect-owned policy remains visible");
        assertions++;
        TestSupport.assertEquals(1, first.observedUnassessedContexts(),
                "observed batch-owned policy is not silently treated as assessed");
        assertions++;
        TestSupport.assertEquals(0.75, first.observationRatio(),
                "observation ratio uses explicit policy contexts as denominator");
        assertions++;
        TestSupport.assertEquals(0.5, first.assessmentRatio(),
                "assessment ratio does not infer completion from observation alone");
        assertions++;

        BatchItemAuthorizationAssessment batchOwnedAssessment = batchAssessment(
                "assessment-batch-owned", batchOwned, AuthorizationDecision.ALLOW,
                PolicyValidationState.INCONCLUSIVE);
        tracker.recordAssessment(
                batchOwned,
                batchOwnedObservation,
                batchOwnedAssessment,
                finding("finding-batch-owned", batchOwnedAssessment.assessmentId(),
                        "BATCH_AUTHORIZATION", batchOwned.endpoint(), "resource-a",
                        batchOwnedAssessment.expectedDecision(), batchOwnedAssessment.observedDecision(),
                        batchOwned.policyReference(), FindingCandidateState.INCONCLUSIVE));

        var second = tracker.summary();
        TestSupport.assertEquals(3, second.assessedContexts(),
                "inconclusive review counts as assessed without claiming security");
        assertions++;
        TestSupport.assertEquals(1, second.inconclusiveContexts(),
                "inconclusive context remains separately accounted");
        assertions++;
        TestSupport.assertEquals(0, second.observedUnassessedContexts(),
                "all currently observed contexts are now assessed");
        assertions++;
        TestSupport.assertEquals(1, tracker.inconclusiveCoverageIds().size(),
                "inconclusive coverage ID remains discoverable");
        assertions++;
        TestSupport.assertEquals(1, tracker.unobservedCoverageIds().size(),
                "unobserved policy remains discoverable");
        assertions++;

        TestSupport.assertEquals(S10CoverageDisposition.UNOBSERVED,
                tracker.entries().stream()
                        .filter(entry -> entry.policyReference().equals("indirect-policy-a"))
                        .findFirst().orElseThrow().disposition(),
                "missing execution evidence never becomes a secure result");
        assertions++;

        List<String> ids = tracker.entries().stream()
                .map(entry -> entry.coverageId())
                .toList();
        List<String> sorted = new ArrayList<>(ids);
        Collections.sort(sorted);
        TestSupport.assertEquals(sorted, ids,
                "combined S10 coverage ordering is deterministic");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> tracker.recordObservation(
                        batchForeign,
                        batchObservation("obs-wrong", "item-x", "resource-a", AuthorizationDecision.ALLOW)),
                "resource-mismatched batch observation must be rejected");
        assertions++;

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> tracker.recordObservation(
                        indirectForeign,
                        indirectResolution("resolution-wrong", "share-b", "resource-a", AuthorizationDecision.DENY)),
                "resolved-target-mismatched indirect observation must be rejected");
        assertions++;

        return assertions;
    }

    private static BatchItemPolicy batchPolicy(
            String reference, String resourceId, AuthorizationDecision expected) {
        return new BatchItemPolicy(
                reference,
                "GT-S10-BATCH-INDIRECT-AUTHORIZATION",
                BATCH_ENDPOINT,
                resourceId,
                "READ",
                "viewer",
                "tenant-a",
                expected,
                List.of("evidence-" + reference));
    }

    private static IndirectReferencePolicy indirectPolicy(
            String reference, String resourceId, AuthorizationDecision expected) {
        return new IndirectReferencePolicy(
                reference,
                "GT-S10-BATCH-INDIRECT-AUTHORIZATION",
                INDIRECT_ENDPOINT,
                resourceId,
                "READ",
                "viewer",
                "tenant-a",
                expected,
                List.of("evidence-" + reference));
    }

    private static BatchItemObservation batchObservation(
            String observationId, String itemKey, String resourceId, AuthorizationDecision observed) {
        return new BatchItemObservation(
                observationId,
                "source-observation-" + observationId,
                "execution-coverage",
                "test-coverage",
                "batch-coverage",
                itemKey,
                BATCH_ENDPOINT,
                resourceId,
                "READ",
                observed,
                List.of("evidence-" + observationId));
    }

    private static BatchItemAuthorizationAssessment batchAssessment(
            String id,
            BatchItemPolicy policy,
            AuthorizationDecision observed,
            PolicyValidationState state) {
        return new BatchItemAuthorizationAssessment(
                id,
                "batch-coverage",
                policy.resourceId(),
                policy.endpoint(),
                policy.resourceId(),
                policy.action(),
                policy.policyReference(),
                policy.expectedDecision(),
                observed,
                state,
                state == PolicyValidationState.INCONCLUSIVE ? "INSUFFICIENT" : "HIGH",
                List.of("evidence-" + id),
                "coverage fixture",
                state == PolicyValidationState.INCONCLUSIVE
                        ? List.of("BATCH_CONTEXT_INCOMPLETE") : List.of());
    }

    private static IndirectReferenceResolution indirectResolution(
            String id, String alias, String resourceId, AuthorizationDecision observed) {
        return new IndirectReferenceResolution(
                id,
                "source-observation-" + id,
                "execution-coverage",
                "test-coverage",
                INDIRECT_ENDPOINT,
                TokenFingerprint.sha256(alias),
                "ALIAS",
                resourceId,
                "READ",
                observed,
                IndirectReferenceSource.OBSERVED,
                List.of("evidence-" + id));
    }

    private static IndirectReferenceAuthorizationAssessment indirectAssessment(
            String id,
            IndirectReferencePolicy policy,
            AuthorizationDecision observed,
            PolicyValidationState state) {
        return new IndirectReferenceAuthorizationAssessment(
                id,
                policy.endpoint(),
                TokenFingerprint.sha256("share-b"),
                "ALIAS",
                policy.resolvedResourceId(),
                policy.action(),
                policy.policyReference(),
                policy.expectedDecision(),
                observed,
                state,
                state == PolicyValidationState.INCONCLUSIVE ? "INSUFFICIENT" : "HIGH",
                List.of("evidence-" + id),
                "coverage fixture",
                state == PolicyValidationState.INCONCLUSIVE
                        ? List.of("INDIRECT_CONTEXT_INCOMPLETE") : List.of());
    }

    private static FindingCandidate finding(
            String candidateId,
            String assessmentId,
            String dimension,
            String endpoint,
            String resourceId,
            AuthorizationDecision expected,
            AuthorizationDecision observed,
            String policyReference,
            FindingCandidateState state) {
        return new FindingCandidate(
                candidateId,
                state,
                "acra-s10",
                List.of("test-coverage"),
                List.of("execution-coverage"),
                List.of("observation-coverage"),
                List.of(assessmentId),
                List.of(dimension),
                endpoint,
                resourceId,
                "user-a",
                "tenant-a",
                expected,
                observed,
                List.of("evidence-" + assessmentId),
                List.of(),
                List.of(policyReference),
                state == FindingCandidateState.INCONCLUSIVE ? "INSUFFICIENT" : "HIGH",
                "coverage fixture; candidate is not an automatically confirmed vulnerability",
                FindingFingerprint.of(
                        endpoint, resourceId, "user-a", "tenant-a", dimension, state.name()));
    }
}
