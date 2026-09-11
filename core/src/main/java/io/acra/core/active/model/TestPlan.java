package io.acra.core.active.model;

import io.acra.core.domain.common.Validation;
import io.acra.core.security.TokenFingerprint;
import java.time.Instant;
import java.util.List;

public record TestPlan(
        String planId,
        SelectionMode selectionMode,
        TestProfile profile,
        List<SecurityTest> tests,
        List<PlanSkip> skipped,
        int estimatedRequests,
        Instant createdAt,
        String fingerprint) {
    public TestPlan {
        planId = Validation.requireNonBlank(planId, "planId");
        if (selectionMode == null || profile == null || createdAt == null) throw new IllegalArgumentException("plan metadata required");
        tests = List.copyOf(tests == null ? List.of() : tests);
        skipped = List.copyOf(skipped == null ? List.of() : skipped);
        if (estimatedRequests < 0) throw new IllegalArgumentException("estimatedRequests");
        int calculatedCost = tests.stream().mapToInt(SecurityTest::estimatedRequestCost).sum();
        if (estimatedRequests != calculatedCost) throw new IllegalArgumentException("estimated request total mismatch");
        String calculated = TokenFingerprint.sha256(tests.stream().map(SecurityTest::signature).reduce("", (a, b) -> a + '\n' + b));
        if (fingerprint == null || fingerprint.isBlank()) fingerprint = calculated;
        if (!fingerprint.equals(calculated)) throw new IllegalArgumentException("plan fingerprint mismatch");
    }

    public static TestPlan create(String planId, SelectionMode mode, TestProfile profile, List<SecurityTest> tests, Instant createdAt) {
        return create(planId, mode, profile, tests, List.of(), createdAt);
    }

    public static TestPlan create(String planId, SelectionMode mode, TestProfile profile, List<SecurityTest> tests,
                                  List<PlanSkip> skipped, Instant createdAt) {
        List<SecurityTest> copy = List.copyOf(tests == null ? List.of() : tests);
        return new TestPlan(planId, mode, profile, copy, skipped,
                copy.stream().mapToInt(SecurityTest::estimatedRequestCost).sum(), createdAt, "");
    }
}
