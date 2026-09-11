package io.acra.core.active.analysis;

import io.acra.core.active.model.SecurityTest;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.authorization.AuthorizationMatrix;
import io.acra.core.domain.authorization.AuthorizationMatrixEntry;
import java.util.ArrayList;
import java.util.List;

public final class ExpectedDecisionCandidateFactory {
    public ExpectedDecisionCandidate groundTruth(AuthorizationDecision decision, String groundTruthId, List<String> evidenceIds) {
        return new ExpectedDecisionCandidate(decision, ExpectedDecisionSource.ACRA_LAB_GROUND_TRUTH,
                groundTruthId, evidenceIds, 1.0);
    }

    public ExpectedDecisionCandidate configuredPolicy(AuthorizationDecision decision, String policyId, List<String> evidenceIds) {
        return new ExpectedDecisionCandidate(decision, ExpectedDecisionSource.EXPLICIT_CONFIGURED_POLICY,
                policyId, evidenceIds, 0.95);
    }

    public List<ExpectedDecisionCandidate> fromMatrix(AuthorizationMatrix matrix, SecurityTest test) {
        if (matrix == null || test == null) throw new IllegalArgumentException("matrix and test required");
        String resource = test.targetResource() == null ? "UNKNOWN"
                : test.targetResource().resourceType() + ':' + test.targetResource().resourceId();
        List<ExpectedDecisionCandidate> candidates = new ArrayList<>();
        for (AuthorizationMatrixEntry entry : matrix.entries()) {
            boolean principal = entry.principal().equals(test.targetContext().principal());
            boolean action = entry.action().equalsIgnoreCase(test.targetContext().action());
            boolean endpoint = entry.endpoint().equals(test.endpoint().endpointId())
                    || entry.endpoint().equals(test.endpoint().method() + " " + test.endpoint().routeTemplate());
            boolean resourceMatch = entry.resource().equals(resource)
                    || entry.resource().equals(test.targetContext().resource());
            if (principal && action && endpoint && resourceMatch && entry.expected() != AuthorizationDecision.UNKNOWN) {
                candidates.add(new ExpectedDecisionCandidate(entry.expected(), ExpectedDecisionSource.AUTHORIZATION_MATRIX,
                        "matrix:" + test.testId(), entry.evidenceIds(), 0.75));
            }
        }
        return List.copyOf(candidates);
    }

    public ExpectedDecisionCandidate validatedMetadata(AuthorizationDecision decision, String metadataId, List<String> evidenceIds) {
        return new ExpectedDecisionCandidate(decision, ExpectedDecisionSource.VALIDATED_API_METADATA,
                metadataId, evidenceIds, 0.65);
    }

    public ExpectedDecisionCandidate inferred(AuthorizationDecision decision, String inferenceId, List<String> evidenceIds) {
        return new ExpectedDecisionCandidate(decision, ExpectedDecisionSource.INFERRED_POLICY,
                inferenceId, evidenceIds, 0.40);
    }
}
