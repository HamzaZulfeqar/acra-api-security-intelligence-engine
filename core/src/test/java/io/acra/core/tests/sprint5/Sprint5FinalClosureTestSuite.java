package io.acra.core.tests.sprint5;

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
import io.acra.core.domain.authorization.AuthorizationAnalysisResult;
import io.acra.core.domain.authorization.AuthorizationContext;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.AuthorizationReport;
import io.acra.core.domain.authorization.BflaAssessmentStatus;
import io.acra.core.domain.authorization.BolaAssessmentStatus;
import io.acra.core.domain.authorization.ContextStatus;
import io.acra.core.domain.common.Confidence;
import io.acra.core.domain.evidence.EvidenceSource;
import io.acra.core.domain.finding.AuthorizationImpactProfile;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingSeverity;
import io.acra.core.domain.http.HttpProtocol;
import io.acra.core.domain.http.HttpResponse;
import io.acra.core.domain.identity.AuthenticationType;
import io.acra.core.domain.identity.Principal;
import io.acra.core.domain.identity.Role;
import io.acra.core.domain.resource.Resource;
import io.acra.core.domain.tenant.Tenant;
import io.acra.core.domain.workflow.WorkflowState;
import io.acra.core.engine.AuthorizationAnalysisRequest;
import io.acra.core.engine.AuthorizationOrchestrator;
import io.acra.core.engine.AuthorizationReportGenerator;
import io.acra.core.engine.PolicyValidationEvaluator;
import io.acra.core.recon.SecurityContextFingerprint;
import io.acra.core.serialization.DomainSerializer;
import io.acra.core.tests.TestSupport;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class Sprint5FinalClosureTestSuite {
    private static final String PROJECT_ID = "project-s5";
    private static final String EXECUTION_ID = "execution-s5";
    private static final String TEST_ID = "test-s5";
    private static final String OBSERVATION_ID = "observation-s5";
    private static final String EVIDENCE_ID = "evidence-s5";

    private Sprint5FinalClosureTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT5_FINAL_CLOSURE PASS assertions=" + assertions);
    }

    public static int run() {
        ExecutionEvidenceStore store = store(PROJECT_ID);
        AuthorizationOrchestrator orchestrator = new AuthorizationOrchestrator(store);
        AuthorizationAnalysisRequest request = request(PROJECT_ID);
        AuthorizationAnalysisResult result = orchestrator.analyze(request);
        int assertions = 0;

        TestSupport.assertEquals(ContextStatus.RESOLVED, result.contextAssessment().resolutionStatus(),
                "context normalization resolves verified supplied context");
        assertions++;
        TestSupport.assertTrue(result.contextAssessment().evidenceVerified(),
                "context normalization authenticates evidence");
        assertions++;
        TestSupport.assertTrue(result.contextAssessment().completeness().objectLevelComplete(),
                "object-level context is complete");
        assertions++;
        TestSupport.assertTrue(result.contextAssessment().completeness().functionLevelComplete(),
                "function-level context is complete");
        assertions++;
        TestSupport.assertEquals(BolaAssessmentStatus.BOLA_CANDIDATE, result.bola().status(),
                "BOLA assessment records expected-deny observed-allow mismatch");
        assertions++;
        TestSupport.assertEquals(BflaAssessmentStatus.BFLA_CANDIDATE, result.bfla().status(),
                "BFLA assessment records expected-deny observed-allow mismatch");
        assertions++;
        TestSupport.assertEquals("/admin/export", result.bfla().endpoint(),
                "BFLA retains explicit endpoint binding");
        assertions++;
        TestSupport.assertTrue(result.tenant().violationCandidate(), "tenant policy mismatch remains a candidate");
        assertions++;
        TestSupport.assertTrue(result.workflow().violationCandidate(), "workflow policy mismatch remains a candidate");
        assertions++;
        TestSupport.assertTrue(result.property().violationCandidate(), "property policy mismatch remains a candidate");
        assertions++;
        TestSupport.assertEquals(FindingCandidateState.CANDIDATE, result.findingCandidate().state(),
                "verified authorization dimensions produce a finding candidate, not a confirmed finding");
        assertions++;
        TestSupport.assertEquals(FindingSeverity.CRITICAL, result.riskAssessment().severity(),
                "explicit supplied impact facts deterministically drive internal severity");
        assertions++;
        TestSupport.assertTrue(result.riskAssessment().internalRiskScore() >= 80,
                "critical internal risk score is threshold-consistent");
        assertions++;

        AuthorizationReportGenerator generator = new AuthorizationReportGenerator();
        AuthorizationReport report = generator.generate(request, result);
        String json = generator.renderJson(report);
        TestSupport.assertContains(json, "CANDIDATE", "report includes candidate state");
        assertions++;
        TestSupport.assertContains(json, "CRITICAL", "report includes deterministic severity");
        assertions++;
        TestSupport.assertNotContains(json, "DummyPassword", "report boundary remains secret-safe");
        assertions++;

        AuthorizationAnalysisResult crossProject = new AuthorizationOrchestrator(store).analyze(request("other-project"));
        TestSupport.assertTrue(!crossProject.contextAssessment().evidenceVerified(),
                "cross-project evidence is rejected");
        assertions++;
        TestSupport.assertEquals(FindingCandidateState.INCONCLUSIVE, crossProject.findingCandidate().state(),
                "cross-project references cannot produce a candidate");
        assertions++;

        String serialized = new DomainSerializer().serialize(result);
        TestSupport.assertNotContains(serialized, "DummyPassword", "full S5 result remains secret-safe");
        assertions++;
        TestSupport.assertTrue(result.bola().assessmentId().startsWith("bola-"),
                "BOLA uses deterministic SHA-256 based assessment identifiers");
        assertions++;
        TestSupport.assertTrue(result.bfla().assessmentId().startsWith("bfla-"),
                "BFLA uses deterministic SHA-256 based assessment identifiers");
        assertions++;
        return assertions;
    }

    private static AuthorizationAnalysisRequest request(String projectId) {
        AuthorizationContext context = context();
        PolicyValidationEvaluator.TenantPolicy tenantPolicy = new PolicyValidationEvaluator.TenantPolicy(
                "tenant-policy", "source", "tenant-a", "tenant-b", "viewer", false, false,
                AuthorizationDecision.DENY, List.of(EVIDENCE_ID));
        PolicyValidationEvaluator.WorkflowPolicy workflowPolicy = new PolicyValidationEvaluator.WorkflowPolicy(
                "workflow-policy", "source", "DRAFT", "APPROVED", "viewer", true, true, false,
                AuthorizationDecision.DENY, List.of(EVIDENCE_ID));
        PolicyValidationEvaluator.PropertyPolicy propertyPolicy = new PolicyValidationEvaluator.PropertyPolicy(
                "property-policy", "password=DummyPassword", "/admin/export", "sensitive",
                PolicyValidationEvaluator.PropertyOperation.READ, "viewer", "tenant-a",
                AuthorizationDecision.DENY, List.of(EVIDENCE_ID));
        AuthorizationImpactProfile impact = new AuthorizationImpactProfile(true, true, false, true, true, false,
                List.of("explicit-lab-impact"));
        return new AuthorizationAnalysisRequest(context, "/admin/export", "policy-s5", projectId,
                OBSERVATION_ID, EXECUTION_ID, TEST_ID, tenantPolicy, workflowPolicy, "DRAFT", "APPROVED",
                true, true, propertyPolicy, "sensitive", PolicyValidationEvaluator.PropertyOperation.READ, impact);
    }

    private static AuthorizationContext context() {
        Principal principal = new Principal("user-a", "User A", AuthenticationType.UNKNOWN, Confidence.unknown());
        Role role = new Role("viewer", "viewer", EvidenceSource.UNKNOWN, Confidence.unknown());
        Tenant tenant = new Tenant("tenant-a", "Tenant A", EvidenceSource.UNKNOWN, Confidence.unknown());
        Resource resource = new Resource("document-b", "document", null, "user-b", "tenant-b", "DRAFT",
                Confidence.unknown());
        Action action = new Action(ActionType.EXPORT, EvidenceSource.UNKNOWN, Confidence.unknown(), "ADMIN_EXPORT");
        return new AuthorizationContext(principal, role, tenant, resource, "user-b", action,
                new WorkflowState("DRAFT", Confidence.unknown()), AuthorizationDecision.DENY,
                AuthorizationDecision.ALLOW, List.of(EVIDENCE_ID), ContextStatus.RESOLVED);
    }

    private static ExecutionEvidenceStore store(String projectId) {
        ExecutionEvidenceStore store = new ExecutionEvidenceStore(projectId);
        store.append(EXECUTION_ID, TEST_ID, EvidenceStage.TEST, EVIDENCE_ID, "s5-policy-evidence");
        store.append(EXECUTION_ID, TEST_ID, EvidenceStage.OBSERVATION, OBSERVATION_ID, observation());
        return store;
    }

    private static Observation observation() {
        ResponseSnapshot response = responseSnapshot();
        return new Observation(OBSERVATION_ID, TEST_ID, response, response, response, response,
                new ExpectedDecisionResolution(AuthorizationDecision.DENY,
                        ExpectedDecisionSource.EXPLICIT_CONFIGURED_POLICY, "policy-s5",
                        List.of(EVIDENCE_ID), 1.0, false), AuthorizationOutcome.ALLOW,
                new MultiWayDifferential(AuthorizationOutcome.ALLOW, AuthorizationOutcome.ALLOW,
                        AuthorizationOutcome.DENY, AuthorizationOutcome.ALLOW, List.of(),
                        DifferentialClassification.UNEXPECTED_CHANGE, List.of()),
                securityContext(), securityContext(), List.of(EVIDENCE_ID), 1.0,
                new ExecutionFingerprint(EXECUTION_ID, TEST_ID, "request-fingerprint", "response-fingerprint",
                        "configuration-fingerprint", "environment-fingerprint"),
                Instant.parse("2026-09-23T00:00:00Z"));
    }

    private static ResponseSnapshot responseSnapshot() {
        HttpResponse response = new HttpResponse(200, List.of(), "{}".getBytes(), "application/json",
                HttpProtocol.HTTP_1_1, new byte[0]);
        return new ResponseSnapshot("response-s5", "request-s5", response, Map.of(), Duration.ZERO,
                Instant.parse("2026-09-23T00:00:00Z"), new ResponseSemanticAnalyzer().fingerprint(response), "");
    }

    private static SecurityContextFingerprint securityContext() {
        return new SecurityContextFingerprint("user-a", "viewer", "tenant-a", "document-b", "user-b",
                "ADMIN_EXPORT", "DRAFT", "POST", "/admin/export", "token-fingerprint",
                AuthorizationDecision.DENY, AuthorizationDecision.ALLOW, List.of(EVIDENCE_ID));
    }
}
