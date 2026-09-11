package io.acra.core.active.evidence;

import io.acra.core.active.analysis.AuthorizationOutcome;
import io.acra.core.active.analysis.ExpectedDecisionResolution;
import io.acra.core.active.analysis.MultiWayDifferential;
import io.acra.core.domain.common.Validation;
import io.acra.core.recon.SecurityContextFingerprint;
import java.time.Instant;
import java.util.List;

public record Observation(
        String observationId,
        String testId,
        ResponseSnapshot baseline,
        ResponseSnapshot positiveControl,
        ResponseSnapshot negativeControl,
        ResponseSnapshot mutation,
        ExpectedDecisionResolution expectedDecision,
        AuthorizationOutcome observedDecision,
        MultiWayDifferential differences,
        SecurityContextFingerprint sourceContext,
        SecurityContextFingerprint targetContext,
        List<String> evidenceIds,
        double confidence,
        ExecutionFingerprint executionFingerprint,
        Instant createdAt) {
    public Observation {
        observationId = Validation.requireNonBlank(observationId, "observationId");
        testId = Validation.requireNonBlank(testId, "testId");
        if (baseline == null || positiveControl == null || negativeControl == null || mutation == null
                || expectedDecision == null || observedDecision == null || differences == null
                || sourceContext == null || targetContext == null || executionFingerprint == null || createdAt == null) {
            throw new IllegalArgumentException("complete observation evidence required");
        }
        evidenceIds = List.copyOf(evidenceIds == null ? List.of() : evidenceIds);
        if (confidence < 0.0 || confidence > 1.0) throw new IllegalArgumentException("confidence");
    }
}
