package io.acra.standalone.model;

import io.acra.core.domain.http.HttpMethod;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

public record InventoryRecord(
        String id,
        UUID projectId,
        UUID targetId,
        HttpMethod method,
        String scheme,
        String host,
        int port,
        String rawPath,
        String canonicalPath,
        Set<String> sourceTypes,
        long observationCount,
        boolean documented,
        Set<Integer> responseStatuses,
        Instant firstSeen,
        Instant lastSeen
) {
    public InventoryRecord {
        id = requireText(id, "id");
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(targetId, "targetId");
        Objects.requireNonNull(method, "method");
        scheme = requireText(scheme, "scheme");
        host = requireText(host, "host");
        if (port < 1 || port > 65535) throw new IllegalArgumentException("port must be 1-65535");
        rawPath = normalizePath(rawPath);
        canonicalPath = normalizePath(canonicalPath);
        sourceTypes = Set.copyOf(sourceTypes == null ? Set.of() : new TreeSet<>(sourceTypes));
        if (observationCount < 1) throw new IllegalArgumentException("observationCount must be positive");
        responseStatuses = Set.copyOf(responseStatuses == null ? Set.of() : new TreeSet<>(responseStatuses));
        Objects.requireNonNull(firstSeen, "firstSeen");
        Objects.requireNonNull(lastSeen, "lastSeen");
    }

    public InventoryRecord merge(InventoryRecord other) {
        if (!id.equals(other.id)
                || !projectId.equals(other.projectId)
                || !targetId.equals(other.targetId)
                || method != other.method
                || !scheme.equals(other.scheme)
                || !host.equals(other.host)
                || port != other.port
                || !canonicalPath.equals(other.canonicalPath)) {
            throw new IllegalArgumentException("inventory identity mismatch");
        }
        TreeSet<String> sources = new TreeSet<>(sourceTypes);
        sources.addAll(other.sourceTypes);
        TreeSet<Integer> statuses = new TreeSet<>(responseStatuses);
        statuses.addAll(other.responseStatuses);
        return new InventoryRecord(
                id,
                projectId,
                targetId,
                method,
                scheme,
                host,
                port,
                rawPath,
                canonicalPath,
                sources,
                observationCount + other.observationCount,
                documented || other.documented,
                statuses,
                firstSeen.isBefore(other.firstSeen) ? firstSeen : other.firstSeen,
                lastSeen.isAfter(other.lastSeen) ? lastSeen : other.lastSeen
        );
    }

    private static String requireText(String value, String field) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) throw new IllegalArgumentException(field + " is required");
        return normalized;
    }

    private static String normalizePath(String value) {
        String normalized = requireText(value, "path");
        return normalized.startsWith("/") ? normalized : "/" + normalized;
    }
}
