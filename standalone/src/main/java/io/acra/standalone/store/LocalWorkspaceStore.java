package io.acra.standalone.store;

import io.acra.standalone.model.ProjectRecord;
import io.acra.standalone.model.TargetRecord;

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

public final class LocalWorkspaceStore {
    private final Path root;

    public LocalWorkspaceStore(Path root) throws IOException {
        this.root = root.toAbsolutePath().normalize();
        Files.createDirectories(projectsRoot());
    }

    public Path root() {
        return root;
    }

    public synchronized ProjectRecord createProject(String name, String description) throws IOException {
        ProjectRecord record = new ProjectRecord(UUID.randomUUID(), name, description, Instant.now());
        Path dir = projectDir(record.id());
        Files.createDirectories(dir.resolve("targets"));
        Properties properties = new Properties();
        properties.setProperty("id", record.id().toString());
        properties.setProperty("name", record.name());
        properties.setProperty("description", record.description());
        properties.setProperty("createdAt", record.createdAt().toString());
        storeAtomic(dir.resolve("project.properties"), properties);
        return record;
    }

    public synchronized List<ProjectRecord> listProjects() throws IOException {
        List<ProjectRecord> records = new ArrayList<>();
        if (!Files.isDirectory(projectsRoot())) return records;
        try (var stream = Files.list(projectsRoot())) {
            for (Path dir : stream.filter(Files::isDirectory).toList()) {
                Path file = dir.resolve("project.properties");
                if (Files.isRegularFile(file)) records.add(readProject(file));
            }
        }
        records.sort(Comparator.comparing(ProjectRecord::createdAt));
        return List.copyOf(records);
    }

    public synchronized TargetRecord addTarget(
            UUID projectId,
            String displayName,
            String baseUrl,
            String environment,
            String authorizationReference,
            String testingMode
    ) throws IOException {
        requireProject(projectId);
        TargetRecord record = TargetRecord.create(
                projectId, displayName, baseUrl, environment, authorizationReference, testingMode);
        Path targetDir = projectDir(projectId).resolve("targets");
        Files.createDirectories(targetDir);
        Properties properties = new Properties();
        properties.setProperty("id", record.id().toString());
        properties.setProperty("projectId", record.projectId().toString());
        properties.setProperty("displayName", record.displayName());
        properties.setProperty("baseUri", record.baseUri().toString());
        properties.setProperty("environment", record.environment());
        properties.setProperty("authorizationReference", record.authorizationReference());
        properties.setProperty("testingMode", record.testingMode());
        properties.setProperty("createdAt", record.createdAt().toString());
        storeAtomic(targetDir.resolve(record.id() + ".properties"), properties);
        return record;
    }

    public synchronized List<TargetRecord> listTargets(UUID projectId) throws IOException {
        requireProject(projectId);
        Path targetDir = projectDir(projectId).resolve("targets");
        if (!Files.isDirectory(targetDir)) return List.of();
        List<TargetRecord> records = new ArrayList<>();
        try (var stream = Files.list(targetDir)) {
            for (Path file : stream.filter(p -> p.getFileName().toString().endsWith(".properties")).toList()) {
                records.add(readTarget(file));
            }
        }
        records.sort(Comparator.comparing(TargetRecord::createdAt));
        return List.copyOf(records);
    }

    private void requireProject(UUID projectId) throws IOException {
        if (!Files.isRegularFile(projectDir(projectId).resolve("project.properties"))) {
            throw new IllegalArgumentException("unknown project");
        }
    }

    private Path projectsRoot() {
        return root.resolve("projects");
    }

    private Path projectDir(UUID projectId) {
        Path resolved = projectsRoot().resolve(projectId.toString()).normalize();
        if (!resolved.startsWith(projectsRoot())) throw new IllegalArgumentException("invalid project id");
        return resolved;
    }

    private static ProjectRecord readProject(Path file) throws IOException {
        Properties p = load(file);
        return new ProjectRecord(
                UUID.fromString(p.getProperty("id")),
                p.getProperty("name"),
                p.getProperty("description", ""),
                Instant.parse(p.getProperty("createdAt")));
    }

    private static TargetRecord readTarget(Path file) throws IOException {
        Properties p = load(file);
        return new TargetRecord(
                UUID.fromString(p.getProperty("id")),
                UUID.fromString(p.getProperty("projectId")),
                p.getProperty("displayName"),
                java.net.URI.create(p.getProperty("baseUri")),
                p.getProperty("environment"),
                p.getProperty("authorizationReference"),
                p.getProperty("testingMode"),
                Instant.parse(p.getProperty("createdAt")));
    }

    private static Properties load(Path file) throws IOException {
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            p.load(in);
        }
        return p;
    }

    private static void storeAtomic(Path file, Properties properties) throws IOException {
        Files.createDirectories(file.getParent());
        Path temp = Files.createTempFile(file.getParent(), file.getFileName().toString(), ".tmp");
        try (OutputStream out = Files.newOutputStream(temp)) {
            properties.store(out, "ACRA local workspace");
        }
        try {
            Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
