package io.acra.core.tests.sprint9;

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
import io.acra.core.engine.PolicyValidationEvaluator;
import io.acra.core.property.PropertyAccessObservation;
import io.acra.core.property.S9PropertyAuthorizationAnalysis;
import io.acra.core.property.S9PropertyAuthorizationAnalyzer;
import io.acra.core.recon.SecurityContextFingerprint;
import io.acra.core.tests.TestSupport;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class Sprint9PropertyAuthorizationFoundationTestSuite {
    private static final String PROJECT_ID = "project-s9";
    private static final String EXECUTION_ID = "execution-s9";
    private static final String TEST_ID = "test-s9";

    private Sprint9PropertyAuthorizationFoundationTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT9_PROPERTY_AUTHORIZATION_FOUNDATION PASS assertions=" + assertions);
    }

    public static int run() {
        ExecutionEvidenceStore store = new ExecutionEvidenceStore(PROJECT_ID);
        addEvidence(store, "e-admin", "obs-admin", AuthorizationDecision.ALLOW);
        addEvidence(store, "e-ssn", "obs-ssn", AuthorizationDecision.DENY);
        addEvidence(store, "e-nickname", "obs-nickname", AuthorizationDecision.ALLOW);
        addEvidence(store, "e-ambiguous", "obs-ambiguous", AuthorizationDecision.ALLOW);

        S9PropertyAuthorizationAnalyzer analyzer = new S9PropertyAuthorizationAnalyzer(store);
        AuthorizationContext context = context();
        int assertions = 0;

        PropertyAccessObservation admin = propertyObservation(
                "obs-admin", "/api/v1/users/me", "is_admin",
                PolicyValidationEvaluator.PropertyOperation.UPDATE,
                AuthorizationDecision.ALLOW, "e-admin");
        PolicyValidationEvaluator.PropertyPolicy adminPolicy = policy(
                "policy-admin-update", "/api/v1/users/me", "is_admin",
                PolicyValidationEvaluator.PropertyOperation.UPDATE,
                AuthorizationDecision.DENY, "e-admin");

        S9PropertyAuthorizationAnalysis candidate = analyzer.analyze(
                context, List.of(adminPolicy), List.of(admin), PROJECT_ID);
        TestSupport.assertEquals(1L, candidate.candidateCount(),
                "explicit DENY plus observed ALLOW produces one property candidate");
        assertions++;
        TestSupport.assertTrue(candidate.assessments().getFirst().violationCandidate(),
                "property mismatch remains a review candidate");
        assertions++;
        TestSupport.assertEquals(AuthorizationDecision.DENY,
                candidate.assessments().getFirst().expectedDecision(),
                "property policy supplies the property-specific expected decision");
        assertions++;
        TestSupport.assertEquals(AuthorizationDecision.ALLOW,
                candidate.assessments().getFirst().observedDecision(),
                "property observation retains the observed decision");
        assertions++;
        TestSupport.assertTrue(candidate.complete(),
                "candidate analysis can be complete without becoming a confirmed finding");
        assertions++;

        PropertyAccessObservation ssn = propertyObservation(
                "obs-ssn", "/api/v1/users/me", "ssn",
                PolicyValidationEvaluator.PropertyOperation.READ,
                AuthorizationDecision.DENY, "e-ssn");
        PolicyValidationEvaluator.PropertyPolicy ssnPolicy = policy(
                "policy-ssn-read", "/api/v1/users/me", "ssn",
                PolicyValidationEvaluator.PropertyOperation.READ,
                AuthorizationDecision.DENY, "e-ssn");
        S9PropertyAuthorizationAnalysis secure = analyzer.analyze(
                context, List.of(ssnPolicy), List.of(ssn), PROJECT_ID);
        TestSupport.assertEquals(0L, secure.candidateCount(),
                "expected DENY plus observed DENY is not a property candidate");
        assertions++;
        TestSupport.assertEquals(PolicyValidationState.DENIED,
                secure.assessments().getFirst().state(),
                "secure denied property observation remains explicitly denied");
        assertions++;

        PropertyAccessObservation nickname = propertyObservation(
                "obs-nickname", "/api/v1/users/me", "nickname",
                PolicyValidationEvaluator.PropertyOperation.READ,
                AuthorizationDecision.ALLOW, "e-nickname");
        S9PropertyAuthorizationAnalysis noPolicy = analyzer.analyze(
                context, List.of(), List.of(nickname), PROJECT_ID);
        TestSupport.assertEquals(1L, noPolicy.inconclusiveCount(),
                "missing property policy fails closed as inconclusive");
        assertions++;
        TestSupport.assertContains(String.join(",", noPolicy.reasons()), "PROPERTY_POLICY_NOT_FOUND",
                "missing-policy reason remains explicit");
        assertions++;

        PropertyAccessObservation ambiguousObservation = propertyObservation(
                "obs-ambiguous", "/api/v1/users/me", "department",
                PolicyValidationEvaluator.PropertyOperation.READ,
                AuthorizationDecision.ALLOW, "e-ambiguous");
        PolicyValidationEvaluator.PropertyPolicy ambiguousDeny = policy(
                "policy-department-deny", "/api/v1/users/me", "department",
                PolicyValidationEvaluator.PropertyOperation.READ,
                AuthorizationDecision.DENY, "e-ambiguous");
        PolicyValidationEvaluator.PropertyPolicy ambiguousAllow = policy(
                "policy-department-allow", "/api/v1/users/me", "department",
                PolicyValidationEvaluator.PropertyOperation.READ,
                AuthorizationDecision.ALLOW, "e-ambiguous");
        S9PropertyAuthorizationAnalysis ambiguous = analyzer.analyze(
                context, List.of(ambiguousAllow, ambiguousDeny), List.of(ambiguousObservation), PROJECT_ID);
        TestSupport.assertEquals(1L, ambiguous.policyConflictCount(),
                "multiple applicable policies remain an explicit conflict");
        assertions++;
        TestSupport.assertEquals(0L, ambiguous.candidateCount(),
                "ambiguous policy cannot be promoted to a property candidate");
        assertions++;

        S9PropertyAuthorizationAnalysis crossProject = analyzer.analyze(
                context, List.of(adminPolicy), List.of(admin), "other-project");
        TestSupport.assertEquals(1L, crossProject.inconclusiveCount(),
                "cross-project evidence fails closed");
        assertions++;
        TestSupport.assertEquals(0L, crossProject.candidateCount(),
                "cross-project provenance cannot produce a candidate");
        assertions++;

        S9PropertyAuthorizationAnalysis deterministicA = analyzer.analyze(
                context,
                List.of(adminPolicy, ssnPolicy),
                List.of(admin, ssn),
                PROJECT_ID);
        S9PropertyAuthorizationAnalysis deterministicB = analyzer.analyze(
                context,
                List.of(ssnPolicy, adminPolicy),
                List.of(ssn, admin),
                PROJECT_ID);
        TestSupport.assertEquals(deterministicA.analysisId(), deterministicB.analysisId(),
                "analysis identifier is deterministic across input ordering");
        assertions++;

        expectFailure(() -> new PropertyAccessObservation(
                "obs-secret",
                EXECUTION_ID,
                TEST_ID,
                "/api/v1/users/me\nAuthorization: Bearer abcdefghijklmnopqrstuvwxyz",
                "profile",
                PolicyValidationEvaluator.PropertyOperation.READ,
                AuthorizationDecision.ALLOW,
                List.of("e-admin")),
                "secret-bearing observation metadata must be rejected");
        assertions++;

        return assertions;
    }

    private static PropertyAccessObservation propertyObservation(
            String observationId,
            String endpoint,
            String property,
            PolicyValidationEvaluator.PropertyOperation operation,
            AuthorizationDecision observed,
            String evidenceId) {
        return new PropertyAccessObservation(
                observationId, EXECUTION_ID, TEST_ID, endpoint, property, operation, observed, List.of(evidenceId));
    }

    private static PolicyValidationEvaluator.PropertyPolicy policy(
            String reference,
            String endpoint,
            String property,
            PolicyValidationEvaluator.PropertyOperation operation,
            AuthorizationDecision expected,
            String evidenceId) {
        return new PolicyValidationEvaluator.PropertyPolicy(
                reference,
                "explicit-s9-fixture",
                endpoint,
                property,
                operation,
                "viewer",
                "tenant-a",
                expected,
                List.of(evidenceId));
    }

    private static AuthorizationContext context() {
        Principal principal = new Principal("user-a", "User A", AuthenticationType.UNKNOWN, Confidence.unknown());
        Role role = new Role("viewer", "viewer", EvidenceSource.UNKNOWN, Confidence.unknown());
        Tenant tenant = new Tenant("tenant-a", "Tenant A", EvidenceSource.UNKNOWN, Confidence.unknown());
        Resource resource = new Resource(
                "user-a", "user-profile", null, "user-a", "tenant-a", "ACTIVE", Confidence.unknown());
        Action action = new Action(ActionType.UPDATE, EvidenceSource.UNKNOWN, Confidence.unknown(), "PROFILE_UPDATE");
        return new AuthorizationContext(
                principal,
                role,
                tenant,
                resource,
                "user-a",
                action,
                new WorkflowState("ACTIVE", Confidence.unknown()),
                AuthorizationDecision.ALLOW,
                AuthorizationDecision.ALLOW,
                List.of(),
                ContextStatus.RESOLVED);
    }

    private static void addEvidence(
            ExecutionEvidenceStore store,
            String evidenceId,
            String observationId,
            AuthorizationDecision observed) {
        store.append(EXECUTION_ID, TEST_ID, EvidenceStage.TEST, evidenceId, "s9-property-evidence");
        store.append(EXECUTION_ID, TEST_ID, EvidenceStage.OBSERVATION, observationId,
                observation(observationId, evidenceId, observed));
    }

    private static Observation observation(String observationId, String evidenceId, AuthorizationDecision observed) {
        ResponseSnapshot response = responseSnapshot(observationId);
        AuthorizationOutcome outcome = observed == AuthorizationDecision.ALLOW
                ? AuthorizationOutcome.ALLOW : AuthorizationOutcome.DENY;
        return new Observation(
                observationId,
                TEST_ID,
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
                outcome,
                new MultiWayDifferential(
                        outcome,
                        outcome,
                        outcome,
                        outcome,
                        List.of(),
                        DifferentialClassification.NO_CHANGE,
                        List.of()),
                securityContext(evidenceId, observed),
                securityContext(evidenceId, observed),
                List.of(evidenceId),
                1.0,
                new ExecutionFingerprint(
                        EXECUTION_ID,
                        TEST_ID,
                        "request-" + observationId,
                        "response-" + observationId,
                        "configuration-s9",
                        "environment-s9"),
                Instant.parse("2026-09-24T00:00:00Z"));
    }

    private static ResponseSnapshot responseSnapshot(String suffix) {
        HttpResponse response = new HttpResponse(
                200, List.of(), "{}".getBytes(), "application/json", HttpProtocol.HTTP_1_1, new byte[0]);
        return new ResponseSnapshot(
                "response-" + suffix,
                "request-" + suffix,
                response,
                Map.of(),
                Duration.ZERO,
                Instant.parse("2026-09-24T00:00:00Z"),
                new ResponseSemanticAnalyzer().fingerprint(response),
                "");
    }

    private static SecurityContextFingerprint securityContext(String evidenceId, AuthorizationDecision observed) {
        return new SecurityContextFingerprint(
                "user-a",
                "viewer",
                "tenant-a",
                "user-a",
                "user-a",
                "PROFILE_UPDATE",
                "ACTIVE",
                "PATCH",
                "/api/v1/users/me",
                "token-fingerprint",
                AuthorizationDecision.UNKNOWN,
                observed,
                List.of(evidenceId));
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
