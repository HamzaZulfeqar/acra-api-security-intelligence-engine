package io.acra.core.route;

import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.http.HttpMethod;
import io.acra.core.security.UniversalRedactor;
import java.util.List;

public record RouteBoundaryObservation(
        String observationId,
        RouteProcessingStage stage,
        String path,
        HttpMethod method,
        String host,
        String apiVersion,
        AuthorizationDecision expectedDecision,
        AuthorizationDecision observedDecision,
        String policyReference,
        RouteObservationSource source,
        List<String> evidenceIds) {

    private static final UniversalRedactor REDACTOR = new UniversalRedactor();

    public RouteBoundaryObservation {
        observationId = required(observationId, "observationId");
        if (stage == null) throw new IllegalArgumentException("stage required");
        path = required(path, "path");
        if (!path.startsWith("/")) throw new IllegalArgumentException("route boundary path must be absolute");
        if (path.indexOf('?') >= 0 || path.indexOf('#') >= 0) {
            throw new IllegalArgumentException("route boundary observation must contain path only");
        }
        if (method == null) throw new IllegalArgumentException("method required");
        host = required(host, "host");
        if (host.contains("://") || host.indexOf('/') >= 0 || host.indexOf('@') >= 0) {
            throw new IllegalArgumentException("host must not contain scheme, path or user-info");
        }
        apiVersion = safe(apiVersion);
        expectedDecision = expectedDecision == null ? AuthorizationDecision.UNKNOWN : expectedDecision;
        observedDecision = observedDecision == null ? AuthorizationDecision.UNKNOWN : observedDecision;
        policyReference = safe(policyReference);
        if (source == null) throw new IllegalArgumentException("source required");
        evidenceIds = List.copyOf(evidenceIds == null ? List.<String>of() : evidenceIds).stream()
                .map(RouteBoundaryObservation::safe)
                .filter(value -> !value.isBlank())
                .distinct()
                .sorted()
                .toList();
        if (evidenceIds.isEmpty()) {
            throw new IllegalArgumentException("route boundary observation requires provenance evidence");
        }
    }

    public RouteStageObservation routeObservation() {
        return new RouteStageObservation(
                observationId + "-route",
                stage,
                path,
                source,
                evidenceIds);
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
