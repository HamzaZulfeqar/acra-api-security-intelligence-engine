package io.acra.core.active.analysis;

import io.acra.core.active.evidence.ResponseSnapshot;
import io.acra.core.analysis.PassiveDifferentialComparator;
import io.acra.core.analysis.ResponseComparisonMode;
import io.acra.core.analysis.semantic.ResourceSemanticMatcher;
import io.acra.core.domain.authorization.AuthorizationDecision;
import java.util.ArrayList;
import java.util.List;

public final class MultiWayDifferentialAnalyzer {
    private final PassiveDifferentialComparator comparator = new PassiveDifferentialComparator();
    private final ResourceSemanticMatcher resourceMatcher = new ResourceSemanticMatcher();
    private final AuthorizationOutcomeNormalizer normalizer = new AuthorizationOutcomeNormalizer();

    public MultiWayDifferential compare(
            ResponseSnapshot baseline,
            ResponseSnapshot positive,
            ResponseSnapshot negative,
            ResponseSnapshot mutation,
            ExpectedDecisionResolution expected) {
        if (baseline == null || positive == null || negative == null || mutation == null || expected == null) {
            throw new IllegalArgumentException("four responses and expected decision required");
        }
        AuthorizationOutcome baselineOutcome = outcome(baseline);
        AuthorizationOutcome positiveOutcome = outcome(positive);
        AuthorizationOutcome negativeOutcome = outcome(negative);
        AuthorizationOutcome mutationOutcome = outcome(mutation);
        List<PairwiseDifference> differences = List.of(
                pair("baseline", baseline, "positive", positive),
                pair("baseline", baseline, "negative", negative),
                pair("baseline", baseline, "mutation", mutation),
                pair("positive", positive, "mutation", mutation),
                pair("negative", negative, "mutation", mutation));
        List<String> reasons = new ArrayList<>();
        DifferentialClassification classification;
        if (positiveOutcome != AuthorizationOutcome.ALLOW || !negativeOutcome.deniedEquivalent()) {
            classification = DifferentialClassification.INCONCLUSIVE;
            reasons.add("positive or negative control did not establish a valid comparison");
        } else if (expected.conflict() || expected.decision() == AuthorizationDecision.AMBIGUOUS) {
            classification = DifferentialClassification.INCONCLUSIVE;
            reasons.add("expected-decision evidence is conflicting");
        } else if (expected.decision() == AuthorizationDecision.UNKNOWN) {
            classification = DifferentialClassification.INCONCLUSIVE;
            reasons.add("expected policy is unknown; interpretation is suspended");
        } else {
            boolean baselineEquivalent = differences.get(2).responseDifference().equivalent();
            boolean expectedMatch = matches(expected.decision(), mutationOutcome);
            if (expectedMatch && baselineEquivalent) {
                classification = DifferentialClassification.NO_CHANGE;
                reasons.add("mutation response remained equivalent and matched expected policy");
            } else if (expectedMatch) {
                classification = DifferentialClassification.EXPECTED_CHANGE;
                reasons.add("mutation response changed in the expected policy direction");
            } else {
                classification = DifferentialClassification.UNEXPECTED_CHANGE;
                reasons.add("mutation outcome did not match the resolved expected decision");
            }
        }
        return new MultiWayDifferential(baselineOutcome, positiveOutcome, negativeOutcome, mutationOutcome,
                differences, classification, reasons);
    }

    private PairwiseDifference pair(String leftName, ResponseSnapshot left, String rightName, ResponseSnapshot right) {
        return new PairwiseDifference(leftName, rightName,
                comparator.compare(left.response(), right.response(), ResponseComparisonMode.SEMANTIC),
                resourceMatcher.compare(left.semanticFingerprint(), right.semanticFingerprint()),
                outcome(left), outcome(right));
    }

    private AuthorizationOutcome outcome(ResponseSnapshot snapshot) {
        return normalizer.normalize(snapshot.response(), snapshot.semanticFingerprint());
    }

    private static boolean matches(AuthorizationDecision decision, AuthorizationOutcome outcome) {
        return switch (decision) {
            case ALLOW -> outcome == AuthorizationOutcome.ALLOW || outcome == AuthorizationOutcome.PARTIAL;
            case DENY -> outcome.deniedEquivalent();
            case ERROR -> outcome == AuthorizationOutcome.ERROR;
            case CONDITIONAL -> outcome == AuthorizationOutcome.PARTIAL;
            case AMBIGUOUS, UNKNOWN -> false;
        };
    }
}
