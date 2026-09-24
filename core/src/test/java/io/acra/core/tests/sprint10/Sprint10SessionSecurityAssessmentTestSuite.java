package io.acra.core.tests.sprint10;

import io.acra.core.active.evidence.EvidenceReferenceValidation;
import io.acra.core.session.S10SessionSecurityAssessmentEvaluator;
import io.acra.core.session.SessionContextDimension;
import io.acra.core.session.SessionCorrelationResult;
import io.acra.core.session.SessionCorrelationState;
import io.acra.core.session.SessionSecurityAssessmentState;
import io.acra.core.tests.TestSupport;
import java.util.List;

public final class Sprint10SessionSecurityAssessmentTestSuite {
    private Sprint10SessionSecurityAssessmentTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT10_SESSION_SECURITY_ASSESSMENT PASS assertions=" + assertions);
    }

    public static int run() {
        S10SessionSecurityAssessmentEvaluator evaluator = new S10SessionSecurityAssessmentEvaluator();
        EvidenceReferenceValidation valid = EvidenceReferenceValidation.accepted();
        int assertions = 0;

        var baseline = evaluator.evaluate(correlation(
                "c-baseline", SessionCorrelationState.BASELINE, false,
                false, true, List.of(), List.of("FIRST_VERIFIED_SESSION_OBSERVATION")), valid);
        TestSupport.assertEquals(SessionSecurityAssessmentState.BASELINE, baseline.state(),
                "first observation remains baseline-only");
        assertions++;
        TestSupport.assertTrue(!baseline.reviewCandidate(),
                "baseline cannot become review candidate");
        assertions++;

        var stable = evaluator.evaluate(correlation(
                "c-stable", SessionCorrelationState.STABLE, false,
                true, true, List.of(), List.of("SESSION_CONTEXT_STABLE")), valid);
        TestSupport.assertEquals(SessionSecurityAssessmentState.STABLE, stable.state(),
                "stable verified session remains stable");
        assertions++;

        var rotation = evaluator.evaluate(correlation(
                "c-rotation", SessionCorrelationState.TOKEN_ROTATED, true,
                true, true, List.of(), List.of("TOKEN_ROTATED_CONTEXT_STABLE")), valid);
        TestSupport.assertEquals(SessionSecurityAssessmentState.SAFE_ROTATION, rotation.state(),
                "verified context-preserving token rotation remains safe rotation");
        assertions++;
        TestSupport.assertTrue(!rotation.reviewCandidate(),
                "safe rotation cannot become review candidate");
        assertions++;

        var drift = evaluator.evaluate(correlation(
                "c-drift", SessionCorrelationState.CONTEXT_DRIFT, true,
                true, true,
                List.of(SessionContextDimension.ROLE, SessionContextDimension.TENANT, SessionContextDimension.SCOPE),
                List.of("TOKEN_ROTATION_CONTEXT_DRIFT")), valid);
        TestSupport.assertEquals(SessionSecurityAssessmentState.CANDIDATE, drift.state(),
                "provenance-valid verified context drift becomes a review candidate");
        assertions++;
        TestSupport.assertTrue(drift.reviewCandidate(),
                "verified context drift exposes explicit review-candidate state");
        assertions++;
        TestSupport.assertContains(drift.rationale(), "not an automatically confirmed vulnerability",
                "candidate rationale preserves review-only boundary");
        assertions++;
        TestSupport.assertEquals(
                List.of(SessionContextDimension.ROLE, SessionContextDimension.TENANT, SessionContextDimension.SCOPE),
                drift.driftDimensions(),
                "assessment preserves exact drift dimensions");
        assertions++;

        var unverifiedDrift = evaluator.evaluate(correlation(
                "c-unverified", SessionCorrelationState.CONTEXT_DRIFT, true,
                true, false,
                List.of(SessionContextDimension.ROLE),
                List.of("CURRENT_IDENTITY_UNVERIFIED")), valid);
        TestSupport.assertEquals(SessionSecurityAssessmentState.INCONCLUSIVE, unverifiedDrift.state(),
                "context drift with unverified identity cannot be promoted");
        assertions++;
        TestSupport.assertTrue(!unverifiedDrift.reviewCandidate(),
                "unverified drift is not a review candidate");
        assertions++;

        var identityUnknown = evaluator.evaluate(correlation(
                "c-unknown", SessionCorrelationState.UNVERIFIED_IDENTITY, true,
                true, false,
                List.of(SessionContextDimension.PRINCIPAL),
                List.of("TOKEN_FINGERPRINT_DOES_NOT_VERIFY_PRINCIPAL")), valid);
        TestSupport.assertEquals(SessionSecurityAssessmentState.INCONCLUSIVE, identityUnknown.state(),
                "unverified identity remains inconclusive");
        assertions++;

        var invalidProvenance = evaluator.evaluate(correlation(
                "c-invalid-provenance", SessionCorrelationState.CONTEXT_DRIFT, true,
                true, true,
                List.of(SessionContextDimension.ROLE),
                List.of("TOKEN_ROTATION_CONTEXT_DRIFT")),
                EvidenceReferenceValidation.rejected(List.of("CROSS_PROJECT_REFERENCE")));
        TestSupport.assertEquals(SessionSecurityAssessmentState.INCONCLUSIVE, invalidProvenance.state(),
                "invalid provenance blocks candidate promotion");
        assertions++;
        TestSupport.assertContains(String.join(",", invalidProvenance.reasons()), "CROSS_PROJECT_REFERENCE",
                "provenance failure reason remains explicit");
        assertions++;

        var missingProvenance = evaluator.evaluate(correlation(
                "c-missing-provenance", SessionCorrelationState.CONTEXT_DRIFT, true,
                true, true,
                List.of(SessionContextDimension.ROLE),
                List.of("SESSION_CONTEXT_DRIFT")), null);
        TestSupport.assertEquals(SessionSecurityAssessmentState.INCONCLUSIVE, missingProvenance.state(),
                "missing provenance validation blocks candidate promotion");
        assertions++;
        TestSupport.assertContains(String.join(",", missingProvenance.reasons()),
                "SESSION_PROVENANCE_VALIDATION_MISSING",
                "missing provenance reason remains explicit");
        assertions++;

        var deterministicA = evaluator.evaluate(correlation(
                "c-deterministic", SessionCorrelationState.CONTEXT_DRIFT, true,
                true, true,
                List.of(SessionContextDimension.ROLE),
                List.of("SESSION_CONTEXT_DRIFT")), valid);
        var deterministicB = evaluator.evaluate(correlation(
                "c-deterministic", SessionCorrelationState.CONTEXT_DRIFT, true,
                true, true,
                List.of(SessionContextDimension.ROLE),
                List.of("SESSION_CONTEXT_DRIFT")), valid);
        TestSupport.assertEquals(deterministicA.assessmentId(), deterministicB.assessmentId(),
                "assessment identifiers are deterministic");
        assertions++;

        return assertions;
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
                "obs-current",
                state,
                previousVerified,
                currentVerified,
                tokenRotated,
                drift,
                List.of("evidence-a", "evidence-b"),
                reasons);
    }
}
