package io.acra.core.extraction;

import io.acra.core.domain.evidence.Evidence;
import java.util.*;

public record ExtractionResult<T>(ResolutionStatus status, List<T> candidates, List<Evidence> evidence, String reason) {
    public ExtractionResult {
        if (status == null) status = ResolutionStatus.UNKNOWN;
        candidates = List.copyOf(candidates == null ? List.of() : candidates);
        evidence = List.copyOf(evidence == null ? List.of() : evidence);
        reason = reason == null ? "" : reason;
        if (status == ResolutionStatus.RESOLVED && candidates.size() != 1)
            throw new IllegalArgumentException("RESOLVED extraction must contain exactly one candidate");
        if (status == ResolutionStatus.CONFLICTING_EVIDENCE && candidates.size() < 2)
            throw new IllegalArgumentException("CONFLICTING_EVIDENCE requires at least two candidates");
    }
    public Optional<T> resolved() { return status == ResolutionStatus.RESOLVED ? Optional.of(candidates.getFirst()) : Optional.empty(); }
    public static <T> ExtractionResult<T> resolved(T candidate, List<Evidence> evidence, String reason) {
        return new ExtractionResult<>(ResolutionStatus.RESOLVED, List.of(candidate), evidence, reason);
    }
    public static <T> ExtractionResult<T> unknown(String reason) { return new ExtractionResult<>(ResolutionStatus.UNKNOWN, List.of(), List.of(), reason); }
    public static <T> ExtractionResult<T> conflict(List<T> candidates, List<Evidence> evidence, String reason) {
        return new ExtractionResult<>(ResolutionStatus.CONFLICTING_EVIDENCE, candidates, evidence, reason);
    }
}
