package io.acra.core.active.evidence;

import io.acra.core.domain.common.Validation;
import java.time.Instant;

public record EvidenceChainEntry(String evidenceId, String executionId, String testId, EvidenceStage stage,
                                 String objectId, String fingerprint, Instant timestamp) {
    public EvidenceChainEntry {
        evidenceId = Validation.requireNonBlank(evidenceId, "evidenceId");
        executionId = Validation.requireNonBlank(executionId, "executionId");
        testId = Validation.requireNonBlank(testId, "testId");
        if (stage == null || timestamp == null) throw new IllegalArgumentException("evidence stage/timestamp required");
        objectId = Validation.requireNonBlank(objectId, "objectId");
        fingerprint = Validation.requireNonBlank(fingerprint, "fingerprint");
    }
}
