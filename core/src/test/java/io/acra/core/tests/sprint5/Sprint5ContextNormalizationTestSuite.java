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
import io.acra.core.domain.authorization.AuthorizationContextNormalizationResult;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.ContextStatus;
import io.acra.core.domain.http.HttpProtocol;
import io.acra.core.domain.http.HttpResponse;
import io.acra.core.engine.AuthorizationContextNormalizer;
import io.acra.core.recon.SecurityContextFingerprint;
import io.acra.core.tests.TestSupport;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class Sprint5ContextNormalizationTestSuite {
    private static final String PROJECT_ID = "project-a";
    private static final String EXECUTION_ID = "execution-1";
    private static final String TEST_ID = "test-1";
    private static final String OBSERVATION_ID = "observation-1";
    private static final String EVIDENCE_ID = "evidence-1";

    private Sprint5ContextNormalizationTestSuite() { }

    public static void main(String[] args) {
        int assertions = run();
        System.out.println("SPRINT5_CONTEXT_NORMALIZATION PASS assertions=" + assertions);
    }

    public static int run() {
        int assertions = 0;
        ExecutionEvidenceStore store = store(observation(context("user-b", "viewer", "tenant-b",
                "document-1", "user-a", "READ")));
        AuthorizationContextNormalizer normalizer = new AuthorizationContextNormalizer(
                new EvidenceReferenceValidator(store));

        AuthorizationContextNormalizationResult result = normalizer.normalizeTarget(
                store.observations().getFirst(), PROJECT_ID);
        TestSupport.assertTrue(result.valid(), "verified observation should normalize");
        TestSupport.assertEquals(ContextStatus.RESOLVED, result.context().status(),
                "complete target fingerprint resolves authorization context");
        TestSupport.assertEquals("user-b", result.context().principal().principalId(),
                "target principal retained");
        TestSupport.assertEquals("tenant-b", result.context().tenant().tenantId(),
                "target tenant retained");
        TestSupport.assertEquals("document-1", result.context().resource().resourceId(),
                "target resource retained");
        TestSupport.assertEquals("user-a", result.context().ownerPrincipalId(),
                "resource owner retained");
        TestSupport.assertEquals(AuthorizationDecision.DENY, result.context().expectedDecision(),
                "expected decision comes from verified observation");
        TestSupport.assertEquals(AuthorizationDecision.ALLOW, result.context().observedDecision(),
                "observed outcome normalized to authorization decision");
        assertions += 8;

        AuthorizationContextNormalizationResult wrongProject = normalizer.normalizeTarget(
                store.observations().getFirst(), "project-b");
        TestSupport.assertFalse(wrongProject.valid(), "cross-project observation must be rejected");
        assertions++;

        AuthorizationContextNormalizationResult noValidator = new AuthorizationContextNormalizer(null)
                .normalizeTarget(store.observations().getFirst(), PROJECT_ID);
        TestSupport.assertFalse(noValidator.valid(), "normalization must fail closed without evidence validator");
        assertions++;

        ExecutionEvidenceStore partialStore = store(observation(context("user-b", "UNKNOWN", "tenant-b",
                "document-1", "user-a", "READ")));
        AuthorizationContextNormalizationResult partial = new AuthorizationContextNormalizer(
                new EvidenceReferenceValidator(partialStore)).normalizeTarget(partialStore.observations().getFirst(), PROJECT_ID);
        TestSupport.assertTrue(partial.valid(), "unknown role does not invalidate evidence-bound context");
        TestSupport.assertEquals(ContextStatus.RESOLVED, partial.context().status(),
                "role is optional for object-level context resolution");
        TestSupport.assertEquals(null, partial.context().role(), "unknown role remains absent rather than fabricated");
        assertions += 3;
        return assertions;
    }

    private static ExecutionEvidenceStore store(Observation observation) {
        ExecutionEvidenceStore store = new ExecutionEvidenceStore(PROJECT_ID);
        store.append(EXECUTION_ID, TEST_ID, EvidenceStage.TEST, EVIDENCE_ID, "policy-evidence");
        store.append(EXECUTION_ID, TEST_ID, EvidenceStage.OBSERVATION, OBSERVATION_ID, observation);
        return store;
    }

    private static Observation observation(SecurityContextFingerprint target) {
        ResponseSnapshot response = responseSnapshot();
        return new Observation(OBSERVATION_ID, TEST_ID, response, response, response, response,
                new ExpectedDecisionResolution(AuthorizationDecision.DENY,
                        ExpectedDecisionSource.EXPLICIT_CONFIGURED_POLICY, "policy-1",
                        List.of(EVIDENCE_ID), 1.0, false), AuthorizationOutcome.ALLOW,
                new MultiWayDifferential(AuthorizationOutcome.ALLOW, AuthorizationOutcome.ALLOW,
                        AuthorizationOutcome.DENY, AuthorizationOutcome.ALLOW, List.of(),
                        DifferentialClassification.UNEXPECTED_CHANGE, List.of()),
                context("user-a", "viewer", "tenant-a", "document-1", "user-a", "READ"),
                target, List.of(EVIDENCE_ID), 1.0,
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

    private static SecurityContextFingerprint context(String principal, String role, String tenant,
                                                      String resource, String owner, String action) {
        return new SecurityContextFingerprint(principal, role, tenant, resource, owner, action,
                "DRAFT", "GET", "/documents/{id}", "token-fingerprint",
                AuthorizationDecision.DENY, AuthorizationDecision.ALLOW, List.of(EVIDENCE_ID));
    }
}
