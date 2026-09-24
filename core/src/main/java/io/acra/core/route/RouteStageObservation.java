package io.acra.core.route;

import io.acra.core.security.UniversalRedactor;
import java.util.List;

public record RouteStageObservation(
        String observationId,
        RouteProcessingStage stage,
        String path,
        RouteObservationSource source,
        List<String> evidenceIds) {

    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public RouteStageObservation {
        observationId = required(observationId, "observationId");
        if (stage == null) throw new IllegalArgumentException("stage required");
        path = required(path, "path");
        if (!path.startsWith("/")) throw new IllegalArgumentException("route stage path must be absolute");
        if (path.indexOf('?') >= 0 || path.indexOf('#') >= 0) {
            throw new IllegalArgumentException("route stage observation must contain path only");
        }
        if (source == null) throw new IllegalArgumentException("source required");
        evidenceIds = List.copyOf(evidenceIds == null ? List.<String>of() : evidenceIds).stream()
                .map(RouteStageObservation::safe)
                .filter(value -> !value.isBlank())
                .distinct()
                .sorted()
                .toList();
        if (evidenceIds.isEmpty()) {
            throw new IllegalArgumentException("stage observation requires provenance evidence");
        }
    }

    private static String required(String value, String name) {
        String safe = safe(value);
        if (safe.isBlank()) throw new IllegalArgumentException(name + " required");
        return safe;
    }

    private static String safe(String value) {
        return REDACTOR.redactText(value == null ? "" : value);
    }
}
