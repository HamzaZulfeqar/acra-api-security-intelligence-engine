package io.acra.core.active.model;

import io.acra.core.domain.common.Validation;
import io.acra.core.domain.http.HttpMethod;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public record TargetDescriptor(
        String projectId,
        String targetId,
        String scheme,
        String host,
        int port,
        ExecutionEnvironment environment,
        boolean authorized,
        List<String> allowedPathPrefixes,
        Set<HttpMethod> allowedMethods) {
    public TargetDescriptor {
        projectId = Validation.requireNonBlank(projectId, "projectId");
        targetId = Validation.requireNonBlank(targetId, "targetId");
        scheme = Validation.requireNonBlank(scheme, "scheme").toLowerCase(Locale.ROOT);
        host = Validation.requireNonBlank(host, "host").toLowerCase(Locale.ROOT);
        if (!Set.of("http", "https").contains(scheme)) throw new IllegalArgumentException("unsupported active scheme");
        if (port < 1 || port > 65535) throw new IllegalArgumentException("port must be 1-65535");
        if (environment == null) environment = ExecutionEnvironment.UNKNOWN;
        allowedPathPrefixes = List.copyOf(allowedPathPrefixes == null ? List.of() : allowedPathPrefixes);
        if (allowedPathPrefixes.stream().anyMatch(p -> p == null || p.isBlank() || !p.startsWith("/"))) {
            throw new IllegalArgumentException("allowed paths must be absolute path prefixes");
        }
        allowedMethods = Set.copyOf(allowedMethods == null ? Set.of() : allowedMethods);
    }

    public String authority() {
        return scheme + "://" + host + ":" + port;
    }
}
