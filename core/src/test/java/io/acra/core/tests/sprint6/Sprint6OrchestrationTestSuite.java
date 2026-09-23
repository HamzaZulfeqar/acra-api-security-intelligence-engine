package io.acra.core.tests.sprint6;

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
import io.acra.core.domain.authorization.AuthorizationPolicySnapshot;
import io.acra.core.domain.authorization.AuthorizationScope;
import io.acra.core.domain.authorization.ContextStatus;
import io.acra.core.domain.authorization.EffectiveAuthorizationMatrix;
import io.acra.core.domain.authorization.EffectiveAuthorizationRequest;
import io.acra.core.domain.authorization.Permission;
import io.acra.core.domain.authorization.RbacAssessmentState;
import io.acra.core.domain.authorization.RoleAssignment;
import io.acra.core.domain.authorization.RolePermissionAssignment;
import io.acra.core.domain.authorization.S6AuthorizationAnalysisResult;
import io.acra.core.domain.authorization.TenantIsolationAssessmentState;
import io.acra.core.domain.authorization.TenantMembership;
import io.acra.core.domain.authorization.TenantMembershipType;
import io.acra.core.domain.common.Confidence;
import io.acra.core.domain.evidence.EvidenceSource;
import io.acra.core.domain.finding.AuthorizationImpactProfile;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.http.HttpProtocol;
import io.acra.core.domain.http.HttpResponse;
import io.acra.core.domain.identity.AuthenticationType;
import io.acra.core.domain.identity.Principal;
import io.acra.core.domain.identity.Role;
import io.acra.core.domain.resource.Resource;
import io.acra.core.domain.tenant.Tenant;
import io.acra.core.domain.workflow.WorkflowState;
import io.acra.core.engine.AuthorizationAnalysisRequest;
import io.acra.core.engine.PolicyValidationEvaluator;
import io.acra.core.engine.S6AuthorizationAnalysisRequest;
import io.acra.core.engine.S6AuthorizationOrchestrator;
import io.acra.core.recon.SecurityContextFingerprint;
import io.acra.core.serialization.DomainSerializer;
import io.acra.core.tests.TestSupport;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class Sprint6OrchestrationTestSuite {
    private static final String PROJECT = "s6-project";
    private static final String EXECUTION = "s6-execution";
    private static final String TEST = "s6-test";
    private static final String OBSERVATION = "s6-observation";
    private static final String EVIDENCE = "s6-evidence";
    private static final Instant NOW = Instant.parse("2026-09-23T14:40:00Z");

    private Sprint6OrchestrationTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT6_ORCHESTRATION PASS assertions=" + assertions);
    }

    public static int run() {
        int assertions = 0;
        ExecutionEvidenceStore store = store();
        S6AuthorizationOrchestrator orchestrator = new S6AuthorizationOrchestrator(store);
        S6AuthorizationAnalysisResult result = orchestrator.analyze(request());

        TestSupport.assertEquals(AuthorizationDecision.DENY, result.effectiveResolution().expectedDecision(),
                "effective policy should resolve cross-tenant privileged action to deny");
        assertions++;
        TestSupport.assertEquals(TenantIsolationAssessmentState.CANDIDATE, result.tenantIsolation().state(),
                "cross-tenant observed allow should feed tenant candidate");
        assertions++;
        TestSupport.assertEquals(RbacAssessmentState.CANDIDATE, result.rbac().state(),
                "effective RBAC deny observed allow should feed RBAC candidate");
        assertions++;
        TestSupport.assertEquals(FindingCandidateState.CANDIDATE, result.findingCandidate().state(),
                "S6 composes into existing FindingCandidate architecture");
        assertions++;
        TestSupport.assertTrue(result.findingCandidate().dimensions().contains("TENANT_ISOLATION"),
                "tenant dimension should be carried into finding candidate");
        assertions++;
        TestSupport.assertTrue(result.findingCandidate().dimensions().contains("RBAC"),
                "RBAC dimension should be carried into finding candidate");
        assertions++;
        TestSupport.assertTrue(result.findingCandidate().dimensions().contains("ROLE_ESCALATION"),
                "explicit privileged action should carry role escalation dimension");
        assertions++;
        TestSupport.assertTrue(result.coverage().ratio() >= 0.8,
                "policy coverage should reflect a mostly resolved explicit policy fixture");
        assertions++;

        EffectiveAuthorizationMatrix matrix = new EffectiveAuthorizationMatrix();
        matrix.put(result.matrixEntry());
        matrix.put(result.matrixEntry());
        TestSupport.assertEquals(1, matrix.size(), "effective matrix should deterministically deduplicate same key");
        assertions++;

        String json = new DomainSerializer().serialize(result);
        TestSupport.assertNotContains(json, "DummyPassword", "S6 orchestration serialization remains secret-safe");
        assertions++;
        return assertions;
    }

    private static S6AuthorizationAnalysisRequest request() {
        AuthorizationContext context = context();
        AuthorizationAnalysisRequest s5 = new AuthorizationAnalysisRequest(context, "/admin/export", "s6-policy",
                PROJECT, OBSERVATION, EXECUTION, TEST,
                new PolicyValidationEvaluator.TenantPolicy("tenant-policy", "source", "tenant-a", "tenant-b",
                        "viewer", false, false, AuthorizationDecision.DENY, List.of(EVIDENCE)),
                null, "", "", false, false, null, "",
                PolicyValidationEvaluator.PropertyOperation.READ,
                new AuthorizationImpactProfile(true, true, false, true, true, false,
                        List.of("explicit-s6-lab-impact")));
        EffectiveAuthorizationRequest effective = new EffectiveAuthorizationRequest("user-a", "tenant-a",
                "tenant-b", "report-b", "report", "/admin/export", "", "ADMIN_EXPORT",
                AuthorizationDecision.ALLOW, false, NOW);
        return new S6AuthorizationAnalysisRequest(s5, policy(), effective, true, "tenant-admin");
    }

    private static AuthorizationPolicySnapshot policy() {
        return AuthorizationPolicySnapshot.create("s6-policy", "1", "password=DummyPassword",
                List.of(new TenantMembership("tm-a", "user-a", "tenant-a", TenantMembershipType.DIRECT,
                        true, List.of(EVIDENCE))),
                List.of(new RoleAssignment("ra-viewer", "user-a", "viewer", "tenant-a",
                        AuthorizationScope.tenant("tenant-a"), true, List.of(EVIDENCE))),
                List.of(),
                List.of(new Permission("p-read", "READ_REPORT", "report", "", "",
                        AuthorizationScope.tenant("tenant-a"), List.of(EVIDENCE))),
                List.of(new RolePermissionAssignment("rpa-read", "viewer", "p-read", "tenant-a",
                        List.of(EVIDENCE))),
                List.of(), List.of(), List.of(EVIDENCE), AuthorizationDecision.DENY, NOW);
    }

    private static AuthorizationContext context() {
        Principal principal = new Principal("user-a", "User A", AuthenticationType.UNKNOWN, Confidence.unknown());
        Role role = new Role("viewer", "viewer", EvidenceSource.UNKNOWN, Confidence.unknown());
        Tenant tenant = new Tenant("tenant-a", "Tenant A", EvidenceSource.UNKNOWN, Confidence.unknown());
        Resource resource = new Resource("report-b", "report", null, "user-b", "tenant-b", "ACTIVE",
                Confidence.unknown());
        Action action = new Action(ActionType.EXPORT, EvidenceSource.UNKNOWN, Confidence.unknown(), "ADMIN_EXPORT");
        return new AuthorizationContext(principal, role, tenant, resource, "user-b", action,
                new WorkflowState("ACTIVE", Confidence.unknown()), AuthorizationDecision.DENY,
                AuthorizationDecision.ALLOW, List.of(EVIDENCE), ContextStatus.RESOLVED);
    }

    private static ExecutionEvidenceStore store() {
        ExecutionEvidenceStore store = new ExecutionEvidenceStore(PROJECT);
        store.append(EXECUTION, TEST, EvidenceStage.TEST, EVIDENCE, "s6-policy");
        store.append(EXECUTION, TEST, EvidenceStage.OBSERVATION, OBSERVATION, observation());
        return store;
    }

    private static Observation observation() {
        ResponseSnapshot response = response();
        return new Observation(OBSERVATION, TEST, response, response, response, response,
                new ExpectedDecisionResolution(AuthorizationDecision.DENY,
                        ExpectedDecisionSource.EXPLICIT_CONFIGURED_POLICY, "s6-policy",
                        List.of(EVIDENCE), 1.0, false),
                AuthorizationOutcome.ALLOW,
                new MultiWayDifferential(AuthorizationOutcome.ALLOW, AuthorizationOutcome.ALLOW,
                        AuthorizationOutcome.DENY, AuthorizationOutcome.ALLOW, List.of(),
                        DifferentialClassification.UNEXPECTED_CHANGE, List.of()),
                securityContext(), securityContext(), List.of(EVIDENCE), 1.0,
                new ExecutionFingerprint(EXECUTION, TEST, "req", "res", "cfg", "env"), NOW);
    }

    private static ResponseSnapshot response() {
        HttpResponse response = new HttpResponse(200, List.of(), "{}".getBytes(), "application/json",
                HttpProtocol.HTTP_1_1, new byte[0]);
        return new ResponseSnapshot("s6-response", "s6-request", response, Map.of(), Duration.ZERO, NOW,
                new ResponseSemanticAnalyzer().fingerprint(response), "");
    }

    private static SecurityContextFingerprint securityContext() {
        return new SecurityContextFingerprint("user-a", "viewer", "tenant-a", "report-b", "user-b",
                "ADMIN_EXPORT", "ACTIVE", "POST", "/admin/export", "token-fingerprint",
                AuthorizationDecision.DENY, AuthorizationDecision.ALLOW, List.of(EVIDENCE));
    }
}
