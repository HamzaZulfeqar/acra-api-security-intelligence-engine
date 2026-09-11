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
import io.acra.core.domain.authorization.PolicyValidationResult;
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
import io.acra.core.recon.SecurityContextFingerprint;
import io.acra.core.serialization.DomainSerializer;
import io.acra.core.tests.TestSupport;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class Sprint5PolicyValidationTestSuite {
	private static final String PROJECT_ID = "project-a";
	private static final String EXECUTION_ID = "execution-1";
	private static final String TEST_ID = "test-1";
	private static final String OBSERVATION_ID = "observation-1";
	private static final String EVIDENCE_ID = "evidence-1";

	private Sprint5PolicyValidationTestSuite() { }

	public static void main(String[] args) {
		int assertions = run();
		System.out.println("SPRINT5_POLICY_VALIDATION PASS assertions=" + assertions);
	}

	public static int run() {
		ExecutionEvidenceStore store = new ExecutionEvidenceStore(PROJECT_ID);
		store.append(EXECUTION_ID, TEST_ID, EvidenceStage.TEST, EVIDENCE_ID, "tenant-policy");
		store.append(EXECUTION_ID, TEST_ID, EvidenceStage.OBSERVATION, OBSERVATION_ID, observation());
		PolicyValidationEvaluator evaluator = new PolicyValidationEvaluator(new EvidenceReferenceValidator(store));
		return tenantPolicyValidation(evaluator) + forgedObservationRejection(evaluator)
				+ securityAndImmutability(evaluator);
	}

	private static int tenantPolicyValidation(PolicyValidationEvaluator evaluator) {
		AuthorizationContext context = context(AuthorizationDecision.ALLOW, AuthorizationDecision.ALLOW,
				ContextStatus.RESOLVED);
		PolicyValidationResult result = evaluator.reviewTenant(context, policy("tenant-policy", "source"),
				OBSERVATION_ID, EXECUTION_ID, TEST_ID, PROJECT_ID);
		TestSupport.assertEquals(PolicyValidationState.ALLOWED, result.state(),
				"valid observation and tenant policy should be allowed");
		return 1;
	}

	private static int forgedObservationRejection(PolicyValidationEvaluator evaluator) {
		AuthorizationContext context = context(AuthorizationDecision.ALLOW, AuthorizationDecision.ALLOW,
				ContextStatus.RESOLVED);
		PolicyValidationResult result = evaluator.reviewTenant(context, policy("tenant-policy", "source"),
				"forged-observation", EXECUTION_ID, TEST_ID, PROJECT_ID);
		TestSupport.assertEquals(PolicyValidationState.INCONCLUSIVE, result.state(),
				"forged observation reference must remain inconclusive");
		return 1;
	}

	private static int securityAndImmutability(PolicyValidationEvaluator evaluator) {
		AuthorizationContext context = context(AuthorizationDecision.ALLOW, AuthorizationDecision.ALLOW,
				ContextStatus.RESOLVED);
		PolicyValidationEvaluator.TenantPolicy policy = new PolicyValidationEvaluator.TenantPolicy(
				"safe-policy", "password=DummyPassword", "tenant-a", "tenant-a", "viewer", false, false,
				AuthorizationDecision.ALLOW, List.of(EVIDENCE_ID));
		PolicyValidationResult result = evaluator.reviewTenant(context, policy, OBSERVATION_ID,
				EXECUTION_ID, TEST_ID, PROJECT_ID);
		TestSupport.assertEquals(List.of(EVIDENCE_ID), result.evidenceIds(), "evidence IDs remain immutable");
		TestSupport.assertNotContains(new DomainSerializer().serialize(result), "DummyPassword",
				"policy secrets are not serialized");
		return 2;
	}

	private static PolicyValidationEvaluator.TenantPolicy policy(String reference, String source) {
		return new PolicyValidationEvaluator.TenantPolicy(reference, source, "tenant-a", "tenant-a", "viewer",
				false, false, AuthorizationDecision.ALLOW, List.of(EVIDENCE_ID));
	}

	private static Observation observation() {
		ResponseSnapshot response = responseSnapshot();
		return new Observation(OBSERVATION_ID, TEST_ID, response, response, response, response,
				new ExpectedDecisionResolution(AuthorizationDecision.ALLOW,
						ExpectedDecisionSource.EXPLICIT_CONFIGURED_POLICY, "tenant-policy",
						List.of(EVIDENCE_ID), 1.0, false), AuthorizationOutcome.ALLOW,
				new MultiWayDifferential(AuthorizationOutcome.ALLOW, AuthorizationOutcome.ALLOW,
						AuthorizationOutcome.DENY, AuthorizationOutcome.ALLOW, List.of(),
						DifferentialClassification.NO_CHANGE, List.of()), securityContext(), securityContext(),
				List.of(EVIDENCE_ID), 1.0,
				new ExecutionFingerprint(EXECUTION_ID, TEST_ID, "request-fingerprint", "response-fingerprint",
						"configuration-fingerprint", "environment-fingerprint"),
				Instant.parse("2026-09-11T00:00:00Z"));
	}

	private static ResponseSnapshot responseSnapshot() {
		HttpResponse response = new HttpResponse(200, List.of(), "{}".getBytes(), "application/json",
				HttpProtocol.HTTP_1_1, new byte[0]);
		return new ResponseSnapshot("response-1", "request-1", response, Map.of(), Duration.ZERO,
				Instant.parse("2026-09-11T00:00:00Z"), new ResponseSemanticAnalyzer().fingerprint(response), "");
	}

	private static SecurityContextFingerprint securityContext() {
		return new SecurityContextFingerprint("user-a", "viewer", "tenant-a", "document-1", "user-a", "READ",
				"DRAFT", "GET", "/documents", "token-fingerprint", AuthorizationDecision.ALLOW,
				AuthorizationDecision.ALLOW, List.of(EVIDENCE_ID));
	}

	private static AuthorizationContext context(AuthorizationDecision expected, AuthorizationDecision observed,
												ContextStatus status) {
		Principal principal = new Principal("user-a", "User A", AuthenticationType.UNKNOWN, Confidence.unknown());
		Role role = new Role("viewer", "viewer", EvidenceSource.UNKNOWN, Confidence.unknown());
		Tenant tenant = new Tenant("tenant-a", "tenant-a", EvidenceSource.UNKNOWN, Confidence.unknown());
		Resource resource = new Resource("document-1", "document", null, "user-a", "tenant-a", "ACTIVE",
				Confidence.unknown());
		Action action = new Action(ActionType.READ, EvidenceSource.UNKNOWN, Confidence.unknown(), "READ");
		return new AuthorizationContext(principal, role, tenant, resource, "user-a", action,
				new WorkflowState("DRAFT", Confidence.unknown()), expected, observed, List.of(EVIDENCE_ID), status);
	}
}
