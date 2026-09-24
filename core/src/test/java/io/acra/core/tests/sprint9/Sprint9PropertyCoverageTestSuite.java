package io.acra.core.tests.sprint9;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.PolicyValidationState;
import io.acra.core.domain.authorization.PropertyAuthorizationAssessment;
import io.acra.core.domain.finding.FindingCandidate;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.engine.PolicyValidationEvaluator;
import io.acra.core.property.PropertyAccessObservation;
import io.acra.core.property.PropertyCoverageDisposition;
import io.acra.core.property.S9PropertyCoverageTracker;
import io.acra.core.tests.TestSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Sprint9PropertyCoverageTestSuite {
    private static final String ENDPOINT = "/api/v1/s9/users/user-a/profile";

    private Sprint9PropertyCoverageTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT9_PROPERTY_COVERAGE PASS assertions=" + assertions);
    }

    public static int run() {
        int assertions = 0;
        S9PropertyCoverageTracker tracker = new S9PropertyCoverageTracker();

        var displayRead = policy(
                "policy-display-read", "display_name",
                PolicyValidationEvaluator.PropertyOperation.READ, AuthorizationDecision.ALLOW);
        var salaryRead = policy(
                "policy-salary-read", "salary_band",
                PolicyValidationEvaluator.PropertyOperation.READ, AuthorizationDecision.DENY);
        var displayUpdate = policy(
                "policy-display-update", "display_name",
                PolicyValidationEvaluator.PropertyOperation.UPDATE, AuthorizationDecision.ALLOW);
        var adminUpdate = policy(
                "policy-admin-update", "is_admin",
                PolicyValidationEvaluator.PropertyOperation.UPDATE, AuthorizationDecision.DENY);

        tracker.recordPolicy(displayRead);
        tracker.recordPolicy(salaryRead);
        tracker.recordPolicy(displayUpdate);
        tracker.recordPolicy(adminUpdate);
        tracker.recordPolicy(displayRead);

        TestSupport.assertEquals(4, tracker.size(),
                "duplicate policy registration must not duplicate property coverage context");
        assertions++;

        PropertyAccessObservation salaryObservation = observation(
                "obs-salary", "salary_band",
                PolicyValidationEvaluator.PropertyOperation.READ, AuthorizationDecision.DENY);
        tracker.recordObservation(salaryRead, salaryObservation);
        var salaryAssessment = assessment(
                "assessment-salary", salaryRead, AuthorizationDecision.DENY,
                PolicyValidationState.DENIED);
        tracker.recordAssessment(
                salaryRead, salaryObservation, salaryAssessment,
                finding("finding-salary", salaryAssessment, FindingCandidateState.REJECTED));

        PropertyAccessObservation adminObservation = observation(
                "obs-admin", "is_admin",
                PolicyValidationEvaluator.PropertyOperation.UPDATE, AuthorizationDecision.ALLOW);
        tracker.recordObservation(adminUpdate, adminObservation);
        var adminAssessment = assessment(
                "assessment-admin", adminUpdate, AuthorizationDecision.ALLOW,
                PolicyValidationState.CONFLICTING);
        tracker.recordAssessment(
                adminUpdate, adminObservation, adminAssessment,
                finding("finding-admin", adminAssessment, FindingCandidateState.CANDIDATE));

        PropertyAccessObservation displayObservation = observation(
                "obs-display", "display_name",
                PolicyValidationEvaluator.PropertyOperation.READ, AuthorizationDecision.ALLOW);
        tracker.recordObservation(displayRead, displayObservation);

        var first = tracker.summary();
        TestSupport.assertEquals(4, first.totalPolicyContexts(),
                "coverage denominator is the explicit property-policy universe");
        assertions++;
        TestSupport.assertEquals(2, first.readPolicyContexts(),
                "READ property-policy contexts remain distinct");
        assertions++;
        TestSupport.assertEquals(2, first.updatePolicyContexts(),
                "UPDATE property-policy contexts remain distinct");
        assertions++;
        TestSupport.assertEquals(3, first.observedContexts(),
                "three property-policy contexts have observations");
        assertions++;
        TestSupport.assertEquals(2, first.assessedContexts(),
                "only two observed contexts have completed assessment/finding projection");
        assertions++;
        TestSupport.assertEquals(1, first.candidateContexts(),
                "one assessed context is a review candidate");
        assertions++;
        TestSupport.assertEquals(1, first.rejectedContexts(),
                "one assessed secure control is rejected");
        assertions++;
        TestSupport.assertEquals(0, first.inconclusiveContexts(),
                "no assessed context is inconclusive yet");
        assertions++;
        TestSupport.assertEquals(1, first.unobservedContexts(),
                "unobserved property-policy context remains explicitly visible");
        assertions++;
        TestSupport.assertEquals(1, first.observedUnassessedContexts(),
                "observed but unassessed context remains explicitly visible");
        assertions++;
        TestSupport.assertEquals(0.75, first.observationRatio(),
                "observation ratio uses all explicit policy contexts as denominator");
        assertions++;
        TestSupport.assertEquals(0.5, first.assessmentRatio(),
                "assessment ratio does not treat unassessed observations as complete");
        assertions++;

        var displayAssessment = assessment(
                "assessment-display", displayRead, AuthorizationDecision.ALLOW,
                PolicyValidationState.INCONCLUSIVE);
        tracker.recordAssessment(
                displayRead, displayObservation, displayAssessment,
                finding("finding-display", displayAssessment, FindingCandidateState.INCONCLUSIVE));

        var second = tracker.summary();
        TestSupport.assertEquals(3, second.assessedContexts(),
                "adding inconclusive review increases assessed coverage without claiming security");
        assertions++;
        TestSupport.assertEquals(1, second.inconclusiveContexts(),
                "inconclusive property context remains separately accounted");
        assertions++;
        TestSupport.assertEquals(0, second.observedUnassessedContexts(),
                "all observed contexts are now assessed");
        assertions++;
        TestSupport.assertEquals(1, tracker.inconclusiveCoverageIds().size(),
                "inconclusive coverage ID remains discoverable");
        assertions++;
        TestSupport.assertEquals(1, tracker.unobservedCoverageIds().size(),
                "untested display_name UPDATE policy remains discoverable");
        assertions++;
        TestSupport.assertEquals(PropertyCoverageDisposition.UNOBSERVED,
                tracker.entries().stream()
                        .filter(entry -> entry.policyReference().equals("policy-display-update"))
                        .findFirst().orElseThrow().disposition(),
                "missing execution evidence must never be converted into a secure result");
        assertions++;

        List<String> ids = tracker.entries().stream().map(entry -> entry.coverageId()).toList();
        List<String> sorted = new ArrayList<>(ids);
        Collections.sort(sorted);
        TestSupport.assertEquals(sorted, ids,
                "property coverage matrix ordering must be deterministic");
        assertions++;

        expectFailure(() -> tracker.recordObservation(
                adminUpdate,
                observation("obs-wrong", "is_admin",
                        PolicyValidationEvaluator.PropertyOperation.READ, AuthorizationDecision.ALLOW)),
                "operation-mismatched observation must be rejected");
        assertions++;

        return assertions;
    }

    private static PolicyValidationEvaluator.PropertyPolicy policy(
            String reference,
            String property,
            PolicyValidationEvaluator.PropertyOperation operation,
            AuthorizationDecision expected) {
        return new PolicyValidationEvaluator.PropertyPolicy(
                reference,
                "GT-S9-PROPERTY-AUTHORIZATION",
                ENDPOINT,
                property,
                operation,
                "viewer",
                "tenant-a",
                expected,
                List.of("evidence-" + reference));
    }

    private static PropertyAccessObservation observation(
            String observationId,
            String property,
            PolicyValidationEvaluator.PropertyOperation operation,
            AuthorizationDecision observed) {
        return new PropertyAccessObservation(
                observationId,
                "execution-coverage",
                "test-coverage",
                ENDPOINT,
                property,
                operation,
                observed,
                List.of("evidence-" + observationId));
    }

    private static PropertyAuthorizationAssessment assessment(
            String assessmentId,
            PolicyValidationEvaluator.PropertyPolicy policy,
            AuthorizationDecision observed,
            PolicyValidationState state) {
        return new PropertyAuthorizationAssessment(
                assessmentId,
                policy.endpoint(),
                policy.property(),
                policy.operation().name(),
                policy.roleId(),
                policy.tenantId(),
                policy.policyReference(),
                policy.expectedDecision(),
                observed,
                state,
                state == PolicyValidationState.INCONCLUSIVE ? "INSUFFICIENT" : "HIGH",
                List.of("evidence-" + assessmentId),
                "coverage fixture",
                state == PolicyValidationState.INCONCLUSIVE
                        ? List.of("PROPERTY_CONTEXT_INCOMPLETE") : List.of());
    }

    private static FindingCandidate finding(
            String candidateId,
            PropertyAuthorizationAssessment assessment,
            FindingCandidateState state) {
        return new FindingCandidate(
                candidateId,
                state,
                "acra-s9",
                List.of("test-coverage"),
                List.of("execution-coverage"),
                List.of("obs-coverage"),
                List.of(assessment.assessmentId()),
                List.of("PROPERTY", "PROPERTY_" + assessment.operation()),
                assessment.endpoint(),
                "user-a",
                "user-a",
                "tenant-a",
                assessment.expectedDecision(),
                assessment.observedDecision(),
                assessment.evidenceIds(),
                List.of(),
                List.of(assessment.policyReference()),
                state == FindingCandidateState.INCONCLUSIVE ? "INSUFFICIENT" : "HIGH",
                "coverage fixture; candidate is not an automatically confirmed vulnerability",
                FindingFingerprint.of(
                        assessment.endpoint(),
                        "user-a#" + assessment.property(),
                        "user-a",
                        "tenant-a",
                        "PROPERTY_" + assessment.operation(),
                        state.name()));
    }

    private static void expectFailure(Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }
}
