package io.acra.core.active.model;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public record UserModeDefinition(
        UserMode mode,
        TestProfileDefinition profile,
        SelectionMode selectionMode,
        int testDepth,
        int requestBudget,
        int mutationBudget,
        int concurrency,
        int requestsPerSecond,
        EvidenceDetail evidenceDetail,
        Set<ActiveControl> controls) {
    public UserModeDefinition {
        if (mode == null || profile == null || selectionMode == null || evidenceDetail == null) {
            throw new IllegalArgumentException("mode definition fields required");
        }
        if (testDepth < 1 || requestBudget < 0 || mutationBudget < 0 || concurrency < 1 || requestsPerSecond < 1) {
            throw new IllegalArgumentException("invalid mode limits");
        }
        controls = Set.copyOf(controls == null ? Set.of() : controls);
    }

    public static UserModeDefinition defaults(UserMode mode) {
        return switch (mode) {
            case BEGINNER -> definition(mode, TestProfile.SAFE_LAB, SelectionMode.AUTOMATIC,
                    1, 20, 5, 1, 1, EvidenceDetail.STANDARD,
                    EnumSet.of(ActiveControl.STOP_ALL, ActiveControl.COVERAGE));
            case PROFESSIONAL -> definition(mode, TestProfile.AUTHORIZATION_DIFFERENTIAL, SelectionMode.HYBRID,
                    2, 80, 20, 2, 2, EvidenceDetail.DETAILED,
                    EnumSet.of(ActiveControl.PROFILE, ActiveControl.SELECTION_MODE, ActiveControl.STRATEGY,
                            ActiveControl.CATEGORY, ActiveControl.STOP_ALL, ActiveControl.REQUEST_BUDGET,
                            ActiveControl.MUTATION_BUDGET, ActiveControl.CONCURRENCY, ActiveControl.RATE_LIMIT,
                            ActiveControl.COVERAGE));
            case RESEARCHER -> definition(mode, TestProfile.RESEARCH_EXPERIMENT, SelectionMode.ALL,
                    4, 200, 50, 1, 2, EvidenceDetail.FORENSIC, EnumSet.allOf(ActiveControl.class));
            case EXPERT -> definition(mode, TestProfile.EXPERT_CONTROLLED, SelectionMode.USER_SELECTED,
                    4, 200, 50, 2, 3, EvidenceDetail.FORENSIC, EnumSet.allOf(ActiveControl.class));
        };
    }

    public PlanningInputValues applyValues(PlanningInputValues input) {
        if (input == null) throw new IllegalArgumentException("planning values required");
        Set<TestContract> contracts = EnumSet.noneOf(TestContract.class);
        contracts.addAll(input.enabledContracts());
        contracts.retainAll(profile.enabledContracts());
        TreeMap<String, String> values = new TreeMap<>(input.configuration().values());
        values.put("userMode", mode.name());
        values.put("profile", profile.profile().name());
        values.put("selectionMode", selectionMode.name());
        values.put("evidenceDetail", evidenceDetail.name());
        values.put("testDepth", Integer.toString(testDepth));
        return new PlanningInputValues(Set.copyOf(contracts), Math.min(input.requestBudget(), requestBudget),
                Math.min(input.mutationBudget(), mutationBudget), ConfigurationSnapshot.of(values));
    }

    private static UserModeDefinition definition(UserMode mode, TestProfile profile, SelectionMode selection,
                                                 int depth, int requests, int mutations, int concurrency,
                                                 int rate, EvidenceDetail evidence, Set<ActiveControl> controls) {
        return new UserModeDefinition(mode, TestProfileDefinition.defaults(profile), selection, depth, requests,
                mutations, concurrency, rate, evidence, controls);
    }

    public record PlanningInputValues(Set<TestContract> enabledContracts, int requestBudget, int mutationBudget,
                                      ConfigurationSnapshot configuration) {
        public PlanningInputValues {
            enabledContracts = Set.copyOf(enabledContracts == null ? Set.of() : enabledContracts);
            if (requestBudget < 0 || mutationBudget < 0 || configuration == null) {
                throw new IllegalArgumentException("invalid planning values");
            }
        }

        public static PlanningInputValues of(Set<TestContract> contracts, int requests, int mutations,
                                             ConfigurationSnapshot configuration) {
            return new PlanningInputValues(contracts, requests, mutations, configuration);
        }
    }
}
