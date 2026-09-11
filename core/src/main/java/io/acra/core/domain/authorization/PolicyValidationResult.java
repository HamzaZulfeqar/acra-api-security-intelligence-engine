package io.acra.core.domain.authorization;

import io.acra.core.security.UniversalRedactor;
import java.util.List;

public record PolicyValidationResult(
		String reviewId,
		String policyReference,
		String policySource,
		AuthorizationContext context,
		AuthorizationDecision expectedDecision,
		AuthorizationDecision observedDecision,
		List<String> evidenceIds,
		String observationId,
		String executionId,
		String testId,
		PolicyValidationState state,
		String confidence,
		String rationale,
		List<String> reasons) {
	private static final UniversalRedactor REDACTOR = new UniversalRedactor();

	public PolicyValidationResult {
		reviewId = safe(reviewId);
		policyReference = safe(policyReference);
		policySource = safe(policySource);
		expectedDecision = expectedDecision == null ? AuthorizationDecision.UNKNOWN : expectedDecision;
		observedDecision = observedDecision == null ? AuthorizationDecision.UNKNOWN : observedDecision;
		evidenceIds = safe(evidenceIds);
		observationId = safe(observationId);
		executionId = safe(executionId);
		testId = safe(testId);
		state = state == null ? PolicyValidationState.INCONCLUSIVE : state;
		confidence = safe(confidence);
		rationale = safe(rationale);
		reasons = safe(reasons);
	}

	private static String safe(String value) {
		return REDACTOR.redactText(value == null ? "" : value);
	}

	private static List<String> safe(List<String> values) {
		return List.copyOf(values == null ? List.<String>of() : values).stream()
				.map(PolicyValidationResult::safe).distinct().sorted().toList();
	}
}
