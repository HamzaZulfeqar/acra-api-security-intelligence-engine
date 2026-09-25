package io.acra.core.route;

import io.acra.core.domain.authorization.AuthorizationDecision;
import java.util.List;

public record RouteAuthorizationAssessment(
        String assessmentId,
        RouteProcessingStage fromStage,
        RouteProcessingStage toStage,
        RouteNormalizationDivergenceKind pathDivergence,
        AuthorizationDecision expectedDecision,
        AuthorizationDecision observedDecision,
        RouteAuthorizationAssessmentState state,
        List<String> evidenceIds,
        List<String> reasons,
        String rationale) {

    public RouteAuthorizationAssessment {
        assessmentId = required(assessmentId, "assessmentId");
        if (fromStage == null || toStage == null || pathDivergence == null) {
            throw new IllegalArgumentException("route assessment stages/divergence required");
        }
        expectedDecision = expectedDecision == null ? AuthorizationDecision.UNKNOWN : expectedDecision;
        observedDecision = observedDecision == null ? AuthorizationDecision.UNKNOWN : observedDecision;
        state = state == null ? RouteAuthorizationAssessmentState.INCONCLUSIVE : state;
        evidenceIds = sorted(evidenceIds);
        reasons = sorted(reasons);
        rationale = rationale == null ? "" : rationale;
    }

    public boolean violationCandidate() {
        return state == RouteAuthorizationAssessmentState.CANDIDATE
                && expectedDecision == AuthorizationDecision.DENY
                && observedDecision == AuthorizationDecision.ALLOW;
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " required");
        return value;
    }

    private static List<String> sorted(List<String> values) {
        return List.copyOf(values == null ? List.<String>of() : values).stream()
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .sorted()
                .toList();
    }
}
