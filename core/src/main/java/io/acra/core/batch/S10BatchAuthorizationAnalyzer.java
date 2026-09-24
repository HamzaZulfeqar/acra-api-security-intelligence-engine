package io.acra.core.batch;

import io.acra.core.active.evidence.EvidenceReferenceValidation;
import io.acra.core.active.evidence.EvidenceReferenceValidator;
import io.acra.core.active.evidence.ExecutionEvidenceStore;
import io.acra.core.domain.authorization.AuthorizationContext;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.ContextStatus;
import io.acra.core.domain.authorization.PolicyValidationState;
import io.acra.core.security.TokenFingerprint;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class S10BatchAuthorizationAnalyzer {
    private final EvidenceReferenceValidator evidenceValidator;

    public S10BatchAuthorizationAnalyzer(ExecutionEvidenceStore evidenceStore) {
        if (evidenceStore == null) throw new IllegalArgumentException("evidenceStore required");
        this.evidenceValidator = new EvidenceReferenceValidator(evidenceStore);
    }

    public S10BatchAuthorizationAnalysis analyze(
            AuthorizationContext actorContext,
            List<BatchItemPolicy> policies,
            List<BatchItemObservation> observations,
            String projectId) {

        if (actorContext == null) throw new IllegalArgumentException("actorContext required");
        if (projectId == null || projectId.isBlank()) throw new IllegalArgumentException("projectId required");

        List<BatchItemPolicy> policyList = List.copyOf(policies == null ? List.of() : policies);
        List<BatchItemObservation> observationList =
                List.copyOf(observations == null ? List.of() : observations).stream()
                        .sorted(Comparator
                                .comparing(BatchItemObservation::batchId)
                                .thenComparing(BatchItemObservation::itemKey)
                                .thenComparing(BatchItemObservation::itemObservationId))
                        .toList();

        if (observationList.stream().map(BatchItemObservation::itemObservationId).distinct().count()
                != observationList.size()) {
            throw new IllegalArgumentException("duplicate batch item observation id");
        }

        List<BatchItemAuthorizationAssessment> assessments = new ArrayList<>();
        Set<String> evidenceIds = new LinkedHashSet<>();
        List<String> aggregateReasons = new ArrayList<>();

        if (observationList.isEmpty()) aggregateReasons.add("BATCH_ITEM_OBSERVATION_MISSING");

        for (BatchItemObservation observation : observationList) {
            evidenceIds.addAll(observation.evidenceIds());

            if (actorContext.status() != ContextStatus.RESOLVED) {
                assessments.add(manualAssessment(
                        observation,
                        PolicyValidationState.INCONCLUSIVE,
                        AuthorizationDecision.UNKNOWN,
                        "",
                        "Actor authorization context is unresolved",
                        List.of("BATCH_ACTOR_CONTEXT_UNRESOLVED")));
                aggregateReasons.add("BATCH_ACTOR_CONTEXT_UNRESOLVED");
                continue;
            }

            EvidenceReferenceValidation evidence = evidenceValidator.validateEvidenceReferences(
                    observation.evidenceIds(),
                    observation.executionId(),
                    observation.testId(),
                    projectId);
            EvidenceReferenceValidation sourceObservation = evidenceValidator.validateObservation(
                    observation.sourceObservationId(),
                    observation.executionId(),
                    observation.testId(),
                    projectId);
            if (!evidence.valid() || !sourceObservation.valid()) {
                List<String> reasons = union(evidence.reasons(), sourceObservation.reasons());
                assessments.add(manualAssessment(
                        observation,
                        PolicyValidationState.INCONCLUSIVE,
                        AuthorizationDecision.UNKNOWN,
                        "",
                        "Batch item provenance could not be validated",
                        reasons));
                aggregateReasons.addAll(reasons);
                continue;
            }

            List<BatchItemPolicy> matches = policyList.stream()
                    .filter(policy -> policy.endpoint().equals(observation.endpoint()))
                    .filter(policy -> policy.resourceId().equals(observation.resourceId()))
                    .filter(policy -> policy.action().equals(observation.action()))
                    .filter(policy -> applicable(policy.roleId(),
                            actorContext.role() == null ? null : actorContext.role().roleId()))
                    .filter(policy -> applicable(policy.tenantId(),
                            actorContext.tenant() == null ? null : actorContext.tenant().tenantId()))
                    .sorted(Comparator.comparing(BatchItemPolicy::policyReference))
                    .toList();

            if (matches.isEmpty()) {
                assessments.add(manualAssessment(
                        observation,
                        PolicyValidationState.INCONCLUSIVE,
                        AuthorizationDecision.UNKNOWN,
                        "",
                        "No explicit applicable batch item policy was supplied",
                        List.of("BATCH_ITEM_POLICY_NOT_FOUND")));
                aggregateReasons.add("BATCH_ITEM_POLICY_NOT_FOUND");
                continue;
            }

            if (matches.size() > 1) {
                assessments.add(manualAssessment(
                        observation,
                        PolicyValidationState.CONFLICTING,
                        AuthorizationDecision.UNKNOWN,
                        "",
                        "Multiple applicable batch item policies prevent deterministic attribution",
                        List.of("BATCH_ITEM_POLICY_AMBIGUOUS")));
                aggregateReasons.add("BATCH_ITEM_POLICY_AMBIGUOUS");
                continue;
            }

            BatchItemPolicy policy = matches.getFirst();
            EvidenceReferenceValidation policyEvidence = evidenceValidator.validateEvidenceReferences(
                    policy.evidenceIds(),
                    observation.executionId(),
                    observation.testId(),
                    projectId);
            if (!policyEvidence.valid()) {
                assessments.add(manualAssessment(
                        observation,
                        PolicyValidationState.INCONCLUSIVE,
                        policy.expectedDecision(),
                        policy.policyReference(),
                        "Batch item policy evidence could not be validated",
                        policyEvidence.reasons()));
                aggregateReasons.addAll(policyEvidence.reasons());
                continue;
            }

            evidenceIds.addAll(policy.evidenceIds());
            PolicyValidationState state = decisionState(
                    policy.expectedDecision(), observation.observedDecision());
            String confidence = state == PolicyValidationState.INCONCLUSIVE ? "INSUFFICIENT" : "HIGH";
            String rationale = state == PolicyValidationState.CONFLICTING
                    ? "Explicit batch item policy and observed item authorization decision differ"
                    : "Explicit batch item policy and observed item authorization decision were evaluated independently";

            assessments.add(new BatchItemAuthorizationAssessment(
                    assessmentId(observation, policy.policyReference(), state),
                    observation.batchId(),
                    observation.itemKey(),
                    observation.endpoint(),
                    observation.resourceId(),
                    observation.action(),
                    policy.policyReference(),
                    policy.expectedDecision(),
                    observation.observedDecision(),
                    state,
                    confidence,
                    union(observation.evidenceIds(), policy.evidenceIds()),
                    rationale,
                    List.of()));
        }

        String material = projectId + "|" + assessments.stream()
                .map(BatchItemAuthorizationAssessment::assessmentId)
                .sorted()
                .reduce("", (left, right) -> left + "|" + right);
        return new S10BatchAuthorizationAnalysis(
                "s10-batch-" + TokenFingerprint.sha256(material).substring(0, 24),
                assessments,
                List.copyOf(evidenceIds),
                List.copyOf(new LinkedHashSet<>(aggregateReasons)));
    }

    private BatchItemAuthorizationAssessment manualAssessment(
            BatchItemObservation observation,
            PolicyValidationState state,
            AuthorizationDecision expected,
            String policyReference,
            String rationale,
            List<String> reasons) {
        return new BatchItemAuthorizationAssessment(
                assessmentId(observation, policyReference, state),
                observation.batchId(),
                observation.itemKey(),
                observation.endpoint(),
                observation.resourceId(),
                observation.action(),
                policyReference,
                expected,
                observation.observedDecision(),
                state,
                "INSUFFICIENT",
                observation.evidenceIds(),
                rationale,
                reasons);
    }

    private static String assessmentId(
            BatchItemObservation observation,
            String policyReference,
            PolicyValidationState state) {
        String material = String.join("|",
                observation.batchId(),
                observation.itemKey(),
                observation.resourceId(),
                observation.action(),
                policyReference == null ? "" : policyReference,
                state.name());
        return "s10-batch-assessment-" + TokenFingerprint.sha256(material).substring(0, 24);
    }

    private static PolicyValidationState decisionState(
            AuthorizationDecision expected,
            AuthorizationDecision observed) {
        if (!binary(expected) || !binary(observed)) return PolicyValidationState.INCONCLUSIVE;
        if (expected == observed && expected == AuthorizationDecision.ALLOW) return PolicyValidationState.ALLOWED;
        if (expected == observed) return PolicyValidationState.DENIED;
        return PolicyValidationState.CONFLICTING;
    }

    private static boolean binary(AuthorizationDecision value) {
        return value == AuthorizationDecision.ALLOW || value == AuthorizationDecision.DENY;
    }

    private static boolean applicable(String policyValue, String contextValue) {
        if (policyValue == null || policyValue.isBlank()
                || "UNKNOWN".equals(policyValue.toUpperCase(Locale.ROOT))) {
            return true;
        }
        return contextValue != null && policyValue.equals(contextValue);
    }

    private static List<String> union(List<String> left, List<String> right) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (left != null) values.addAll(left);
        if (right != null) values.addAll(right);
        return List.copyOf(values);
    }
}
