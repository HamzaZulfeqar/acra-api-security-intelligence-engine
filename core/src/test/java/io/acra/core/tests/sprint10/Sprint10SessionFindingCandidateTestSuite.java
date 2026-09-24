package io.acra.core.tests.sprint10;

import io.acra.core.active.evidence.EvidenceStage;
import io.acra.core.active.evidence.ExecutionEvidenceStore;
import io.acra.core.domain.finding.FindingCandidateState;
import io.acra.core.domain.identity.AuthenticationType;
import io.acra.core.recon.IdentityConfidenceState;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.session.AuthenticationSessionObservation;
import io.acra.core.session.S10SessionEvidenceValidator;
import io.acra.core.session.S10SessionFindingCandidateEvaluator;
import io.acra.core.session.S10SessionFindingRequest;
import io.acra.core.session.S10SessionSecurityAssessmentEvaluator;
import io.acra.core.session.SessionContextDimension;
import io.acra.core.session.SessionCorrelationResult;
import io.acra.core.session.SessionCorrelationState;
import io.acra.core.tests.TestSupport;
import java.time.Instant;
import java.util.List;

public final class Sprint10SessionFindingCandidateTestSuite {
    private static final String PROJECT = "project-s10-finding";
    private static final String EXECUTION = "execution-s10-finding";
    private static final String TEST = "test-s10-finding";
    private static final String EVIDENCE = "evidence-s10-finding";
    private static final String OBSERVATION = "observation-s10-finding";

    private Sprint10SessionFindingCandidateTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT10_SESSION_FINDING_CANDIDATE PASS assertions=" + assertions);
    }

    public static int run() {
        int assertions = 0;
        ExecutionEvidenceStore store = store();
        S10SessionEvidenceValidator provenance = new S10SessionEvidenceValidator(store);
        S10SessionFindingCandidateEvaluator findingEvaluator =
                new S10SessionFindingCandidateEvaluator(provenance);
        S10SessionSecurityAssessmentEvaluator assessmentEvaluator =
                new S10SessionSecurityAssessmentEvaluator();

        SessionCorrelationResult safeRotation = correlation(
                "c-safe",
                SessionCorrelationState.TOKEN_ROTATED,
                true,
                true,
                true,
                List.of(),
                List.of("TOKEN_ROTATED_CONTEXT_STABLE"));
        var safeAssessment = assessmentEvaluator.evaluate(
                safeRotation,
                provenance.validate(request(PROJECT).evidenceBinding()));
        var safeFinding = findingEvaluator.evaluate(
                safeAssessment,
                safeRotation,
                request(PROJECT));

        TestSupport.assertEquals(FindingCandidateState.REJECTED, safeFinding.state(),
                "verified context-preserving token rotation must be rejected as finding candidate");
        assertions++;
        TestSupport.assertContains(String.join(",", safeFinding.dimensions()), "TOKEN_ROTATION",
                "safe rotation remains visible as a finding dimension");
        assertions++;
        TestSupport.assertTrue(!safeFinding.rationale().isBlank(),
                "rejected safe rotation retains rationale");
        assertions++;

        SessionCorrelationResult drift = correlation(
                "c-drift",
                SessionCorrelationState.CONTEXT_DRIFT,
                true,
                true,
                true,
                List.of(SessionContextDimension.ROLE, SessionContextDimension.TENANT, SessionContextDimension.SCOPE),
                List.of("TOKEN_ROTATION_CONTEXT_DRIFT"));
        var driftAssessment = assessmentEvaluator.evaluate(
                drift,
                provenance.validate(request(PROJECT).evidenceBinding()));
        var driftFinding = findingEvaluator.evaluate(
                driftAssessment,
                drift,
                request(PROJECT));

        TestSupport.assertEquals(FindingCandidateState.CANDIDATE, driftFinding.state(),
                "verified provenance-backed session context drift becomes a review-only finding candidate");
        assertions++;
        TestSupport.assertContains(String.join(",", driftFinding.dimensions()), "SESSION_CONTEXT_ROLE",
                "role drift is preserved in finding dimensions");
        assertions++;
        TestSupport.assertContains(String.join(",", driftFinding.dimensions()), "SESSION_CONTEXT_TENANT",
                "tenant drift is preserved in finding dimensions");
        assertions++;
        TestSupport.assertContains(String.join(",", driftFinding.dimensions()), "SESSION_CONTEXT_SCOPE",
                "scope drift is preserved in finding dimensions");
        assertions++;
        TestSupport.assertContains(driftFinding.rationale(), "not an automatically confirmed vulnerability",
                "session finding candidate preserves review-only boundary");
        assertions++;
        TestSupport.assertEquals("SEC-003", driftFinding.policyReferences().getFirst(),
                "explicit repository rule reference is retained");
        assertions++;

        var crossProjectFinding = findingEvaluator.evaluate(
                driftAssessment,
                drift,
                request("other-project"));
        TestSupport.assertEquals(FindingCandidateState.INCONCLUSIVE, crossProjectFinding.state(),
                "cross-project session finding provenance must fail closed");
        assertions++;
        TestSupport.assertContains(String.join(",", crossProjectFinding.contradictoryEvidence()),
                "CROSS_PROJECT_REFERENCE",
                "cross-project finding contradiction remains explicit");
        assertions++;

        S10SessionFindingRequest mismatched = new S10SessionFindingRequest(
                PROJECT,
                TEST,
                EXECUTION,
                OBSERVATION,
                "/api/v1/s10/session-context",
                "user-a",
                "tenant-a",
                "SEC-003",
                List.of(EVIDENCE));
        SessionCorrelationResult mismatchCorrelation = new SessionCorrelationResult(
                "c-mismatch",
                "other-session",
                "obs-previous",
                OBSERVATION,
                SessionCorrelationState.CONTEXT_DRIFT,
                true,
                true,
                true,
                List.of(SessionContextDimension.ROLE),
                List.of(EVIDENCE),
                List.of("SESSION_CONTEXT_DRIFT"));
        var mismatchAssessment = assessmentEvaluator.evaluate(
                mismatchCorrelation,
                provenance.validate(mismatched.evidenceBinding()));
        var mismatchFinding = findingEvaluator.evaluate(
                mismatchAssessment,
                drift,
                mismatched);
        TestSupport.assertEquals(FindingCandidateState.INCONCLUSIVE, mismatchFinding.state(),
                "assessment/correlation attribution mismatch must fail closed");
        assertions++;
        TestSupport.assertContains(String.join(",", mismatchFinding.contradictoryEvidence()),
                "SESSION_FINDING_REQUEST_MISMATCH",
                "finding attribution mismatch remains explicit");
        assertions++;

        var deterministicA = findingEvaluator.evaluate(driftAssessment, drift, request(PROJECT));
        var deterministicB = findingEvaluator.evaluate(driftAssessment, drift, request(PROJECT));
        TestSupport.assertEquals(deterministicA.candidateId(), deterministicB.candidateId(),
                "session finding candidate identifier is deterministic");
        assertions++;
        TestSupport.assertEquals(deterministicA.fingerprint().fingerprint(),
                deterministicB.fingerprint().fingerprint(),
                "session finding fingerprint is deterministic");
        assertions++;

        return assertions;
    }

    private static ExecutionEvidenceStore store() {
        ExecutionEvidenceStore store = new ExecutionEvidenceStore(PROJECT);
        store.append(EXECUTION, TEST, EvidenceStage.TEST, EVIDENCE, "s10-session-finding-evidence");
        store.append(EXECUTION, TEST, EvidenceStage.OBSERVATION, OBSERVATION,
                new AuthenticationSessionObservation(
                        OBSERVATION,
                        "session-a",
                        TokenFingerprint.sha256("token-current"),
                        "user-a",
                        "admin",
                        "tenant-b",
                        List.of("admin.write", "profile.read"),
                        AuthenticationType.OAUTH,
                        IdentityConfidenceState.USER_CONFIRMED,
                        Instant.parse("2026-09-24T17:30:00Z"),
                        List.of(EVIDENCE)));
        return store;
    }

    private static S10SessionFindingRequest request(String project) {
        return new S10SessionFindingRequest(
                project,
                TEST,
                EXECUTION,
                OBSERVATION,
                "/api/v1/s10/session-context",
                "user-a",
                "tenant-a",
                "SEC-003",
                List.of(EVIDENCE));
    }

    private static SessionCorrelationResult correlation(
            String id,
            SessionCorrelationState state,
            boolean tokenRotated,
            boolean previousVerified,
            boolean currentVerified,
            List<SessionContextDimension> drift,
            List<String> reasons) {
        return new SessionCorrelationResult(
                id,
                "session-a",
                "obs-previous",
                OBSERVATION,
                state,
                previousVerified,
                currentVerified,
                tokenRotated,
                drift,
                List.of(EVIDENCE),
                reasons);
    }
}
