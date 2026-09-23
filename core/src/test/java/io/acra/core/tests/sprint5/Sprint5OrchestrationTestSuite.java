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
import io.acra.core.domain.authorization.AuthorizationAnalysisRequest;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.BflaAssessmentStatus;
import io.acra.core.domain.authorization.BolaAssessmentStatus;
import io.acra.core.domain.authorization.PolicyValidationState;
import io.acra.core.domain.finding.AuthorizationImpact;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.finding.ImpactLevel;
import io.acra.core.domain.finding.SeverityLevel;
import io.acra.core.domain.http.HttpProtocol;
import io.acra.core.domain.http.HttpResponse;
import io.acra.core.engine.AuthorizationAnalysisOrchestrator;
import io.acra.core.engine.PolicyValidationEvaluator;
import io.acra.core.recon.SecurityContextFingerprint;
import io.acra.core.tests.TestSupport;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class Sprint5OrchestrationTestSuite {
    private static final String PROJECT_ID = "project-a";
    private static final String EXECUTION_ID = "execution-1";
    private static final String TEST_ID = "test-1";
    private static final String OBSERVATION_ID = "observation-1";
    private static final String EVIDENCE_ID = "evidence-1";

    private Sprint5OrchestrationTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT5_ORCHESTRATION PASS assertions=" + assertions);
    }

    public static int run() {
        int assertions = 0;
        Observation observation = violationObservation();
        ExecutionEvidenceStore store = new ExecutionEvidenceStore(PROJECT_ID);
        store.append(EXECUTION_ID, TEST_ID, EvidenceStage.TEST, EVIDENCE_ID, "explicit-policy-evidence");
        store.append(EXECUTION_ID, TEST_ID, EvidenceStage.OBSERVATION, OBSERVATION_ID, observation);

        AuthorizationAnalysisOrchestrator orchestrator = new AuthorizationAnalysisOrchestrator(store);
        AuthorizationAnalysisRequest request = new AuthorizationAnalysisRequest(
                observation,
                PROJECT_ID,
                "GET /documents/{id}",
                new PolicyValidationEvaluator.TenantPolicy(
                        "tenant-policy", "fixture", "tenant-b", "tenant-b", "viewer",
                        false, false, AuthorizationDecision.DENY, List.of(EVIDENCE_ID)),
                null,
                "",
                "",
                false,
                false,
                null,
                "",
                null,
                "",
                new AuthorizationImpact(ImpactLevel.HIGH, false, false, false,
                        false, false, false, List.of(EVIDENCE_ID)));

        var result = orchestrator.analyze(request);
        TestSupport.assertTrue(result.normalization().valid(), "verified observation must normalize");
        TestSupport.assertEquals(BolaAssessmentStatus.BOLA_CANDIDATE, result.bola().status(),
                "cross-owner expected-deny/observed-allow should become BOLA candidate assessment");
        TestSupport.assertEquals(BflaAssessmentStatus.BFLA_CANDIDATE, result.bfla().status(),
                "function decision mismatch should become BFLA candidate assessment");
        TestSupport.assertEquals(PolicyValidationState.CONFLICTING, result.tenant().state(),
                "explicit tenant policy deny/observed allow remains an explicit conflict");
        TestSupport.assertEquals(FindingCandidateState.SUPPORTED, result.candidate().state(),
                "validated explicit deny/allow mismatch should produce supported candidate");
        TestSupport.assertEquals(SeverityLevel.HIGH, result.severity().level(),
                "explicit high impact must not be downgraded");
        TestSupport.assertTrue(result.candidate().category().contains("BOLA"),
                "candidate records BOLA support");
        TestSupport.assertTrue(result.candidate().category().contains("TENANT"),
                "candidate records tenant support");
        assertions += 8;

        AuthorizationAnalysisRequest wrongProject = new AuthorizationAnalysisRequest(
                observation, "project-b", "GET /documents/{id}",
                null, null, "", "", false, false, null, "", null, "",
                new AuthorizationImpact(ImpactLevel.HIGH, false, false, true,
                        false, false, false, List.of(EVIDENCE_ID)));
        var rejected = orchestrator.analyze(wrongProject);
        TestSupport.assertFalse(rejected.normalization().valid(),
                "cross-project observation must fail context normalization");
        TestSupport.assertEquals(FindingCandidateState.INCONCLUSIVE, rejected.candidate().state(),
                "cross-project evidence cannot produce supported candidate");
        TestSupport.assertEquals(SeverityLevel.UNKNOWN, rejected.severity().level(),
                "inconclusive candidate cannot receive authorization severity");
        assertions += 3;
        return assertions;
    }

    private static Observation violationObservation() {
        ResponseSnapshot response = responseSnapshot();
        SecurityContextFingerprint source = new SecurityContextFingerprint(
                "user-a", "viewer", "tenant-a", "document-1", "user-a", "READ",
                "DRAFT", "GET", "/documents/{id}", "token-a",
                AuthorizationDecision.ALLOW, AuthorizationDecision.ALLOW, List.of(EVIDENCE_ID));
        SecurityContextFingerprint target = new SecurityContextFingerprint(
                "user-b", "viewer", "tenant-b", "document-1", "user-a", "READ",
                "DRAFT", "GET", "/documents/{id}", "token-b",
                AuthorizationDecision.DENY, AuthorizationDecision.ALLOW, List.of(EVIDENCE_ID));
        return new Observation(OBSERVATION_ID, TEST_ID, response, response, response, response,
                new ExpectedDecisionResolution(AuthorizationDecision.DENY,
                        ExpectedDecisionSource.EXPLICIT_CONFIGURED_POLICY, "tenant-policy",
                        List.of(EVIDENCE_ID), 1.0, false), AuthorizationOutcome.ALLOW,
                new MultiWayDifferential(AuthorizationOutcome.ALLOW, AuthorizationOutcome.ALLOW,
                        AuthorizationOutcome.DENY, AuthorizationOutcome.ALLOW, List.of(),
                        DifferentialClassification.UNEXPECTED_CHANGE, List.of()),
                source, target, List.of(EVIDENCE_ID), 1.0,
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
