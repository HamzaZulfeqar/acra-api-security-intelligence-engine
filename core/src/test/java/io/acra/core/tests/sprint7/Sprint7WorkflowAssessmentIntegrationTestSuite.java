package io.acra.core.tests.sprint7;

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
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.AuthorizationPolicySnapshot;
import io.acra.core.domain.authorization.AuthorizationRuleEffect;
import io.acra.core.domain.finding.AuthorizationImpactProfile;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.FindingSeverity;
import io.acra.core.domain.http.HttpProtocol;
import io.acra.core.domain.http.HttpResponse;
import io.acra.core.domain.workflow.S7WorkflowAnalysisResult;
import io.acra.core.domain.workflow.WorkflowAuthorizationRequest;
import io.acra.core.domain.workflow.WorkflowPolicySnapshot;
import io.acra.core.domain.workflow.WorkflowTokenBinding;
import io.acra.core.domain.workflow.WorkflowTransitionAssessmentState;
import io.acra.core.domain.workflow.WorkflowTransitionRule;
import io.acra.core.engine.S7WorkflowAnalysisRequest;
import io.acra.core.engine.S7WorkflowOrchestrator;
import io.acra.core.recon.SecurityContextFingerprint;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.serialization.DomainSerializer;
import io.acra.core.tests.TestSupport;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class Sprint7WorkflowAssessmentIntegrationTestSuite {
    private static final String PROJECT = "s7-project";
    private static final String EXECUTION = "s7-execution";
    private static final String TEST = "s7-test";
    private static final String OBSERVATION = "s7-observation";
    private static final String POLICY_EVIDENCE = "s7-policy-evidence";
    private static final String RULE_EVIDENCE = "s7-rule-evidence";
    private static final String BINDING_EVIDENCE = "s7-binding-evidence";
    private static final Instant NOW = Instant.parse("2026-09-23T17:30:00Z");
    private static final String RAW_TOKEN = "s7-synthetic-approver-secret";
    private static final String TOKEN_FP = TokenFingerprint.sha256(RAW_TOKEN);

    private Sprint7WorkflowAssessmentIntegrationTestSuite() { }

    public static void main(String[] args) throws Exception {
        int assertions = run();
        System.out.println("SPRINT7_WORKFLOW_ASSESSMENT PASS assertions=" + assertions);
    }

    public static int run() throws Exception {
        int assertions = 0;
        ExecutionEvidenceStore store = store();
        S7WorkflowOrchestrator orchestrator = new S7WorkflowOrchestrator(store);

        S7WorkflowAnalysisResult candidate = orchestrator.analyze(request(
                AuthorizationDecision.ALLOW, TOKEN_FP, false, true));
        TestSupport.assertEquals(AuthorizationDecision.DENY, candidate.resolution().expectedDecision(),
                "workflow policy should resolve this controlled approval attempt to DENY");
        assertions++;
        TestSupport.assertEquals(WorkflowTransitionAssessmentState.CANDIDATE, candidate.assessment().state(),
                "expected deny / observed allow becomes workflow candidate assessment");
        assertions++;
        TestSupport.assertEquals(FindingCandidateState.CANDIDATE, candidate.findingCandidate().state(),
                "verified workflow mismatch composes into FindingCandidate");
        assertions++;
        TestSupport.assertTrue(candidate.findingCandidate().dimensions().contains("WORKFLOW"),
                "workflow dimension retained");
        assertions++;
        TestSupport.assertTrue(candidate.findingCandidate().dimensions().contains("APPROVAL"),
                "approval failure dimension retained");
        assertions++;
        TestSupport.assertTrue(candidate.findingCandidate().dimensions().contains("TOKEN_BINDING"),
                "token-binding policy dimension retained");
        assertions++;
        TestSupport.assertEquals(FindingSeverity.MEDIUM, candidate.riskAssessment().severity(),
                "workflow integrity plus privileged-function impact follows the existing deterministic severity scale");
        assertions++;
        TestSupport.assertEquals(55, candidate.riskAssessment().internalRiskScore(),
                "workflow risk score must remain deterministic rather than being inflated for Sprint 7");
        assertions++;
        TestSupport.assertEquals(0, candidate.findingCandidate().contradictoryEvidence().size(),
                "valid evidence ownership should not create provenance contradictions");
        assertions++;

        S7WorkflowAnalysisResult denied = orchestrator.analyze(request(
                AuthorizationDecision.DENY, TOKEN_FP, false, true));
        TestSupport.assertEquals(WorkflowTransitionAssessmentState.NO_VIOLATION, denied.assessment().state(),
                "expected deny / observed deny is no violation");
        assertions++;
        TestSupport.assertEquals(FindingCandidateState.REJECTED, denied.findingCandidate().state(),
                "compatible denied observation is rejected as finding candidate");
        assertions++;

        S7WorkflowAnalysisRequest forged = new S7WorkflowAnalysisRequest(
                workflowPolicy(), null,
                workflowRequest(AuthorizationDecision.ALLOW, TOKEN_FP, false, true),
                "different-project", TEST, EXECUTION, OBSERVATION, "/api/v1/s7/workflow/approve",
                impact());
        S7WorkflowAnalysisResult forgedResult = orchestrator.analyze(forged);
        TestSupport.assertEquals(FindingCandidateState.INCONCLUSIVE, forgedResult.findingCandidate().state(),
                "cross-project provenance mismatch must block finding candidate");
        assertions++;
        TestSupport.assertTrue(forgedResult.findingCandidate().contradictoryEvidence()
                        .stream().anyMatch(value -> value.contains("CROSS_PROJECT_REFERENCE")),
                "cross-project rejection reason retained");
        assertions++;

        String serialized = new DomainSerializer().serialize(candidate);
        TestSupport.assertNotContains(serialized, RAW_TOKEN, "raw token excluded from S7 analysis serialization");
        assertions++;
        TestSupport.assertNotContains(serialized, TOKEN_FP,
                "token-context fingerprint is minimized out of downstream S7 analysis result");
        assertions++;

        String gt = Files.readString(Path.of("lab/ground-truth/GT-S7-WORKFLOW-AUTHORIZATION.json"));
        TestSupport.assertContains(gt, "\"S7-GT-001\"", "ground truth includes valid submit control");
        assertions++;
        TestSupport.assertContains(gt, "\"S7-GT-010\"", "ground truth includes conflict control");
        assertions++;
        TestSupport.assertContains(gt, "\"scope\": \"controlled localhost ACRA-Lab only\"",
                "ground truth scope is explicitly local/controlled");
        assertions++;
        TestSupport.assertNotContains(gt, RAW_TOKEN, "ground truth contains no raw authentication material");
        assertions++;

        return assertions;
    }

    private static S7WorkflowAnalysisRequest request(
            AuthorizationDecision observed,
            String tokenFingerprint,
            boolean approvalProvided,
            boolean separationSatisfied) {
        return new S7WorkflowAnalysisRequest(
                workflowPolicy(),
                null,
                workflowRequest(observed, tokenFingerprint, approvalProvided, separationSatisfied),
                PROJECT,
                TEST,
                EXECUTION,
                OBSERVATION,
                "/api/v1/s7/workflow/approve",
                impact());
    }

    private static WorkflowAuthorizationRequest workflowRequest(
            AuthorizationDecision observed,
            String tokenFingerprint,
            boolean approvalProvided,
            boolean separationSatisfied) {
        return new WorkflowAuthorizationRequest(
                "document-approval",
                "approver-a",
                List.of("approver"),
                "tenant-a",
                "document-1",
                "APPROVE",
                "SUBMITTED",
                "APPROVED",
                approvalProvided,
                separationSatisfied,
                "",
                tokenFingerprint,
                observed,
                NOW);
    }

    private static WorkflowPolicySnapshot workflowPolicy() {
        return WorkflowPolicySnapshot.create(
                "s7-workflow-policy",
                "1",
                "controlled-s7-assessment",
                List.of(new WorkflowTransitionRule(
                        "approve-rule",
                        "document-approval",
                        "SUBMITTED",
                        "APPROVED",
                        "APPROVE",
                        "tenant-a",
                        List.of("approver"),
                        true,
                        true,
                        false,
                        false,
                        "binding-approve",
                        AuthorizationRuleEffect.ALLOW,
                        null,
                        "",
                        List.of(RULE_EVIDENCE))),
                List.of(new WorkflowTokenBinding(
                        "binding-approve",
                        "document-approval",
                        "APPROVE",
                        "",
                        "approver",
                        "tenant-a",
                        TOKEN_FP,
                        List.of(BINDING_EVIDENCE))),
                List.of(POLICY_EVIDENCE),
                AuthorizationDecision.DENY,
                NOW);
    }

    private static AuthorizationImpactProfile impact() {
        return new AuthorizationImpactProfile(
                false,
                true,
                false,
                false,
                true,
                false,
                List.of("workflow-state integrity", "privileged approval"));
    }

    private static ExecutionEvidenceStore store() {
        ExecutionEvidenceStore store = new ExecutionEvidenceStore(PROJECT);
        store.append(EXECUTION, TEST, EvidenceStage.TEST, POLICY_EVIDENCE, "s7 workflow policy");
        store.append(EXECUTION, TEST, EvidenceStage.TEST, RULE_EVIDENCE, "s7 workflow transition rule");
        store.append(EXECUTION, TEST, EvidenceStage.TEST, BINDING_EVIDENCE, "s7 token binding");
        store.append(EXECUTION, TEST, EvidenceStage.OBSERVATION, OBSERVATION, observation());
        return store;
    }

    private static Observation observation() {
        ResponseSnapshot response = response();
        return new Observation(
                OBSERVATION,
                TEST,
                response,
                response,
                response,
                response,
                new ExpectedDecisionResolution(
                        AuthorizationDecision.DENY,
                        ExpectedDecisionSource.EXPLICIT_CONFIGURED_POLICY,
                        "s7-workflow-policy",
                        List.of(POLICY_EVIDENCE, RULE_EVIDENCE, BINDING_EVIDENCE),
                        1.0,
                        false),
                AuthorizationOutcome.ALLOW,
                new MultiWayDifferential(
                        AuthorizationOutcome.DENY,
                        AuthorizationOutcome.ALLOW,
                        AuthorizationOutcome.DENY,
                        AuthorizationOutcome.ALLOW,
                        List.of(),
                        DifferentialClassification.UNEXPECTED_CHANGE,
                        List.of()),
                context(),
                context(),
                List.of(POLICY_EVIDENCE, RULE_EVIDENCE, BINDING_EVIDENCE),
                1.0,
                new ExecutionFingerprint(EXECUTION, TEST, "req", "res", "cfg", "env"),
                NOW);
    }

    private static ResponseSnapshot response() {
        HttpResponse response = new HttpResponse(
                200, List.of(), "{}".getBytes(), "application/json", HttpProtocol.HTTP_1_1, new byte[0]);
        return new ResponseSnapshot(
                "s7-response", "s7-request", response, Map.of(), Duration.ZERO, NOW,
                new ResponseSemanticAnalyzer().fingerprint(response), "");
    }

    private static SecurityContextFingerprint context() {
        return new SecurityContextFingerprint(
                "author-a",
                "author",
                "tenant-a",
                "document-1",
                "author-a",
                "APPROVE",
                "SUBMITTED",
                "POST /api/v1/s7/workflow/approve",
                "RAW",
                "token-context-ref",
                AuthorizationDecision.DENY,
                AuthorizationDecision.ALLOW,
                List.of(POLICY_EVIDENCE, RULE_EVIDENCE, BINDING_EVIDENCE));
    }
}
