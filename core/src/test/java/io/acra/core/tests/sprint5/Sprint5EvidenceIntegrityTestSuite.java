package io.acra.core.tests.sprint5;

import io.acra.core.active.evidence.EvidenceReferenceValidation;
import io.acra.core.active.evidence.EvidenceReferenceValidator;
import io.acra.core.active.evidence.EvidenceStage;
import io.acra.core.active.evidence.ExecutionEvidenceStore;
import io.acra.core.domain.authorization.AuthorizationAssessmentAggregate;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.BolaAssessment;
import io.acra.core.domain.authorization.BolaAssessmentStatus;
import io.acra.core.domain.authorization.BolaConfidence;
import io.acra.core.engine.AuthorizationAssessmentCorrelator;
import io.acra.core.serialization.DomainSerializer;
import io.acra.core.tests.TestSupport;

import java.util.ArrayList;
import java.util.List;

public final class Sprint5EvidenceIntegrityTestSuite {
	private Sprint5EvidenceIntegrityTestSuite() { }

	public static void main(String[] args) {
		int assertions = run();
		System.out.println("SPRINT5_EVIDENCE_INTEGRITY PASS assertions=" + assertions);
	}

	public static int run() {
		int assertions = 0;
		ExecutionEvidenceStore store = new ExecutionEvidenceStore("project-a");
		store.append("execution-a", "test-a", EvidenceStage.TEST, "evidence-a", "historical-a");
		store.append("execution-b", "test-b", EvidenceStage.TEST, "evidence-b", "replay-b");
		EvidenceReferenceValidator validator = new EvidenceReferenceValidator(store);

		assertions += accepted(validator.validateEvidenceReferences(List.of("evidence-a"),
				"execution-a", "test-a", "project-a"), "matching evidence and provenance");
		assertions += rejected(new EvidenceReferenceValidator(null)
				.validateEvidenceReferences(List.of("evidence-a"), "execution-a", "test-a", "project-a"), "missing store");
		for (String reference : List.of("", "  ", "UNKNOWN", "<REDACTED>", "missing")) {
			assertions += rejected(validator.validateEvidenceReferences(List.of(reference),
					"execution-a", "test-a", "project-a"), "invalid evidence reference");
		}
		assertions += rejected(validator.validateEvidenceReferences(List.of(),
				"execution-a", "test-a", "project-a"), "missing evidence ID");
		assertions += rejected(validator.validateEvidenceReferences(List.of("evidence-a"),
				"execution-b", "test-a", "project-a"), "wrong execution ownership");
		assertions += rejected(validator.validateEvidenceReferences(List.of("evidence-a"),
				"execution-a", "test-b", "project-a"), "wrong test ownership");
		assertions += rejected(validator.validateEvidenceReferences(List.of("evidence-a"),
				"execution-a", "test-a", "project-b"), "cross-project reference");
		assertions += rejected(validator.validateObservation("missing-observation", "execution-a", "test-a", "project-a"),
				"unknown observation reference");

		List<String> original = new ArrayList<>(List.of("evidence-a"));
		EvidenceReferenceValidation result = validator.validateEvidenceReferences(original,
				"execution-a", "test-a", "project-a");
		original.add("changed-locally");
		TestSupport.assertEquals(List.of("evidence-a"), result.valid() ? List.of("evidence-a") : List.of(),
				"validation result does not retain mutable input");
		TestSupport.assertEquals("historical-a", store.object("evidence-a"), "historical evidence unchanged");
		assertions += 2;

		assertions += accepted(validator.validateEvidenceReferences(List.of("evidence-b"),
				"execution-b", "test-b", "project-a"), "replay remains separate");
		assertions += rejected(validator.validateEvidenceReferences(List.of("evidence-a"),
				"execution-b", "test-b", "project-a"), "replay cannot cross lineage");

		BolaAssessment assessment = new BolaAssessment("assessment-a", "observation-a", "execution-a", "test-a",
				"user-a", "resource-a", "owner-a", "READ", AuthorizationDecision.DENY,
				AuthorizationDecision.ALLOW, BolaAssessmentStatus.BOLA_CANDIDATE, BolaConfidence.HIGH,
				List.of("evidence-a"), "safe rationale");
		AuthorizationAssessmentAggregate aggregate = AuthorizationAssessmentCorrelator.correlate(
				List.of(assessment), List.of(), validator, "project-a");
		TestSupport.assertEquals("INSUFFICIENT", aggregate.confidence(), "invalid observation binding cannot increase confidence");
		TestSupport.assertEquals("INSUFFICIENT", aggregate.state().name(), "invalid observation binding cannot create valid correlation");
		assertions += 2;

		String serialized = new DomainSerializer().serialize(new BolaAssessment("safe", "obs", "exec", "test",
				"password=DummyPassword", "token=DummyToken", "api_key=DummyKey", "Authorization: Bearer DummyBearer",
				AuthorizationDecision.DENY, AuthorizationDecision.ALLOW, BolaAssessmentStatus.BOLA_CANDIDATE,
				BolaConfidence.HIGH, List.of("Cookie: sid=DummyCookie", "session_secret=DummySession"),
				"safe result"));
		for (String secret : List.of("DummyPassword", "DummyToken", "DummyKey", "DummyBearer", "DummyCookie", "DummySession")) {
			TestSupport.assertNotContains(serialized, secret, "security value is not serialized: " + secret);
			assertions++;
		}
		return assertions;
	}

	private static int accepted(EvidenceReferenceValidation result, String message) {
		TestSupport.assertTrue(result.valid(), message + " should be accepted: " + result.reasons());
		return 1;
	}

	private static int rejected(EvidenceReferenceValidation result, String message) {
		TestSupport.assertFalse(result.valid(), message + " should be rejected");
		return 1;
	}
}