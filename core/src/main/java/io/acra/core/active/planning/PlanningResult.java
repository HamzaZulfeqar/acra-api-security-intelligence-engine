package io.acra.core.active.planning;

import io.acra.core.active.model.TestPlan;

public record PlanningResult(TestPlan plan, PlanningMetrics metrics) {
    public PlanningResult {
        if (plan == null || metrics == null) throw new IllegalArgumentException("plan and metrics required");
        if (plan.tests().size() != metrics.plannedTests()
                || plan.estimatedRequests() != metrics.estimatedRequests()) {
            throw new IllegalArgumentException("planning metrics do not match plan");
        }
    }
}
