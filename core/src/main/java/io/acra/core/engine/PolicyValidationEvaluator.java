package io.acra.core.engine;

import io.acra.core.active.evidence.EvidenceReferenceValidation;
import io.acra.core.active.evidence.EvidenceReferenceValidator;
import io.acra.core.domain.authorization.AuthorizationContext;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.PolicyValidationResult;
import io.acra.core.domain.authorization.PolicyValidationState;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.security.UniversalRedactor;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class PolicyValidationEvaluator {
	private static final UniversalRedactor REDACTOR = new UniversalRedactor();
	private final EvidenceReferenceValidator evidenceValidator;

	public PolicyValidationEvaluator(EvidenceReferenceValidator evidenceValidator) {
		this.evidenceValidator = evidenceValidator;
	}

	public PolicyValidationResult reviewTenant(AuthorizationContext context, TenantPolicy policy,
											   String observationId, String executionId, String testId,
											   String projectId) {
		List<String> reasons = new ArrayList<>();
		if (context == null || policy == null) return inconclusive(policyReference(policy), policySource(policy), context,
				AuthorizationDecision.UNKNOWN, AuthorizationDecision.UNKNOWN, List.of(), observationId,
				executionId, testId, List.of("POLICY_OR_CONTEXT_MISSING"));
		if (context.status() == io.acra.core.domain.authorization.ContextStatus.CONFLICTING_EVIDENCE) {
			reasons.add("CONFLICTING_TENANT_EVIDENCE");
		}
		if (unknown(context.tenant() == null ? null : context.tenant().tenantId())
				|| context.resource() == null || unknown(context.resource().tenantId())) {
			reasons.add("TENANT_CONTEXT_UNKNOWN");
		}
		String contextTenant = context.tenant() == null ? "UNKNOWN" : context.tenant().tenantId();
		String resourceTenant = context.resource() == null ? "UNKNOWN" : context.resource().tenantId();
		if (!unknown(contextTenant) && !unknown(resourceTenant)
				&& (!policy.subjectTenantId().equals(contextTenant)
				|| !policy.resourceTenantId().equals(resourceTenant))) {
			reasons.add("TENANT_POLICY_CONTEXT_MISMATCH");
		}
		if (!unknown(policy.roleId()) && (context.role() == null || !policy.roleId().equals(context.role().roleId()))) {
			reasons.add("TENANT_ROLE_MISMATCH");
		}
		boolean crossTenant = !policy.subjectTenantId().equals(policy.resourceTenantId());
		if (crossTenant && policy.expectedDecision() == AuthorizationDecision.ALLOW
				&& !policy.globalAdministrator() && !policy.delegatedTenantPrivilege()) {
			reasons.add("CROSS_TENANT_ALLOW_UNSUPPORTED");
		}
		if (!binary(policy.expectedDecision()) || !binary(context.observedDecision())) {
			reasons.add("NON_BINARY_DECISION");
		}
		if (binary(context.expectedDecision()) && context.expectedDecision() != policy.expectedDecision()) {
			reasons.add("POLICY_DECISION_CONFLICT");
		}
		addEvidenceFailure(reasons, policy.evidenceIds(), observationId, executionId, testId, projectId);
		if (!reasons.isEmpty()) {
			return result(policy, context, observationId, executionId, testId,
					policy.expectedDecision(), context.observedDecision(), policyValidationState(reasons, policy.expectedDecision()),
					"INSUFFICIENT", reasons);
		}
		return result(policy, context, observationId, executionId, testId, policy.expectedDecision(),
				context.observedDecision(), decisionState(policy.expectedDecision(), context.observedDecision()),
				"HIGH", List.of());
	}

	public PolicyValidationResult reviewWorkflow(AuthorizationContext context, WorkflowPolicy policy,
												 String currentState, String requestedState,
												 boolean approvalProvided, boolean roleSeparationSatisfied,
												 String observationId, String executionId, String testId,
												 String projectId) {
		List<String> reasons = new ArrayList<>();
		if (context == null || policy == null) return inconclusive(policyReference(policy), policySource(policy), context,
				AuthorizationDecision.UNKNOWN, AuthorizationDecision.UNKNOWN, List.of(), observationId,
				executionId, testId, List.of("POLICY_OR_CONTEXT_MISSING"));
		if (unknown(currentState) || unknown(requestedState) || unknown(policy.fromState()) || unknown(policy.toState())) {
			reasons.add("WORKFLOW_STATE_UNKNOWN");
		}
		if (!policy.fromState().equals(currentState) || !policy.toState().equals(requestedState)) {
			reasons.add("WORKFLOW_TRANSITION_MISMATCH");
		}
		if (policy.requiredRole() != null && !unknown(policy.requiredRole())
				&& (context.role() == null || !policy.requiredRole().equals(context.role().roleId()))) {
			reasons.add("WORKFLOW_ROLE_MISMATCH");
		}
		if (policy.approvalRequired() && !approvalProvided) reasons.add("WORKFLOW_APPROVAL_MISSING");
		if (policy.roleSeparationRequired() && !roleSeparationSatisfied) reasons.add("WORKFLOW_ROLE_SEPARATION_MISSING");
		if (policy.terminalSource() && policy.expectedDecision() == AuthorizationDecision.ALLOW) {
			reasons.add("TERMINAL_WORKFLOW_ALLOW_CONFLICT");
		}
		if (!binary(policy.expectedDecision()) || !binary(context.observedDecision())) reasons.add("NON_BINARY_DECISION");
		if (binary(context.expectedDecision()) && context.expectedDecision() != policy.expectedDecision()) {
			reasons.add("POLICY_DECISION_CONFLICT");
		}
		addEvidenceFailure(reasons, policy.evidenceIds(), observationId, executionId, testId, projectId);
		if (!reasons.isEmpty()) return result(policy, context, observationId, executionId, testId,
				policy.expectedDecision(), context.observedDecision(), policyValidationState(reasons, policy.expectedDecision()),
				"INSUFFICIENT", reasons);
		return result(policy, context, observationId, executionId, testId, policy.expectedDecision(),
				context.observedDecision(), decisionState(policy.expectedDecision(), context.observedDecision()),
				"HIGH", List.of());
	}

	public PolicyValidationResult reviewProperty(AuthorizationContext context, PropertyPolicy policy,
												 String property, PropertyOperation operation, String endpoint,
												 String observationId, String executionId, String testId,
												 String projectId) {
		List<String> reasons = new ArrayList<>();
		if (context == null || policy == null) return inconclusive(policyReference(policy), policySource(policy), context,
				AuthorizationDecision.UNKNOWN, AuthorizationDecision.UNKNOWN, List.of(), observationId,
				executionId, testId, List.of("POLICY_OR_CONTEXT_MISSING"));
		if (unknown(property) || operation == null || unknown(endpoint)) reasons.add("PROPERTY_REFERENCE_UNKNOWN");
		if (!policy.property().equals(property) || policy.operation() != operation || !policy.endpoint().equals(endpoint)) {
			reasons.add("PROPERTY_POLICY_MISMATCH");
		}
		if (policy.roleId() != null && !unknown(policy.roleId())
				&& (context.role() == null || !policy.roleId().equals(context.role().roleId()))) {
			reasons.add("PROPERTY_ROLE_MISMATCH");
		}
		if (policy.tenantId() != null && !unknown(policy.tenantId())
				&& (context.tenant() == null || !policy.tenantId().equals(context.tenant().tenantId()))) {
			reasons.add("PROPERTY_TENANT_MISMATCH");
		}
		if (!binary(policy.expectedDecision()) || !binary(context.observedDecision())) reasons.add("NON_BINARY_DECISION");
		if (binary(context.expectedDecision()) && context.expectedDecision() != policy.expectedDecision()) {
			reasons.add("POLICY_DECISION_CONFLICT");
		}
		addEvidenceFailure(reasons, policy.evidenceIds(), observationId, executionId, testId, projectId);
		if (!reasons.isEmpty()) return result(policy, context, observationId, executionId, testId,
				policy.expectedDecision(), context.observedDecision(), policyValidationState(reasons, policy.expectedDecision()),
				"INSUFFICIENT", reasons);
		return result(policy, context, observationId, executionId, testId, policy.expectedDecision(),
				context.observedDecision(), decisionState(policy.expectedDecision(), context.observedDecision()),
				"HIGH", List.of());
	}

	public PolicyValidationResult reviewConflict(List<PolicyValidationResult> reviews) {
		if (reviews == null || reviews.isEmpty()) {
			return inconclusive("", "", null, AuthorizationDecision.UNKNOWN, AuthorizationDecision.UNKNOWN,
					List.of(), "", "", "", List.of("POLICY_REVIEW_MISSING"));
		}
		List<PolicyValidationResult> ordered = reviews.stream().filter(java.util.Objects::nonNull)
				.sorted(java.util.Comparator.comparing(PolicyValidationResult::reviewId)).toList();
		if (ordered.isEmpty()) return inconclusive("", "", null, AuthorizationDecision.UNKNOWN,
				AuthorizationDecision.UNKNOWN, List.of(), "", "", "", List.of("POLICY_REVIEW_MISSING"));
		Set<PolicyValidationState> states = new LinkedHashSet<>();
		List<String> reasons = new ArrayList<>();
		Set<AuthorizationDecision> decisions = new LinkedHashSet<>();
		for (PolicyValidationResult review : ordered) {
			states.add(review.state());
			decisions.add(review.expectedDecision());
			reasons.addAll(review.reasons());
		}
		if (states.contains(PolicyValidationState.CONFLICTING)
				|| (states.contains(PolicyValidationState.ALLOWED) && states.contains(PolicyValidationState.DENIED))
				|| (decisions.contains(AuthorizationDecision.ALLOW) && decisions.contains(AuthorizationDecision.DENY))) {
			reasons.add("POLICY_DECISION_CONFLICT");
			return conflict(ordered, reasons);
		}
		if (states.contains(PolicyValidationState.INCONCLUSIVE)) {
			reasons.add("POLICY_REVIEW_INCONCLUSIVE");
			return conflictResult(ordered, PolicyValidationState.INCONCLUSIVE, "INSUFFICIENT", reasons);
		}
		return conflictResult(ordered, ordered.getFirst().state(), ordered.getFirst().confidence(), reasons);
	}

	public record TenantPolicy(String policyReference, String policySource, String subjectTenantId,
							   String resourceTenantId, String roleId, boolean globalAdministrator,
							   boolean delegatedTenantPrivilege, AuthorizationDecision expectedDecision,
								  List<String> evidenceIds) implements Policy {
		public TenantPolicy {
			policyReference = required(policyReference, "policyReference");
			policySource = required(policySource, "policySource");
			subjectTenantId = normalized(subjectTenantId);
			resourceTenantId = normalized(resourceTenantId);
			roleId = normalized(roleId);
			expectedDecision = expectedDecision == null ? AuthorizationDecision.UNKNOWN : expectedDecision;
			evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
		}
	}

	public record WorkflowPolicy(String policyReference, String policySource, String fromState, String toState,
								 String requiredRole, boolean approvalRequired, boolean roleSeparationRequired,
								 boolean terminalSource, AuthorizationDecision expectedDecision,
								  List<String> evidenceIds) implements Policy {
		public WorkflowPolicy {
			policyReference = required(policyReference, "policyReference");
			policySource = required(policySource, "policySource");
			fromState = normalized(fromState);
			toState = normalized(toState);
			requiredRole = normalized(requiredRole);
			expectedDecision = expectedDecision == null ? AuthorizationDecision.UNKNOWN : expectedDecision;
			evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
		}
	}

	public enum PropertyOperation { READ, UPDATE }

	public record PropertyPolicy(String policyReference, String policySource, String endpoint, String property,
								 PropertyOperation operation, String roleId, String tenantId,
								  AuthorizationDecision expectedDecision, List<String> evidenceIds) implements Policy {
		public PropertyPolicy {
			policyReference = required(policyReference, "policyReference");
			policySource = required(policySource, "policySource");
			endpoint = normalized(endpoint);
			property = normalized(property);
			roleId = normalized(roleId);
			tenantId = normalized(tenantId);
			expectedDecision = expectedDecision == null ? AuthorizationDecision.UNKNOWN : expectedDecision;
			evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
		}
	}

	private void addEvidenceFailure(List<String> reasons, List<String> evidenceIds, String observationId,
									String executionId, String testId, String projectId) {
		if (evidenceValidator == null) {
			reasons.add("EVIDENCE_VALIDATION_UNAVAILABLE");
			return;
		}
		EvidenceReferenceValidation validation = evidenceValidator.validateEvidenceReferences(
				evidenceIds, executionId, testId, projectId);
		if (!validation.valid()) reasons.addAll(validation.reasons());
		if (unknown(observationId)) {
			reasons.add("OBSERVATION_REFERENCE_MISSING");
		} else {
			EvidenceReferenceValidation observationValidation = evidenceValidator.validateObservation(
					observationId, executionId, testId, projectId);
			if (!observationValidation.valid()) reasons.addAll(observationValidation.reasons());
		}
	}

	private PolicyValidationResult result(Policy policy, AuthorizationContext context, String observationId,
										  String executionId, String testId, AuthorizationDecision expected,
										  AuthorizationDecision observed, PolicyValidationState state,
										  String confidence, List<String> reasons) {
		String reviewId = "policy-" + TokenFingerprint.sha256(canonical(policy.policyReference(), observationId,
				executionId, testId, state.name())).substring(0, 24);
		return new PolicyValidationResult(reviewId, policy.policyReference(), policy.policySource(), context, expected, observed,
				policy.evidenceIds(), observationId, executionId, testId, state, confidence,
				state == PolicyValidationState.ALLOWED ? "Explicit supplied policy and observation agree"
						: "Supplied policy review did not establish an unambiguous authorization result", reasons);
	}

	private PolicyValidationResult conflict(List<PolicyValidationResult> reviews, List<String> reasons) {
		return conflictResult(reviews, PolicyValidationState.CONFLICTING, "INSUFFICIENT", reasons);
	}

	private PolicyValidationResult conflictResult(List<PolicyValidationResult> reviews, PolicyValidationState state,
												 String confidence, List<String> reasons) {
		List<String> refs = reviews.stream().map(PolicyValidationResult::policyReference).toList();
		List<String> evidence = reviews.stream().flatMap(review -> review.evidenceIds().stream()).toList();
		String material = canonical(refs.toArray(String[]::new));
		return new PolicyValidationResult("policy-conflict-" + TokenFingerprint.sha256(material).substring(0, 24),
				String.join(",", refs), "supplied-policy-conflict", null, AuthorizationDecision.UNKNOWN,
				AuthorizationDecision.UNKNOWN, evidence, "", "", "", state, confidence,
				"Conflicting supplied policy reviews retained; no precedence applied", reasons);
	}

	private PolicyValidationResult inconclusive(String reference, String source, AuthorizationContext context,
												AuthorizationDecision expected, AuthorizationDecision observed,
												List<String> evidence, String observationId, String executionId,
												String testId, List<String> reasons) {
		return new PolicyValidationResult("policy-inconclusive-" + TokenFingerprint.sha256(canonical(reference,
				observationId, executionId, testId)).substring(0, 24), reference, source, context, expected, observed,
				evidence, observationId, executionId, testId, PolicyValidationState.INCONCLUSIVE, "INSUFFICIENT",
				"Supplied policy or authorization context is incomplete", reasons);
	}

	private PolicyValidationState policyValidationState(List<String> reasons, AuthorizationDecision expected) {
		if (reasons.stream().anyMatch(reason -> reason.contains("CONFLICT") || reason.contains("UNSUPPORTED"))) {
			return PolicyValidationState.CONFLICTING;
		}
		if (reasons.stream().anyMatch(this::incompleteReason)) return PolicyValidationState.INCONCLUSIVE;
		return expected == AuthorizationDecision.DENY ? PolicyValidationState.DENIED : PolicyValidationState.CONFLICTING;
	}

	private boolean incompleteReason(String reason) {
		return reason.contains("UNKNOWN") || reason.contains("MISSING") || reason.contains("EVIDENCE_")
				|| reason.contains("OBSERVATION_") || reason.equals("POLICY_OR_CONTEXT_MISSING");
	}

	private PolicyValidationState decisionState(AuthorizationDecision expected, AuthorizationDecision observed) {
		if (expected == observed && expected == AuthorizationDecision.ALLOW) return PolicyValidationState.ALLOWED;
		if (expected == observed && expected == AuthorizationDecision.DENY) return PolicyValidationState.DENIED;
		return PolicyValidationState.CONFLICTING;
	}

	private boolean binary(AuthorizationDecision decision) {
		return decision == AuthorizationDecision.ALLOW || decision == AuthorizationDecision.DENY;
	}

	private boolean unknown(String value) {
		return value == null || value.isBlank() || value.strip().equalsIgnoreCase("UNKNOWN")
				|| value.toLowerCase(Locale.ROOT).contains(UniversalRedactor.REDACTED)
				|| !value.equals(REDACTOR.redactText(value));
	}

	private static String required(String value, String name) {
		if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " required");
		return REDACTOR.redactText(value);
	}

	private static String normalized(String value) {
		return value == null || value.isBlank() ? "UNKNOWN" : REDACTOR.redactText(value);
	}

	private static String policyReference(TenantPolicy policy) { return policy == null ? "" : policy.policyReference(); }
	private static String policyReference(WorkflowPolicy policy) { return policy == null ? "" : policy.policyReference(); }
	private static String policyReference(PropertyPolicy policy) { return policy == null ? "" : policy.policyReference(); }
	private static String policySource(TenantPolicy policy) { return policy == null ? "" : policy.policySource(); }
	private static String policySource(WorkflowPolicy policy) { return policy == null ? "" : policy.policySource(); }
	private static String policySource(PropertyPolicy policy) { return policy == null ? "" : policy.policySource(); }

	private static String canonical(String... values) {
		StringBuilder result = new StringBuilder();
		for (String value : values) result.append(value == null ? -1 : value.length()).append(':').append(value == null ? "" : value);
		return result.toString();
	}

	private interface Policy {
		String policyReference();
		String policySource();
		List<String> evidenceIds();
	}
}
