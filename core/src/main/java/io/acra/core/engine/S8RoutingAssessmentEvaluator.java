package io.acra.core.engine;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.route.RouteAuthorizationAssessment;
import io.acra.core.route.RouteAuthorizationAssessmentState;
import io.acra.core.route.RouteBoundaryTransition;
import io.acra.core.route.RouteSecurityBoundaryState;
import io.acra.core.security.TokenFingerprint;
import java.util.ArrayList;
import java.util.List;

public final class S8RoutingAssessmentEvaluator {

    public RouteAuthorizationAssessment evaluate(
            RouteBoundaryTransition transition,
            AuthorizationDecision expectedDecision,
            AuthorizationDecision observedDecision,
            List<String> evidenceIds) {
        if (transition == null) throw new IllegalArgumentException("transition required");
        expectedDecision = expectedDecision == null ? AuthorizationDecision.UNKNOWN : expectedDecision;
        observedDecision = observedDecision == null ? AuthorizationDecision.UNKNOWN : observedDecision;

        List<String> reasons = new ArrayList<>(transition.reasons());
        RouteAuthorizationAssessmentState state;
        String rationale;

        if (transition.state() == RouteSecurityBoundaryState.INCONCLUSIVE
                || expectedDecision == AuthorizationDecision.UNKNOWN
                || observedDecision == AuthorizationDecision.UNKNOWN) {
            state = RouteAuthorizationAssessmentState.INCONCLUSIVE;
            rationale = "Routing attribution or authorization evidence is incomplete";
        } else if (!transition.routingChanged()) {
            state = RouteAuthorizationAssessmentState.NO_VIOLATION;
            rationale = "No routing representation/boundary change is present for Sprint 8 assessment";
        } else if (expectedDecision == AuthorizationDecision.DENY
                && observedDecision == AuthorizationDecision.ALLOW) {
            state = RouteAuthorizationAssessmentState.CANDIDATE;
            rationale = "Observed ALLOW conflicts with expected DENY on an evidence-backed routing change";
        } else {
            state = RouteAuthorizationAssessmentState.NO_VIOLATION;
            rationale = "Observed authorization behavior does not establish a routing authorization bypass candidate";
        }

        String material = transition.fromStage() + "|" + transition.toStage() + "|"
                + transition.pathDivergence() + "|" + expectedDecision + "|" + observedDecision + "|" + state;
        return new RouteAuthorizationAssessment(
                "s8-route-assessment-" + TokenFingerprint.sha256(material).substring(0, 24),
                transition.fromStage(),
                transition.toStage(),
                transition.pathDivergence(),
                expectedDecision,
                observedDecision,
                state,
                evidenceIds,
                reasons,
                rationale);
    }
}
