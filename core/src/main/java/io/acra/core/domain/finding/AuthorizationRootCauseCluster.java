package io.acra.core.domain.finding;

import java.util.List;

public record AuthorizationRootCauseCluster(
        String clusterId,
        String rootCauseKey,
        List<String> candidateIds,
        List<String> dimensions,
        List<String> policyReferences,
        List<String> endpoints,
        List<String> resources) {

    public AuthorizationRootCauseCluster {
        candidateIds = sorted(candidateIds);
        dimensions = sorted(dimensions);
        policyReferences = sorted(policyReferences);
        endpoints = sorted(endpoints);
        resources = sorted(resources);
    }

    private static List<String> sorted(List<String> values) {
        return List.copyOf(values == null ? List.<String>of() : values).stream()
                .filter(v -> v != null && !v.isBlank()).distinct().sorted().toList();
    }
}
