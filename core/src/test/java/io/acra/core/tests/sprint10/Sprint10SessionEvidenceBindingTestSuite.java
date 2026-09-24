package io.acra.core.tests.sprint10;

import io.acra.core.active.evidence.EvidenceStage;
import io.acra.core.active.evidence.ExecutionEvidenceStore;
import io.acra.core.domain.identity.AuthenticationType;
import io.acra.core.recon.IdentityConfidenceState;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.session.AuthenticationSessionObservation;
import io.acra.core.session.S10SessionEvidenceValidator;
import io.acra.core.session.SessionEvidenceBinding;
import io.acra.core.tests.TestSupport;
import java.time.Instant;
import java.util.List;

public final class Sprint10SessionEvidenceBindingTestSuite {
    private static final String PROJECT = "project-s10";
    private static final String EXECUTION = "execution-s10";
    private static final String TEST = "test-s10";
    private static final String OBSERVATION = "session-observation-s10";
    private static final String EVIDENCE = "session-evidence-s10";

    private Sprint10SessionEvidenceBindingTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT10_SESSION_EVIDENCE_BINDING PASS assertions=" + assertions);
    }

    public static int run() {
        int assertions = 0;

        ExecutionEvidenceStore store = new ExecutionEvidenceStore(PROJECT);
        store.append(EXECUTION, TEST, EvidenceStage.TEST, EVIDENCE, "explicit-session-context-evidence");
        store.append(EXECUTION, TEST, EvidenceStage.OBSERVATION, OBSERVATION, observation(OBSERVATION, List.of(EVIDENCE)));

        S10SessionEvidenceValidator validator = new S10SessionEvidenceValidator(store);
        var valid = validator.validate(binding(PROJECT, EXECUTION, TEST, OBSERVATION, List.of(EVIDENCE)));
        TestSupport.assertTrue(valid.valid(),
                "matching project/test/execution/session observation lineage must validate");
        assertions++;

        var crossProject = validator.validate(binding(
                "other-project", EXECUTION, TEST, OBSERVATION, List.of(EVIDENCE)));
        TestSupport.assertTrue(!crossProject.valid(),
                "cross-project session evidence must fail closed");
        assertions++;
        TestSupport.assertContains(String.join(",", crossProject.reasons()), "CROSS_PROJECT_REFERENCE",
                "cross-project rejection reason remains explicit");
        assertions++;

        var wrongExecution = validator.validate(binding(
                PROJECT, "other-execution", TEST, OBSERVATION, List.of(EVIDENCE)));
        TestSupport.assertTrue(!wrongExecution.valid(),
                "execution ownership mismatch must fail closed");
        assertions++;
        TestSupport.assertContains(String.join(",", wrongExecution.reasons()), "EXECUTION_OWNERSHIP_MISMATCH",
                "execution evidence mismatch remains explicit");
        assertions++;
        TestSupport.assertContains(String.join(",", wrongExecution.reasons()), "SESSION_EXECUTION_LINEAGE_MISMATCH",
                "session observation execution mismatch remains explicit");
        assertions++;

        var wrongTest = validator.validate(binding(
                PROJECT, EXECUTION, "other-test", OBSERVATION, List.of(EVIDENCE)));
        TestSupport.assertTrue(!wrongTest.valid(),
                "test ownership mismatch must fail closed");
        assertions++;
        TestSupport.assertContains(String.join(",", wrongTest.reasons()), "TEST_OWNERSHIP_MISMATCH",
                "test evidence mismatch remains explicit");
        assertions++;

        var unknownEvidence = validator.validate(binding(
                PROJECT, EXECUTION, TEST, OBSERVATION, List.of("unknown-evidence")));
        TestSupport.assertTrue(!unknownEvidence.valid(),
                "unknown evidence reference must fail closed");
        assertions++;
        TestSupport.assertContains(String.join(",", unknownEvidence.reasons()), "EVIDENCE_UNKNOWN",
                "unknown evidence reason remains explicit");
        assertions++;
        TestSupport.assertContains(String.join(",", unknownEvidence.reasons()),
                "SESSION_EVIDENCE_PROVENANCE_CONTRADICTORY",
                "observation/evidence set mismatch remains explicit");
        assertions++;

        var duplicateEvidence = validator.validate(binding(
                PROJECT, EXECUTION, TEST, OBSERVATION, List.of(EVIDENCE, EVIDENCE)));
        TestSupport.assertTrue(!duplicateEvidence.valid(),
                "duplicate evidence references must fail closed");
        assertions++;
        TestSupport.assertContains(String.join(",", duplicateEvidence.reasons()),
                "EVIDENCE_REFERENCE_DUPLICATED",
                "duplicate evidence reason remains explicit");
        assertions++;

        ExecutionEvidenceStore wrongTypeStore = new ExecutionEvidenceStore(PROJECT);
        wrongTypeStore.append(EXECUTION, TEST, EvidenceStage.TEST, EVIDENCE, "explicit-session-context-evidence");
        wrongTypeStore.append(EXECUTION, TEST, EvidenceStage.OBSERVATION, OBSERVATION, "not-a-session-observation");
        var wrongType = new S10SessionEvidenceValidator(wrongTypeStore).validate(
                binding(PROJECT, EXECUTION, TEST, OBSERVATION, List.of(EVIDENCE)));
        TestSupport.assertTrue(!wrongType.valid(),
                "non-session observation object must fail closed");
        assertions++;
        TestSupport.assertContains(String.join(",", wrongType.reasons()),
                "SESSION_OBSERVATION_TYPE_INVALID",
                "session observation type mismatch remains explicit");
        assertions++;

        ExecutionEvidenceStore contradictoryStore = new ExecutionEvidenceStore(PROJECT);
        contradictoryStore.append(EXECUTION, TEST, EvidenceStage.TEST, EVIDENCE, "explicit-session-context-evidence");
        contradictoryStore.append(
                EXECUTION, TEST, EvidenceStage.OBSERVATION, OBSERVATION,
                observation(OBSERVATION, List.of("different-evidence")));
        var contradictory = new S10SessionEvidenceValidator(contradictoryStore).validate(
                binding(PROJECT, EXECUTION, TEST, OBSERVATION, List.of(EVIDENCE)));
        TestSupport.assertTrue(!contradictory.valid(),
                "session observation with contradictory evidence set must fail closed");
        assertions++;
        TestSupport.assertContains(String.join(",", contradictory.reasons()),
                "SESSION_EVIDENCE_PROVENANCE_CONTRADICTORY",
                "contradictory session provenance reason remains explicit");
        assertions++;

        return assertions;
    }

    private static SessionEvidenceBinding binding(
            String project,
            String execution,
            String test,
            String observation,
            List<String> evidence) {
        return new SessionEvidenceBinding(project, execution, test, observation, evidence);
    }

    private static AuthenticationSessionObservation observation(String id, List<String> evidence) {
        return new AuthenticationSessionObservation(
                id,
                "session-a",
                TokenFingerprint.sha256("token-a"),
                "user-a",
                "viewer",
                "tenant-a",
                List.of("profile.read"),
                AuthenticationType.OAUTH,
                IdentityConfidenceState.USER_CONFIRMED,
                Instant.parse("2026-09-24T17:00:00Z"),
                evidence);
    }
}
