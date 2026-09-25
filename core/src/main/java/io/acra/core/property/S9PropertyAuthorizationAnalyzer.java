package io.acra.core.property;

import io.acra.core.active.evidence.EvidenceReferenceValidation;
import io.acra.core.active.evidence.EvidenceReferenceValidator;
import io.acra.core.active.evidence.ExecutionEvidenceStore;
import io.acra.core.domain.authorization.AuthorizationContext;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.PolicyValidationState;
import io.acra.core.domain.authorization.PropertyAuthorizationAssessment;
import io.acra.core.engine.AuthorizationDimensionEvaluator;
import io.acra.core.engine.PolicyValidationEvaluator;
import io.acra.core.security.TokenFingerprint;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class S9PropertyAuthorizationAnalyzer {
    private final EvidenceReferenceValidator evidenceValidator;
    private final AuthorizationDimensionEvaluator dimensionEvaluator;

    public S9PropertyAuthorizationAnalyzer(ExecutionEvidenceStore evidenceStore) {
        if (evidenceStore == null) throw new IllegalArgumentException("evidenceStore required");
        this.evidenceValidator = new EvidenceReferenceValidator(evidenceStore);
        this.dimensionEvaluator = new AuthorizationDimensionEvaluator(
                new PolicyValidationEvaluator(evidenceValidator));
    }

    public S9PropertyAuthorizationAnalysis analyze(
            AuthorizationContext context,
            List<PolicyValidationEvaluator.PropertyPolicy> policies,
            List<PropertyAccessObservation> observations,
            String projectId) {

        if (context == null) throw new IllegalArgumentException("context required");
        if (projectId == null || projectId.isBlank()) throw new IllegalArgumentException("projectId required");

        List<PolicyValidationEvaluator.PropertyPolicy> policyList =
                List.copyOf(policies == null ? List.of() : policies);
        List<PropertyAccessObservation> observationList =
                List.copyOf(observations == null ? List.of() : observations).stream()
                        .sorted(Comparator
                                .comparing(PropertyAccessObservation::endpoint)
                                .thenComparing(PropertyAccessObservation::property)
                                .thenComparing(value -> value.operation().name())
                                .thenComparing(PropertyAccessObservation::observationId))
                        .toList();

        List<PropertyAuthorizationAssessment> assessments = new ArrayList<>();
        Set<String> evidenceIds = new LinkedHashSet<>();
        List<String> aggregateReasons = new ArrayList<>();

        if (observationList.isEmpty()) {
            aggregateReasons.add("PROPERTY_OBSERVATION_MISSING");
        }

        for (PropertyAccessObservation observation : observationList) {
            evidenceIds.addAll(observation.evidenceIds());
            EvidenceReferenceValidation evidence = evidenceValidator.validateEvidenceReferences(
                    observation.evidenceIds(),
                    observation.executionId(),
                    observation.testId(),
                    projectId);
            EvidenceReferenceValidation observationLineage = evidenceValidator.validateObservation(
                    observation.observationId(),
                    observation.executionId(),
                    observation.testId(),
                    projectId);

            if (!evidence.valid() || !observationLineage.valid()) {
                List<String> provenanceReasons = new ArrayList<>();
                provenanceReasons.addAll(evidence.reasons());
                provenanceReasons.addAll(observationLineage.reasons());
                provenanceReasons = List.copyOf(new LinkedHashSet<>(provenanceReasons));
                assessments.add(manualAssessment(
                        observation,
                        context,
                        PolicyValidationState.INCONCLUSIVE,
                        "Property observation provenance could not be validated",
                        provenanceReasons));
                aggregateReasons.addAll(provenanceReasons);
                continue;
            }

            List<PolicyValidationEvaluator.PropertyPolicy> matches = policyList.stream()
                    .filter(policy -> policy.endpoint().equals(observation.endpoint()))
                    .filter(policy -> policy.property().equals(observation.property()))
                    .filter(policy -> policy.operation() == observation.operation())
                    .filter(policy -> applicable(policy.roleId(), context.role() == null ? null : context.role().roleId()))
                    .filter(policy -> applicable(policy.tenantId(), context.tenant() == null ? null : context.tenant().tenantId()))
                    .sorted(Comparator.comparing(PolicyValidationEvaluator.PropertyPolicy::policyReference))
                    .toList();

            if (matches.isEmpty()) {
                assessments.add(manualAssessment(
                        observation,
                        context,
                        PolicyValidationState.INCONCLUSIVE,
                        "No explicit applicable property policy was supplied",
                        List.of("PROPERTY_POLICY_NOT_FOUND")));
                aggregateReasons.add("PROPERTY_POLICY_NOT_FOUND");
                continue;
            }

            if (matches.size() > 1) {
                assessments.add(manualAssessment(
                        observation,
                        context,
                        PolicyValidationState.CONFLICTING,
                        "Multiple applicable property policies prevent deterministic attribution",
                        List.of("PROPERTY_POLICY_AMBIGUOUS")));
                aggregateReasons.add("PROPERTY_POLICY_AMBIGUOUS");
                continue;
            }

            PolicyValidationEvaluator.PropertyPolicy policy = matches.getFirst();
            evidenceIds.addAll(policy.evidenceIds());
            AuthorizationContext propertyContext = propertyContext(
                    context,
                    policy.expectedDecision(),
                    observation.observedDecision(),
                    observation.evidenceIds());

            assessments.add(dimensionEvaluator.evaluateProperty(
                    propertyContext,
                    policy,
                    observation.property(),
                    observation.operation(),
                    observation.endpoint(),
                    observation.observationId(),
                    observation.executionId(),
                    observation.testId(),
                    projectId));
        }

        String material = projectId + "|" + assessments.stream()
                .map(PropertyAuthorizationAssessment::assessmentId)
                .sorted()
                .reduce("", (left, right) -> left + "|" + right);
        String analysisId = "s9-property-" + TokenFingerprint.sha256(material).substring(0, 24);

        return new S9PropertyAuthorizationAnalysis(
                analysisId,
                assessments,
                List.copyOf(evidenceIds),
                List.copyOf(new LinkedHashSet<>(aggregateReasons)));
    }

    private PropertyAuthorizationAssessment manualAssessment(
            PropertyAccessObservation observation,
            AuthorizationContext context,
            PolicyValidationState state,
            String rationale,
            List<String> reasons) {

        String material = observation.observationId() + "|" + observation.endpoint() + "|"
                + observation.property() + "|" + observation.operation().name() + "|" + state.name();
        return new PropertyAuthorizationAssessment(
                "s9-property-assessment-" + TokenFingerprint.sha256(material).substring(0, 24),
                observation.endpoint(),
                observation.property(),
                observation.operation().name(),
                context.role() == null ? "UNKNOWN" : context.role().roleId(),
                context.tenant() == null ? "UNKNOWN" : context.tenant().tenantId(),
                "",
                AuthorizationDecision.UNKNOWN,
                observation.observedDecision(),
                state,
                "INSUFFICIENT",
                observation.evidenceIds(),
                rationale,
                reasons);
    }

    private AuthorizationContext propertyContext(
            AuthorizationContext context,
            AuthorizationDecision expected,
            AuthorizationDecision observed,
            List<String> observationEvidence) {

        Set<String> evidence = new LinkedHashSet<>(context.evidenceIds());
        evidence.addAll(observationEvidence);
        return new AuthorizationContext(
                context.principal(),
                context.role(),
                context.tenant(),
                context.resource(),
                context.ownerPrincipalId(),
                context.action(),
                context.workflowState(),
                expected,
                observed,
                List.copyOf(evidence),
                context.status());
    }

    private boolean applicable(String policyValue, String contextValue) {
        if (policyValue == null || policyValue.isBlank()
                || "UNKNOWN".equals(policyValue.toUpperCase(Locale.ROOT))) {
            return true;
        }
        return contextValue != null && policyValue.equals(contextValue);
    }
}
