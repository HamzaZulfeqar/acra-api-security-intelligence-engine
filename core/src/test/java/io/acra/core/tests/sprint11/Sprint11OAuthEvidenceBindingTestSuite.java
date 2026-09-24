package io.acra.core.tests.sprint11;

import io.acra.core.active.evidence.EvidenceStage;
import io.acra.core.active.evidence.ExecutionEvidenceStore;
import io.acra.core.oauth.OAuthContextObservation;
import io.acra.core.oauth.OAuthEvidenceBinding;
import io.acra.core.oauth.OAuthProtocol;
import io.acra.core.oauth.PkceMethod;
import io.acra.core.oauth.S11OAuthEvidenceValidator;
import io.acra.core.recon.IdentityConfidenceState;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.tests.TestSupport;
import java.time.Instant;
import java.util.List;

public final class Sprint11OAuthEvidenceBindingTestSuite {
    private static final String PROJECT = "project-s11";
    private static final String EXECUTION = "execution-s11";
    private static final String TEST = "test-s11";
    private static final String REQUEST = "request-s11";
    private static final String OBSERVATION = "oauth-observation-s11";
    private static final String EVIDENCE = "oauth-evidence-s11";

    private Sprint11OAuthEvidenceBindingTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT11_OAUTH_EVIDENCE_BINDING PASS assertions=" + assertions);
    }

    public static int run() {
        int assertions = 0;

        ExecutionEvidenceStore store = new ExecutionEvidenceStore(PROJECT);
        store.append(EXECUTION, TEST, EvidenceStage.TEST, EVIDENCE, "explicit-oauth-context-evidence");
        store.append(EXECUTION, TEST, EvidenceStage.OBSERVATION, OBSERVATION,
                observation(OBSERVATION, REQUEST, List.of(EVIDENCE)));

        S11OAuthEvidenceValidator validator = new S11OAuthEvidenceValidator(store);

        var valid = validator.validate(binding(PROJECT, EXECUTION, TEST, REQUEST, OBSERVATION, List.of(EVIDENCE)));
        TestSupport.assertTrue(valid.valid(), "matching OAuth project/request/test/execution lineage validates");
        assertions++;

        var crossProject = validator.validate(binding(
                "other-project", EXECUTION, TEST, REQUEST, OBSERVATION, List.of(EVIDENCE)));
        TestSupport.assertTrue(!crossProject.valid(), "cross-project OAuth evidence fails closed");
        assertions++;
        TestSupport.assertContains(String.join(",", crossProject.reasons()), "CROSS_PROJECT_REFERENCE",
                "cross-project OAuth reason remains explicit");
        assertions++;

        var wrongExecution = validator.validate(binding(
                PROJECT, "other-execution", TEST, REQUEST, OBSERVATION, List.of(EVIDENCE)));
        TestSupport.assertTrue(!wrongExecution.valid(), "OAuth execution mismatch fails closed");
        assertions++;
        TestSupport.assertContains(String.join(",", wrongExecution.reasons()), "EXECUTION_OWNERSHIP_MISMATCH",
                "OAuth evidence execution mismatch remains explicit");
        assertions++;
        TestSupport.assertContains(String.join(",", wrongExecution.reasons()), "OAUTH_EXECUTION_LINEAGE_MISMATCH",
                "OAuth observation execution mismatch remains explicit");
        assertions++;

        var wrongTest = validator.validate(binding(
                PROJECT, EXECUTION, "other-test", REQUEST, OBSERVATION, List.of(EVIDENCE)));
        TestSupport.assertTrue(!wrongTest.valid(), "OAuth test mismatch fails closed");
        assertions++;
        TestSupport.assertContains(String.join(",", wrongTest.reasons()), "TEST_OWNERSHIP_MISMATCH",
                "OAuth evidence test mismatch remains explicit");
        assertions++;
        TestSupport.assertContains(String.join(",", wrongTest.reasons()), "OAUTH_TEST_LINEAGE_MISMATCH",
                "OAuth observation test mismatch remains explicit");
        assertions++;

        var wrongRequest = validator.validate(binding(
                PROJECT, EXECUTION, TEST, "other-request", OBSERVATION, List.of(EVIDENCE)));
        TestSupport.assertTrue(!wrongRequest.valid(), "OAuth request mismatch fails closed");
        assertions++;
        TestSupport.assertContains(String.join(",", wrongRequest.reasons()), "OAUTH_REQUEST_LINEAGE_MISMATCH",
                "OAuth request lineage mismatch remains explicit");
        assertions++;

        var unknownEvidence = validator.validate(binding(
                PROJECT, EXECUTION, TEST, REQUEST, OBSERVATION, List.of("unknown-evidence")));
        TestSupport.assertTrue(!unknownEvidence.valid(), "unknown OAuth evidence fails closed");
        assertions++;
        TestSupport.assertContains(String.join(",", unknownEvidence.reasons()), "EVIDENCE_UNKNOWN",
                "unknown OAuth evidence reason remains explicit");
        assertions++;
        TestSupport.assertContains(String.join(",", unknownEvidence.reasons()),
                "OAUTH_EVIDENCE_PROVENANCE_CONTRADICTORY",
                "OAuth observation/evidence set mismatch remains explicit");
        assertions++;

        var duplicateEvidence = validator.validate(binding(
                PROJECT, EXECUTION, TEST, REQUEST, OBSERVATION, List.of(EVIDENCE, EVIDENCE)));
        TestSupport.assertTrue(!duplicateEvidence.valid(), "duplicate OAuth evidence fails closed");
        assertions++;
        TestSupport.assertContains(String.join(",", duplicateEvidence.reasons()), "EVIDENCE_REFERENCE_DUPLICATED",
                "duplicate OAuth evidence reason remains explicit");
        assertions++;

        ExecutionEvidenceStore wrongTypeStore = new ExecutionEvidenceStore(PROJECT);
        wrongTypeStore.append(EXECUTION, TEST, EvidenceStage.TEST, EVIDENCE, "explicit-oauth-context-evidence");
        wrongTypeStore.append(EXECUTION, TEST, EvidenceStage.OBSERVATION, OBSERVATION, "not-oauth-observation");
        var wrongType = new S11OAuthEvidenceValidator(wrongTypeStore).validate(
                binding(PROJECT, EXECUTION, TEST, REQUEST, OBSERVATION, List.of(EVIDENCE)));
        TestSupport.assertTrue(!wrongType.valid(), "non-OAuth observation object fails closed");
        assertions++;
        TestSupport.assertContains(String.join(",", wrongType.reasons()), "OAUTH_OBSERVATION_TYPE_INVALID",
                "OAuth observation type mismatch remains explicit");
        assertions++;

        ExecutionEvidenceStore contradictoryStore = new ExecutionEvidenceStore(PROJECT);
        contradictoryStore.append(EXECUTION, TEST, EvidenceStage.TEST, EVIDENCE, "explicit-oauth-context-evidence");
        contradictoryStore.append(EXECUTION, TEST, EvidenceStage.OBSERVATION, OBSERVATION,
                observation(OBSERVATION, REQUEST, List.of("different-evidence")));
        var contradictory = new S11OAuthEvidenceValidator(contradictoryStore).validate(
                binding(PROJECT, EXECUTION, TEST, REQUEST, OBSERVATION, List.of(EVIDENCE)));
        TestSupport.assertTrue(!contradictory.valid(), "contradictory OAuth evidence set fails closed");
        assertions++;
        TestSupport.assertContains(String.join(",", contradictory.reasons()),
                "OAUTH_EVIDENCE_PROVENANCE_CONTRADICTORY",
                "contradictory OAuth provenance reason remains explicit");
        assertions++;

        return assertions;
    }

    private static OAuthEvidenceBinding binding(
            String project,
            String execution,
            String test,
            String request,
            String observation,
            List<String> evidence) {
        return new OAuthEvidenceBinding(project, execution, test, request, observation, evidence);
    }

    private static OAuthContextObservation observation(
            String observationId,
            String requestId,
            List<String> evidence) {
        return new OAuthContextObservation(
                observationId,
                requestId,
                OAuthProtocol.OIDC,
                "https://issuer.example",
                "https://issuer.example/authorize",
                "https://issuer.example/token",
                "client-a",
                List.of("https://api.example"),
                List.of("api://orders"),
                List.of("openid", "profile"),
                List.of("code"),
                "authorization_code",
                TokenFingerprint.sha256("https://app.example/callback"),
                PkceMethod.S256,
                true,
                true,
                IdentityConfidenceState.INFERRED,
                Instant.parse("2026-09-24T18:45:00Z"),
                evidence);
    }
}
