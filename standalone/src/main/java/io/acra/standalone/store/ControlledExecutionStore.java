package io.acra.standalone.store;

import io.acra.core.active.analysis.AuthorizationOutcome;
import io.acra.core.active.analysis.DifferentialClassification;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.testing.TestState;
import io.acra.standalone.model.ControlledExecutionRecord;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

public final class ControlledExecutionStore {
    private final Path projectsRoot;

    public ControlledExecutionStore(LocalWorkspaceStore workspace) {
        this.projectsRoot = java.util.Objects.requireNonNull(workspace)
                .root().resolve("projects").toAbsolutePath().normalize();
    }

    public synchronized ControlledExecutionRecord save(ControlledExecutionRecord record) throws IOException {
        requireProject(record.projectId());
        Path root = executionRoot(record.projectId());
        Properties p = new Properties();
        p.setProperty("runId", record.runId().toString());
        p.setProperty("projectId", record.projectId().toString());
        p.setProperty("targetId", record.targetId().toString());
        p.setProperty("expectationId", record.expectationId().toString());
        p.setProperty("testId", record.testId());
        p.setProperty("executionId", record.executionId());
        p.setProperty("observationId", record.observationId());
        p.setProperty("endpoint", record.endpoint());
        p.setProperty("requestPath", record.requestPath());
        p.setProperty("mutatedPath", record.mutatedPath());
        p.setProperty("expectedDecision", record.expectedDecision().name());
        p.setProperty("observedDecision", record.observedDecision().name());
        p.setProperty("differentialClassification", record.differentialClassification().name());
        p.setProperty("state", record.state().name());
        p.setProperty("evidenceArtifactId", record.evidenceArtifactId().toString());
        p.setProperty("coreEvidenceObjectCount", Integer.toString(record.coreEvidenceObjectCount()));
        p.setProperty("createdAt", record.createdAt().toString());
        storeAtomic(root.resolve(record.runId() + ".properties"), p);
        return record;
    }

    public synchronized List<ControlledExecutionRecord> list(UUID projectId) throws IOException {
        requireProject(projectId);
        Path root = executionRoot(projectId);
        if (!Files.isDirectory(root)) return List.of();
        List<ControlledExecutionRecord> out = new ArrayList<>();
        try (var stream = Files.list(root)) {
            for (Path file : stream.filter(path -> path.getFileName().toString().endsWith(".properties")).toList()) {
                out.add(read(file));
            }
        }
        out.sort(Comparator.comparing(ControlledExecutionRecord::createdAt)
                .thenComparing(record -> record.runId().toString()));
        return List.copyOf(out);
    }

    public synchronized ControlledExecutionRecord latestForExpectation(UUID projectId, UUID expectationId)
            throws IOException {
        ControlledExecutionRecord latest = null;
        for (ControlledExecutionRecord record : list(projectId)) {
            if (!record.expectationId().equals(expectationId)) continue;
            if (latest == null || record.createdAt().isAfter(latest.createdAt())) latest = record;
        }
        return latest;
    }

    private ControlledExecutionRecord read(Path file) throws IOException {
        Properties p = load(file);
        return new ControlledExecutionRecord(
                UUID.fromString(p.getProperty("runId")),
                UUID.fromString(p.getProperty("projectId")),
                UUID.fromString(p.getProperty("targetId")),
                UUID.fromString(p.getProperty("expectationId")),
                p.getProperty("testId"),
                p.getProperty("executionId"),
                p.getProperty("observationId", ""),
                p.getProperty("endpoint"),
                p.getProperty("requestPath"),
                p.getProperty("mutatedPath"),
                AuthorizationDecision.valueOf(p.getProperty("expectedDecision")),
                AuthorizationOutcome.valueOf(p.getProperty("observedDecision")),
                DifferentialClassification.valueOf(p.getProperty("differentialClassification")),
                TestState.valueOf(p.getProperty("state")),
                UUID.fromString(p.getProperty("evidenceArtifactId")),
                Integer.parseInt(p.getProperty("coreEvidenceObjectCount")),
                Instant.parse(p.getProperty("createdAt"))
        );
    }

    private void requireProject(UUID projectId) {
        if (projectId == null) throw new IllegalArgumentException("projectId is required");
        Path project = projectsRoot.resolve(projectId.toString()).normalize();
        if (!project.startsWith(projectsRoot) || !Files.isRegularFile(project.resolve("project.properties"))) {
            throw new IllegalArgumentException("unknown project");
        }
    }

    private Path executionRoot(UUID projectId) throws IOException {
        Path project = projectsRoot.resolve(projectId.toString()).normalize();
        Path root = project.resolve("active-executions").normalize();
        if (!root.startsWith(project)) throw new IllegalArgumentException("invalid active execution path");
        Files.createDirectories(root);
        return root;
    }

    private static Properties load(Path file) throws IOException {
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            p.load(in);
        }
        return p;
    }

    private static void storeAtomic(Path file, Properties properties) throws IOException {
        Path temp = Files.createTempFile(file.getParent(), file.getFileName().toString(), ".tmp");
        try (OutputStream out = Files.newOutputStream(temp)) {
            properties.store(out, "ACRA controlled active execution");
        }
        try {
            Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
