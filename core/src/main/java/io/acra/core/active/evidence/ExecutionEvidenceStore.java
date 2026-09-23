package io.acra.core.active.evidence;

import io.acra.core.active.analysis.MultiWayDifferential;
import io.acra.core.security.TokenFingerprint;
import io.acra.core.serialization.DomainSerializer;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import io.acra.core.domain.common.Validation;

public final class ExecutionEvidenceStore {
    private final Clock clock;
    private final String projectId;
    private final AtomicLong sequence = new AtomicLong();
    private final Map<String, Object> objects = new LinkedHashMap<>();
    private final List<EvidenceChainEntry> chain = new ArrayList<>();
    private final DomainSerializer serializer = new DomainSerializer();

    public ExecutionEvidenceStore() {
        this(Clock.systemUTC(), null);
    }

    public ExecutionEvidenceStore(Clock clock) {
        this(clock, null);
    }

    public ExecutionEvidenceStore(String projectId) {
        this(Clock.systemUTC(), projectId);
    }

    public ExecutionEvidenceStore(Clock clock, String projectId) {
        if (clock == null) throw new IllegalArgumentException("clock required");
        this.clock = clock;
        this.projectId = projectId == null ? null : Validation.requireNonBlank(projectId, "projectId");
    }

    public String projectId() {
        return projectId;
    }

    public synchronized EvidenceChainEntry append(String executionId, String testId, EvidenceStage stage,
                                                   String objectId, Object value) {
        if (value == null) throw new IllegalArgumentException("evidence value required");
        objectId = Validation.requireNonBlank(objectId, "objectId");
        if (objects.putIfAbsent(objectId, value) != null) throw new IllegalStateException("immutable evidence object already exists");
        String fingerprint = TokenFingerprint.sha256(serializer.serialize(value));
        EvidenceChainEntry entry = new EvidenceChainEntry(
                String.format("S4-EVIDENCE-%08d", sequence.incrementAndGet()), executionId, testId,
                stage, objectId, fingerprint, clock.instant());
        chain.add(entry);
        return entry;
    }

    public synchronized boolean contains(String objectId) {
        return objectId != null && objects.containsKey(objectId);
    }

    public synchronized List<EvidenceChainEntry> chainForObject(String objectId) {
        return chain.stream().filter(entry -> entry.objectId().equals(objectId)).toList();
    }

    public synchronized boolean containsEvidenceId(String evidenceId) {
        return evidenceId != null && chain.stream().anyMatch(entry -> entry.evidenceId().equals(evidenceId));
    }

    public synchronized List<EvidenceChainEntry> chainForEvidenceId(String evidenceId) {
        return chain.stream().filter(entry -> entry.evidenceId().equals(evidenceId)).toList();
    }

    public synchronized Object object(String objectId) {
        Object value = objects.get(objectId);
        if (value == null) throw new IllegalArgumentException("unknown evidence object");
        return value;
    }

    public synchronized List<EvidenceChainEntry> chain(String executionId) {
        return chain.stream().filter(entry -> entry.executionId().equals(executionId)).toList();
    }

    public synchronized List<Observation> observations() {
        return objects.values().stream().filter(Observation.class::isInstance).map(Observation.class::cast).toList();
    }

    public synchronized List<MultiWayDifferential> differentials() {
        return objects.values().stream().filter(MultiWayDifferential.class::isInstance).map(MultiWayDifferential.class::cast).toList();
    }
}
