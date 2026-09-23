package io.acra.core.tests.sprint5;

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
import io.acra.core.engine.PropertyAuthorizationEvaluator;
import io.acra.core.engine.TenantAuthorizationEvaluator;
import io.acra.core.engine.WorkflowAuthorizationEvaluator;
import io.acra.core.recon.SecurityContextFingerprint;
import io.acra.core.tests.TestSupport;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class Sprint5AuthorizationDimensionTestSuite {
    private static final String PROJECT_ID = "project-a";
    private static final String EXECUTION_ID = "execution-1";
    private static final String TEST_ID = "test-1";
    private static final String OBSERVATION_ID = "observation-1";
    private static final String EVIDENCE_ID = "evidence-1";

    private Sprint5AuthorizationDimensionTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT5_AUTHORIZATION_DIMENSIONS PASS assertions=" + assertions);
    }

    public static int run() {
        ExecutionEvidenceStore store = new ExecutionEvidenceStore(PROJECT_ID);
        store.append(EXECUTION_ID, TEST_ID, EvidenceStage.TEST, EVIDENCE_ID, "policy-evidence");
        store.append(EXECUTION_ID, TEST_ID, EvidenceStage.OBSERVATION, OBSERVATION_ID, observation());
        PolicyValidationEvaluator policyEvaluator = new PolicyValidationEvaluator(new EvidenceReferenceValidator(store));
        int assertions = 0;

        AuthorizationContext allowContext = context(AuthorizationDecision.ALLOW, AuthorizationDecision.ALLOW);

        TenantAuthorizationEvaluator tenantEvaluator = new TenantAuthorizationEvaluator(policyEvaluator);
        var tenant = tenantEvaluator.evaluate(allowContext,
                new PolicyValidationEvaluator.TenantPolicy("tenant-policy", "fixture", "tenant-a", "tenant-a",
                        "viewer", false, false, AuthorizationDecision.ALLOW, List.of(EVIDENCE_ID)),
                OBSERVATION_ID, EXECUTION_ID, TEST_ID, PROJECT_ID);
        TestSupport.assertEquals(PolicyValidationState.ALLOWED, tenant.state(),
                "same-tenant explicit policy should be allowed");
        TestSupport.assertEquals("tenant-a", tenant.subjectTenantId(), "subject tenant retained");
        assertions += 2;

        WorkflowAuthorizationEvaluator workflowEvaluator = new WorkflowAuthorizationEvaluator(policyEvaluator);
        var workflow = workflowEvaluator.evaluate(allowContext,
                new PolicyValidationEvaluator.WorkflowPolicy("workflow-policy", "fixture", "DRAFT", "SUBMITTED",
                        "viewer", false, false, false, AuthorizationDecision.ALLOW, List.of(EVIDENCE_ID)),
                "DRAFT", "SUBMITTED", false, true,
                OBSERVATION_ID, EXECUTION_ID, TEST_ID, PROJECT_ID);
        TestSupport.assertEquals(PolicyValidationState.ALLOWED, workflow.state(),
                "valid workflow transition should be allowed");
        TestSupport.assertEquals("SUBMITTED", workflow.requestedState(), "requested state retained");
        assertions += 2;

        PropertyAuthorizationEvaluator propertyEvaluator = new PropertyAuthorizationEvaluator(policyEvaluator);
        var property = propertyEvaluator.evaluate(allowContext,
                new PolicyValidationEvaluator.PropertyPolicy("property-policy", "fixture",
                        "/documents/{id}", "title", PolicyValidationEvaluator.PropertyOperation.READ,
                        "viewer", "tenant-a", AuthorizationDecision.ALLOW, List.of(EVIDENCE_ID)),
                "title", PolicyValidationEvaluator.PropertyOperation.READ, "/documents/{id}",
                OBSERVATION_ID, EXECUTION_ID, TEST_ID, PROJECT_ID);
        TestSupport.assertEquals(PolicyValidationState.ALLOWED, property.state(),
                "valid property policy should be allowed");
        TestSupport.assertEquals("title", property.property(), "property retained");
        assertions += 2;

        var forged = tenantEvaluator.evaluate(allowContext,
                new PolicyValidationEvaluator.TenantPolicy("tenant-policy", "fixture", "tenant-a", "tenant-a",
                        "viewer", false, false, AuthorizationDecision.ALLOW, List.of(EVIDENCE_ID)),
                "forged-observation", EXECUTION_ID, TEST_ID, PROJECT_ID);
        TestSupport.assertEquals(PolicyValidationState.INCONCLUSIVE, forged.state(),
                "forged observation must fail closed through typed evaluator");
        assertions++;
        return assertions;
    }

    private static AuthorizationContext context(AuthorizationDecision expected, AuthorizationDecision observed) {
        Principal principal = new Principal("user-a", "User A", AuthenticationType.UNKNOWN, Confidence.unknown());
        Role role = new Role("viewer", "viewer", EvidenceSource.CONFIGURATION, Confidence.unknown());
        Tenant tenant = new Tenant("tenant-a", "tenant-a", EvidenceSource.CONFIGURATION, Confidence.unknown());
        Resource resource = new Resource("document-1", "document", null, "user-a", "tenant-a", "DRAFT",
                Confidence.unknown());
        Action action = new Action(ActionType.READ, EvidenceSource.CONFIGURATION, Confidence.unknown(), "READ");
        return new AuthorizationContext(principal, role, tenant, resource, "user-a", action,
                new WorkflowState("DRAFT", Confidence.unknown()), expected, observed, List.of(EVIDENCE_ID),
                ContextStatus.RESOLVED);
    }

    private static Observation observation() {
        ResponseSnapshot response = responseSnapshot();
        SecurityContextFingerprint fingerprint = new SecurityContextFingerprint(
                "user-a", "viewer", "tenant-a", "document-1", "user-a", "READ",
                "DRAFT", "GET", "/documents/{id}", "token-fingerprint",
                AuthorizationDecision.ALLOW, AuthorizationDecision.ALLOW, List.of(EVIDENCE_ID));
        return new Observation(OBSERVATION_ID, TEST_ID, response, response, response, response,
                new ExpectedDecisionResolution(AuthorizationDecision.ALLOW,
                        ExpectedDecisionSource.EXPLICIT_CONFIGURED_POLICY, "policy-1",
                        List.of(EVIDENCE_ID), 1.0, false), AuthorizationOutcome.ALLOW,
                new MultiWayDifferential(AuthorizationOutcome.ALLOW, AuthorizationOutcome.ALLOW,
                        AuthorizationOutcome.DENY, AuthorizationOutcome.ALLOW, List.of(),
                        DifferentialClassification.NO_CHANGE, List.of()),
                fingerprint, fingerprint, List.of(EVIDENCE_ID), 1.0,
                new ExecutionFingerprint(EXECUTION_ID, TEST_ID, "request-fingerprint",
                        "response-fingerprint", "configuration-fingerprint", "environment-fingerprint"),
                Instant.parse("2026-09-23T00:00:00Z"));
    }

    private static ResponseSnapshot responseSnapshot() {
        HttpResponse response = new HttpResponse(200, List.of(), "{}".getBytes(), "application/json",
                HttpProtocol.HTTP_1_1, new byte[0]);
        return new ResponseSnapshot("response-1", "request-1", response, Map.of(), Duration.ZERO,
                Instant.parse("2026-09-23T00:00:00Z"), new ResponseSemanticAnalyzer().fingerprint(response), "");
    }
}
