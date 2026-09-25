package io.acra.core.active.model;

import java.util.EnumSet;
import java.util.Set;

public record TestProfileDefinition(
        TestProfile profile,
        Set<TestContract> enabledContracts,
        Set<MutationType> mutationFamilies,
        int requestBudget,
        int mutationBudget,
        int concurrency,
        int requestsPerSecond,
        ComparisonModel comparisonModel,
        ConfirmationPolicy confirmationPolicy,
        double evidenceThreshold,
        EvidenceDetail evidenceDetail,
        SafetyClass safetyClass,
        boolean localAutomaticExecution) {
    public TestProfileDefinition {
        if (profile == null || comparisonModel == null || confirmationPolicy == null
                || evidenceDetail == null || safetyClass == null) {
            throw new IllegalArgumentException("profile definition fields required");
        }
        enabledContracts = Set.copyOf(enabledContracts == null ? Set.of() : enabledContracts);
        mutationFamilies = Set.copyOf(mutationFamilies == null ? Set.of() : mutationFamilies);
        if (requestBudget < 0 || mutationBudget < 0 || concurrency < 1 || requestsPerSecond < 1) {
            throw new IllegalArgumentException("invalid profile limits");
        }
        if (evidenceThreshold < 0.0 || evidenceThreshold > 1.0) throw new IllegalArgumentException("evidenceThreshold");
    }

    public TestProfileDefinition(TestProfile profile, Set<TestContract> enabledContracts,
                                 Set<MutationType> mutationFamilies, int requestBudget, int mutationBudget,
                                 int concurrency, int requestsPerSecond, ComparisonModel comparisonModel,
                                 ConfirmationPolicy confirmationPolicy, double evidenceThreshold) {
        this(profile, enabledContracts, mutationFamilies, requestBudget, mutationBudget, concurrency,
                requestsPerSecond, comparisonModel, confirmationPolicy, evidenceThreshold,
                EvidenceDetail.DETAILED, SafetyClass.SAFE_READ_ONLY, true);
    }

    public static TestProfileDefinition defaults(TestProfile profile) {
        return switch (profile) {
            case SAFE_LAB -> new TestProfileDefinition(profile,
                    EnumSet.of(TestContract.CROSS_USER, TestContract.CROSS_RESOURCE, TestContract.CROSS_TENANT,
                            TestContract.ROLE_COMPARISON, TestContract.ANONYMOUS_VS_AUTHENTICATED),
                    EnumSet.of(MutationType.IDENTITY_SUBSTITUTION, MutationType.ROLE_SUBSTITUTION,
                            MutationType.AUTHENTICATED_CONTEXT_SUBSTITUTION,
                            MutationType.TENANT_SUBSTITUTION, MutationType.RESOURCE_SUBSTITUTION),
                    40, 10, 1, 2, ComparisonModel.STATUS_AND_SEMANTIC, ConfirmationPolicy.ALWAYS, 0.80,
                    EvidenceDetail.STANDARD, SafetyClass.SAFE_READ_ONLY, true);
            case AUTHORIZATION_DIFFERENTIAL -> new TestProfileDefinition(profile,
                    EnumSet.of(TestContract.CROSS_USER, TestContract.CROSS_RESOURCE, TestContract.CROSS_TENANT,
                            TestContract.ROLE_COMPARISON, TestContract.ANONYMOUS_VS_AUTHENTICATED),
                    EnumSet.of(MutationType.IDENTITY_SUBSTITUTION, MutationType.ROLE_SUBSTITUTION,
                            MutationType.AUTHENTICATED_CONTEXT_SUBSTITUTION,
                            MutationType.TENANT_SUBSTITUTION, MutationType.RESOURCE_SUBSTITUTION),
                    100, 25, 2, 3, ComparisonModel.SEMANTIC_RESOURCE, ConfirmationPolicy.ALWAYS, 0.80,
                    EvidenceDetail.DETAILED, SafetyClass.SAFE_READ_ONLY, true);
            case ROUTING_DIFFERENTIAL -> new TestProfileDefinition(profile,
                    EnumSet.of(TestContract.URI_REPRESENTATION, TestContract.NORMALIZATION,
                            TestContract.ROUTE_EQUIVALENCE, TestContract.METHOD_VARIATION),
                    EnumSet.of(MutationType.REPRESENTATION_VARIATION, MutationType.ENCODING_REPRESENTATION,
                            MutationType.NORMALIZATION_REPRESENTATION, MutationType.EQUIVALENT_ROUTE_REPRESENTATION,
                            MutationType.METHOD_REPRESENTATION),
                    60, 15, 1, 2, ComparisonModel.STRUCTURAL, ConfirmationPolicy.ALWAYS, 0.85,
                    EvidenceDetail.DETAILED, SafetyClass.SAFE_READ_ONLY, true);
            case CONTEXT_DIFFERENTIAL -> new TestProfileDefinition(profile,
                    EnumSet.of(TestContract.IDENTITY, TestContract.ROLE, TestContract.TENANT,
                            TestContract.RESOURCE, TestContract.TOKEN_CONTEXT),
                    EnumSet.of(MutationType.IDENTITY_SUBSTITUTION, MutationType.ROLE_SUBSTITUTION,
                            MutationType.AUTHENTICATED_CONTEXT_SUBSTITUTION,
                            MutationType.TENANT_SUBSTITUTION, MutationType.RESOURCE_SUBSTITUTION),
                    80, 20, 1, 2, ComparisonModel.STATUS_AND_SEMANTIC, ConfirmationPolicy.ALWAYS, 0.85,
                    EvidenceDetail.DETAILED, SafetyClass.SAFE_READ_ONLY, true);
            case RESEARCH_EXPERIMENT -> new TestProfileDefinition(profile,
                    EnumSet.allOf(TestContract.class), EnumSet.allOf(MutationType.class),
                    200, 50, 1, 2, ComparisonModel.RESEARCH_ABLATION, ConfirmationPolicy.ALWAYS, 0.90,
                    EvidenceDetail.FORENSIC, SafetyClass.SAFE_READ_ONLY, true);
            case EXPERT_CONTROLLED -> new TestProfileDefinition(profile,
                    EnumSet.allOf(TestContract.class), EnumSet.allOf(MutationType.class),
                    200, 50, 2, 3, ComparisonModel.STATUS_AND_SEMANTIC, ConfirmationPolicy.ALWAYS, 0.90,
                    EvidenceDetail.FORENSIC, SafetyClass.SAFE_READ_ONLY, true);
        };
    }
}
