package io.acra.core.active.research;

import io.acra.core.domain.common.Validation;
import io.acra.core.security.TokenFingerprint;
import java.util.List;

public record S11PredictionExecutionSnapshot(
        String executionId,
        List<AblationPredictionResult> results,
        S11AblationCampaignPlan campaignPlan,
        String fingerprint) {

    public S11PredictionExecutionSnapshot {
        if (campaignPlan == null) throw new IllegalArgumentException("campaignPlan required");
        results = List.copyOf(results == null ? List.of() : results);
        if (results.size() != campaignPlan.cells().size()) {
            throw new IllegalArgumentException("prediction result count must match campaign cells");
        }
        if (campaignPlan.coverage().executedCells() != campaignPlan.cells().size()) {
            throw new IllegalArgumentException("prediction execution snapshot requires executed campaign cells");
        }
        if (results.stream().anyMatch(result -> result.state() == null)) {
            throw new IllegalArgumentException("prediction result state required");
        }

        String material = canonical(results, campaignPlan);
        String expectedExecutionId = "s11-prediction-exec-"
                + TokenFingerprint.sha256(material).substring(0, 24);
        executionId = executionId == null || executionId.isBlank() ? expectedExecutionId
                : Validation.requireNonBlank(executionId, "executionId");
        if (!executionId.equals(expectedExecutionId)) {
            throw new IllegalArgumentException("prediction execution identity mismatch");
        }

        String calculated = TokenFingerprint.sha256(executionId + "|" + material);
        fingerprint = fingerprint == null || fingerprint.isBlank() ? calculated : fingerprint;
        if (!fingerprint.equals(calculated)) {
            throw new IllegalArgumentException("prediction execution fingerprint mismatch");
        }
    }

    private static String canonical(
            List<AblationPredictionResult> results,
            S11AblationCampaignPlan campaignPlan) {
        StringBuilder out = new StringBuilder(campaignPlan.planId())
                .append('|').append(campaignPlan.fingerprint()).append('\n');
        for (AblationPredictionResult result : results) {
            out.append(result.caseId()).append('|')
                    .append(result.variantId()).append('|')
                    .append(result.state()).append('|')
                    .append(result.prediction()).append('|')
                    .append(result.fingerprint()).append('\n');
        }
        return out.toString();
    }
}
