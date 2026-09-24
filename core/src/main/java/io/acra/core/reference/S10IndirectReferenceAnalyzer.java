package io.acra.core.reference;

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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class S10IndirectReferenceAnalyzer {
    private final EvidenceReferenceValidator evidenceValidator;

    public S10IndirectReferenceAnalyzer(ExecutionEvidenceStore evidenceStore) {
        if (evidenceStore == null) throw new IllegalArgumentException("evidenceStore required");
        this.evidenceValidator = new EvidenceReferenceValidator(evidenceStore);
    }

    public S10IndirectReferenceAnalysis analyze(
            AuthorizationContext actorContext,
            List<IndirectReferencePolicy> policies,
            List<IndirectReferenceResolution> resolutions,
            String projectId) {

        if (actorContext == null) throw new IllegalArgumentException("actorContext required");
        if (projectId == null || projectId.isBlank()) throw new IllegalArgumentException("projectId required");

        List<IndirectReferencePolicy> policyList = List.copyOf(policies == null ? List.of() : policies);
        List<IndirectReferenceResolution> resolutionList =
                List.copyOf(resolutions == null ? List.of() : resolutions).stream()
                        .sorted(Comparator
                                .comparing(IndirectReferenceResolution::referenceFingerprint)
                                .thenComparing(IndirectReferenceResolution::resolvedResourceId)
                                .thenComparing(IndirectReferenceResolution::resolutionId))
                        .toList();

        if (resolutionList.stream().map(IndirectReferenceResolution::resolutionId).distinct().count()
                != resolutionList.size()) {
            throw new IllegalArgumentException("duplicate indirect reference resolution id");
        }

        Map<String, Set<String>> targetsByFingerprint = resolutionList.stream()
                .collect(Collectors.groupingBy(
                        IndirectReferenceResolution::referenceFingerprint,
                        Collectors.mapping(
                                IndirectReferenceResolution::resolvedResourceId,
                                Collectors.toCollection(LinkedHashSet::new))));

        Set<String> conflictingFingerprints = targetsByFingerprint.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<IndirectReferenceAuthorizationAssessment> assessments = new ArrayList<>();
        Set<String> evidenceIds = new LinkedHashSet<>();
        List<String> aggregateReasons = new ArrayList<>();

        if (resolutionList.isEmpty()) aggregateReasons.add("INDIRECT_REFERENCE_RESOLUTION_MISSING");
        if (!conflictingFingerprints.isEmpty()) aggregateReasons.add("INDIRECT_REFERENCE_RESOLUTION_CONFLICT");

        for (IndirectReferenceResolution resolution : resolutionList) {
            evidenceIds.addAll(resolution.evidenceIds());

            if (conflictingFingerprints.contains(resolution.referenceFingerprint())) {
                assessments.add(manualAssessment(
                        resolution,
                        PolicyValidationState.INCONCLUSIVE,
                        AuthorizationDecision.UNKNOWN,
                        "",
                        "One indirect-reference fingerprint resolves to multiple resources",
                        List.of("INDIRECT_REFERENCE_RESOLUTION_CONFLICT")));
                continue;
            }

            if (actorContext.status() != ContextStatus.RESOLVED) {
                assessments.add(manualAssessment(
                        resolution,
                        PolicyValidationState.INCONCLUSIVE,
                        AuthorizationDecision.UNKNOWN,
                        "",
                        "Actor authorization context is unresolved",
                        List.of("INDIRECT_ACTOR_CONTEXT_UNRESOLVED")));
                aggregateReasons.add("INDIRECT_ACTOR_CONTEXT_UNRESOLVED");
                continue;
            }

            EvidenceReferenceValidation evidence = evidenceValidator.validateEvidenceReferences(
                    resolution.evidenceIds(),
                    resolution.executionId(),
                    resolution.testId(),
                    projectId);
            EvidenceReferenceValidation sourceObservation = evidenceValidator.validateObservation(
                    resolution.sourceObservationId(),
                    resolution.executionId(),
                    resolution.testId(),
                    projectId);
            if (!evidence.valid() || !sourceObservation.valid()) {
                List<String> reasons = union(evidence.reasons(), sourceObservation.reasons());
                assessments.add(manualAssessment(
                        resolution,
                        PolicyValidationState.INCONCLUSIVE,
                        AuthorizationDecision.UNKNOWN,
                        "",
                        "Indirect-reference resolution provenance could not be validated",
                        reasons));
                aggregateReasons.addAll(reasons);
                continue;
            }

            List<IndirectReferencePolicy> matches = policyList.stream()
                    .filter(policy -> policy.endpoint().equals(resolution.endpoint()))
                    .filter(policy -> policy.resolvedResourceId().equals(resolution.resolvedResourceId()))
                    .filter(policy -> policy.action().equals(resolution.action()))
                    .filter(policy -> applicable(policy.roleId(),
                            actorContext.role() == null ? null : actorContext.role().roleId()))
                    .filter(policy -> applicable(policy.tenantId(),
                            actorContext.tenant() == null ? null : actorContext.tenant().tenantId()))
                    .sorted(Comparator.comparing(IndirectReferencePolicy::policyReference))
                    .toList();

            if (matches.isEmpty()) {
                assessments.add(manualAssessment(
                        resolution,
                        PolicyValidationState.INCONCLUSIVE,
                        AuthorizationDecision.UNKNOWN,
                        "",
                        "No explicit policy was supplied for the resolved target resource",
                        List.of("INDIRECT_RESOLVED_RESOURCE_POLICY_NOT_FOUND")));
                aggregateReasons.add("INDIRECT_RESOLVED_RESOURCE_POLICY_NOT_FOUND");
                continue;
            }

            if (matches.size() > 1) {
                assessments.add(manualAssessment(
                        resolution,
                        PolicyValidationState.CONFLICTING,
                        AuthorizationDecision.UNKNOWN,
                        "",
                        "Multiple policies apply to the resolved target resource",
                        List.of("INDIRECT_RESOLVED_RESOURCE_POLICY_AMBIGUOUS")));
                aggregateReasons.add("INDIRECT_RESOLVED_RESOURCE_POLICY_AMBIGUOUS");
                continue;
            }

            IndirectReferencePolicy policy = matches.getFirst();
            EvidenceReferenceValidation policyEvidence = evidenceValidator.validateEvidenceReferences(
                    policy.evidenceIds(),
                    resolution.executionId(),
                    resolution.testId(),
                    projectId);
            if (!policyEvidence.valid()) {
                assessments.add(manualAssessment(
                        resolution,
                        PolicyValidationState.INCONCLUSIVE,
                        policy.expectedDecision(),
                        policy.policyReference(),
                        "Resolved-resource policy evidence could not be validated",
                        policyEvidence.reasons()));
                aggregateReasons.addAll(policyEvidence.reasons());
                continue;
            }

            evidenceIds.addAll(policy.evidenceIds());
            PolicyValidationState state = decisionState(
                    policy.expectedDecision(), resolution.observedDecision());
            String confidence = state == PolicyValidationState.INCONCLUSIVE ? "INSUFFICIENT" : "HIGH";
            String rationale = state == PolicyValidationState.CONFLICTING
                    ? "Authorization for the resolved target resource differs from explicit policy"
                    : "Authorization was evaluated against the resolved target resource, not the indirect key";

            assessments.add(new IndirectReferenceAuthorizationAssessment(
                    assessmentId(resolution, policy.policyReference(), state),
                    resolution.endpoint(),
                    resolution.referenceFingerprint(),
                    resolution.referenceKind(),
                    resolution.resolvedResourceId(),
                    resolution.action(),
                    policy.policyReference(),
                    policy.expectedDecision(),
                    resolution.observedDecision(),
                    state,
                    confidence,
                    union(resolution.evidenceIds(), policy.evidenceIds()),
                    rationale,
                    List.of()));
        }

        String material = projectId + "|" + assessments.stream()
                .map(IndirectReferenceAuthorizationAssessment::assessmentId)
                .sorted()
                .reduce("", (left, right) -> left + "|" + right);
        return new S10IndirectReferenceAnalysis(
                "s10-indirect-" + TokenFingerprint.sha256(material).substring(0, 24),
                assessments,
                List.copyOf(evidenceIds),
                List.copyOf(new LinkedHashSet<>(aggregateReasons)));
    }

    private IndirectReferenceAuthorizationAssessment manualAssessment(
            IndirectReferenceResolution resolution,
            PolicyValidationState state,
            AuthorizationDecision expected,
            String policyReference,
            String rationale,
            List<String> reasons) {
        return new IndirectReferenceAuthorizationAssessment(
                assessmentId(resolution, policyReference, state),
                resolution.endpoint(),
                resolution.referenceFingerprint(),
                resolution.referenceKind(),
                resolution.resolvedResourceId(),
                resolution.action(),
                policyReference,
                expected,
                resolution.observedDecision(),
                state,
                "INSUFFICIENT",
                resolution.evidenceIds(),
                rationale,
                reasons);
    }

    private static String assessmentId(
            IndirectReferenceResolution resolution,
            String policyReference,
            PolicyValidationState state) {
        String material = String.join("|",
                resolution.referenceFingerprint(),
                resolution.resolvedResourceId(),
                resolution.action(),
                policyReference == null ? "" : policyReference,
                state.name());
        return "s10-indirect-assessment-" + TokenFingerprint.sha256(material).substring(0, 24);
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
