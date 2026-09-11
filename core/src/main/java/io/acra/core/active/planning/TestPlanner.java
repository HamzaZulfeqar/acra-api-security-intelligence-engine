package io.acra.core.active.planning;

import io.acra.core.active.execution.TestPriority;
import io.acra.core.active.model.PlanSkip;
import io.acra.core.active.model.ReproducibilityMetadata;
import io.acra.core.active.model.SecurityTest;
import io.acra.core.active.model.SelectionMode;
import io.acra.core.active.model.TestPlan;
import io.acra.core.domain.endpoint.RiskTier;
import io.acra.core.recon.ContextCoverage;
import io.acra.core.recon.EndpointRiskAssessment;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class TestPlanner {
    private record Candidate(SecurityTest test, TestPriority priority) {}
    private enum FilterKind { NONE, SCOPE, SELECTION }
    private record Eligibility(FilterKind kind, String reason) {}

    public TestPlan plan(PlanningInput input) {
        return planWithMetrics(input).plan();
    }

    public PlanningResult planWithMetrics(PlanningInput input) {
        if (input == null) throw new IllegalArgumentException("planning input required");
        List<Candidate> candidates = new ArrayList<>();
        List<PlanSkip> skipped = new ArrayList<>();
        Set<String> inventoryIds = input.apiInventory().stream().map(record -> record.endpoint().endpointId()).collect(java.util.stream.Collectors.toSet());
        int scopeFiltered = 0;
        int selectionFiltered = 0;

        for (TestSeed seed : input.seeds()) {
            Eligibility eligibility = eligibility(input, seed, inventoryIds);
            if (eligibility.kind() != FilterKind.NONE) {
                skipped.add(new PlanSkip(seed.testId(), eligibility.reason()));
                if (eligibility.kind() == FilterKind.SCOPE) scopeFiltered++;
                else selectionFiltered++;
                continue;
            }
            SecurityTest test = toTest(input, seed);
            candidates.add(new Candidate(test, priority(input, seed)));
        }

        candidates.sort(Comparator.comparingLong((Candidate candidate) -> candidate.priority().score()).reversed()
                .thenComparing(Comparator.comparingInt((Candidate candidate) -> candidate.test().priority()).reversed())
                .thenComparing(candidate -> candidate.test().signature()));

        int requestLimit = Math.min(input.requestBudget(), input.profile().requestBudget());
        int mutationLimit = Math.min(input.mutationBudget(), input.profile().mutationBudget());
        int requests = 0;
        int mutations = 0;
        int deduplicated = 0;
        int budgetFiltered = 0;
        Set<String> signatures = new HashSet<>();
        List<SecurityTest> selected = new ArrayList<>();
        for (Candidate candidate : candidates) {
            SecurityTest test = candidate.test();
            if (!signatures.add(test.signature())) {
                skipped.add(new PlanSkip(test.testId(), "duplicate canonical test signature"));
                deduplicated++;
            } else if (mutations >= mutationLimit) {
                skipped.add(new PlanSkip(test.testId(), "mutation budget exhausted during planning"));
                budgetFiltered++;
            } else if (requests + test.estimatedRequestCost() > requestLimit) {
                skipped.add(new PlanSkip(test.testId(), "request budget exhausted during planning"));
                budgetFiltered++;
            } else {
                selected.add(test);
                mutations++;
                requests += test.estimatedRequestCost();
            }
        }
        TestPlan plan = TestPlan.create(input.planId(), input.selectionMode(), input.profile().profile(), selected, skipped, input.createdAt());
        return new PlanningResult(plan, new PlanningMetrics(input.seeds().size(), candidates.size(), deduplicated,
                scopeFiltered, selectionFiltered, budgetFiltered, selected.size(), requests));
    }

    private static Eligibility eligibility(PlanningInput input, TestSeed seed, Set<String> inventoryIds) {
        if (!inventoryIds.contains(seed.endpoint().endpointId())) return scope("endpoint is absent from the S3 API inventory");
        if (!seed.endpoint().host().equalsIgnoreCase(input.target().host())) return scope("endpoint host does not match target");
        if (!input.enabledContracts().contains(seed.contract()) || !input.profile().enabledContracts().contains(seed.contract())) {
            return selection("test contract is disabled");
        }
        if (!input.profile().mutationFamilies().contains(seed.mutation().type())) return selection("mutation family is disabled by profile");
        if (!input.safetyPolicy().allowedMethods().contains(seed.endpoint().method())) return scope("method is disabled by safety policy");
        if (seed.userExcluded()) return selection("user explicitly excluded test");
        boolean selected = switch (input.selectionMode()) {
            case AUTOMATIC -> seed.recommended();
            case USER_SELECTED -> seed.userSelected();
            case HYBRID -> seed.recommended() || seed.userSelected();
            case ALL -> true;
        };
        return selected ? new Eligibility(FilterKind.NONE, "") : selection("test is not selected by mode");
    }

    private static Eligibility scope(String reason) { return new Eligibility(FilterKind.SCOPE, reason); }
    private static Eligibility selection(String reason) { return new Eligibility(FilterKind.SELECTION, reason); }

    private static SecurityTest toTest(PlanningInput input, TestSeed seed) {
        TestPriority priority = priority(input, seed);
        int normalizedPriority = (int) Math.min(100, Math.round(priority.score() / 30.0));
        return new SecurityTest(seed.testId(), seed.testVersion(), seed.contract(), seed.baseline().request().protocol(),
                input.target(), seed.endpoint(), seed.endpoint().method(), seed.baseline(), seed.positiveControl(),
                seed.negativeControl(), seed.mutation(), seed.sourceContext(), seed.targetContext(), seed.sourceResource(),
                seed.targetResource(), seed.expectedDecision(), seed.expectedEvidence(), input.safetyPolicy(), normalizedPriority,
                seed.selectionReason(), input.configurationSnapshot(), seed.dependencies(),
                new ReproducibilityMetadata("0.4.0-rc1", 0L, input.createdAt(),
                        List.of("S3_API_INVENTORY", "S3_SECURITY_CONTEXT_GRAPH", "S3_AUTHORIZATION_MATRIX")),
                seed.estimatedRequestCost(), seed.invariants());
    }

    private static TestPriority priority(PlanningInput input, TestSeed seed) {
        EndpointRiskAssessment risk = input.riskPriority().get(seed.endpoint().endpointId());
        ContextCoverage coverage = input.contextCoverage().get(seed.endpoint().endpointId());
        int riskScore = risk == null ? inventoryRisk(input, seed) : Math.min(100, risk.score());
        int context = coverage == null ? 0 : (int) Math.round(coverage.ratio() * 100.0);
        int resource = seed.targetResource() == null ? 20 : 70;
        int endpoint = risk == null ? 50 : switch (risk.priority()) {
            case HIGH -> 80;
            case MEDIUM -> 60;
            case LOW -> 40;
            case UNKNOWN -> 20;
        };
        int testClass = switch (seed.contract()) {
            case CROSS_TENANT, ROLE_COMPARISON -> 90;
            case CROSS_USER, CROSS_RESOURCE, ANONYMOUS_VS_AUTHENTICATED -> 80;
            default -> 60;
        };
        int dependencies = seed.dependencies().isEmpty() ? 100 : 60;
        int user = seed.userSelected() ? 100 : seed.recommended() ? 70 : 30;
        return new TestPriority(riskScore, context, resource, endpoint, testClass, dependencies, user);
    }

    private static int inventoryRisk(PlanningInput input, TestSeed seed) {
        return input.apiInventory().stream()
                .filter(record -> record.endpoint().endpointId().equals(seed.endpoint().endpointId()))
                .map(record -> record.riskTier())
                .findFirst().map(TestPlanner::riskScore).orElse(20);
    }

    private static int riskScore(RiskTier tier) {
        return switch (tier) {
            case CRITICAL -> 100;
            case HIGH -> 80;
            case MEDIUM -> 60;
            case LOW -> 40;
            case UNKNOWN -> 20;
        };
    }
}
