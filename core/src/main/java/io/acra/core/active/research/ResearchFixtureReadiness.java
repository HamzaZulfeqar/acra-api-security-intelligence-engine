package io.acra.core.active.research;

import io.acra.core.domain.common.Validation;
import java.util.List;

public record ResearchFixtureReadiness(
        String caseId,
        String sourceCaseId,
        FixtureReadinessState state,
        List<String> routes,
        List<String> evidenceReferences,
        String reason) {

    public ResearchFixtureReadiness {
        caseId = Validation.requireNonBlank(caseId, "caseId");
        sourceCaseId = Validation.requireNonBlank(sourceCaseId, "sourceCaseId");
        if (state == null) throw new IllegalArgumentException("state required");
        routes = clean(routes);
        evidenceReferences = clean(evidenceReferences);
        reason = Validation.requireNonBlank(reason, "reason");

        if (state == FixtureReadinessState.READY
                && (routes.isEmpty() || evidenceReferences.isEmpty())) {
            throw new IllegalArgumentException("READY fixture requires route and evidence references");
        }
        if (state == FixtureReadinessState.PARTIAL
                && (routes.isEmpty() || evidenceReferences.isEmpty())) {
            throw new IllegalArgumentException("PARTIAL fixture requires route and evidence references");
        }
        if (state == FixtureReadinessState.MISSING_FIXTURE
                && (!routes.isEmpty() || !evidenceReferences.isEmpty())) {
            throw new IllegalArgumentException("MISSING_FIXTURE cannot claim executable route/evidence");
        }
    }

    private static List<String> clean(List<String> values) {
        return List.copyOf(values == null ? List.of() : values).stream()
                .map(value -> Validation.requireNonBlank(value, "readiness value"))
                .distinct()
                .sorted()
                .toList();
    }
}
