package io.acra.standalone.store;

import io.acra.core.domain.http.HttpMethod;
import io.acra.standalone.model.InventoryRecord;
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
import java.util.Set;
import java.util.TreeSet;
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
        Files.createDirectories(dir.resolve("inventory"));
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
        writeTarget(targetDir.resolve(record.id() + ".properties"), record);
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

    public synchronized TargetRecord findTarget(UUID projectId, UUID targetId) throws IOException {
        requireProject(projectId);
        if (targetId == null) throw new IllegalArgumentException("targetId is required");
        Path file = projectDir(projectId).resolve("targets").resolve(targetId + ".properties").normalize();
        Path targetRoot = projectDir(projectId).resolve("targets").normalize();
        if (!file.startsWith(targetRoot) || !Files.isRegularFile(file)) {
            throw new IllegalArgumentException("unknown target");
        }
        TargetRecord record = readTarget(file);
        if (!record.projectId().equals(projectId)) throw new IllegalArgumentException("target does not belong to project");
        return record;
    }

    public synchronized InventoryRecord upsertInventory(InventoryRecord candidate) throws IOException {
        requireProject(candidate.projectId());
        findTarget(candidate.projectId(), candidate.targetId());

        Path inventoryDir = projectDir(candidate.projectId()).resolve("inventory");
        Files.createDirectories(inventoryDir);
        Path file = inventoryDir.resolve(candidate.id() + ".properties").normalize();
        if (!file.startsWith(inventoryDir.normalize())) throw new IllegalArgumentException("invalid inventory id");

        InventoryRecord merged = Files.isRegularFile(file)
                ? readInventory(file).merge(candidate)
                : candidate;
        writeInventory(file, merged);
        return merged;
    }

    public synchronized List<InventoryRecord> listInventory(UUID projectId) throws IOException {
        requireProject(projectId);
        Path inventoryDir = projectDir(projectId).resolve("inventory");
        if (!Files.isDirectory(inventoryDir)) return List.of();

        List<InventoryRecord> records = new ArrayList<>();
        try (var stream = Files.list(inventoryDir)) {
            for (Path file : stream.filter(p -> p.getFileName().toString().endsWith(".properties")).toList()) {
                records.add(readInventory(file));
            }
        }
        records.sort(Comparator
                .comparing((InventoryRecord r) -> r.method().name())
                .thenComparing(InventoryRecord::canonicalPath)
                .thenComparing(InventoryRecord::host));
        return List.copyOf(records);
    }

    private void requireProject(UUID projectId) throws IOException {
        if (projectId == null || !Files.isRegularFile(projectDir(projectId).resolve("project.properties"))) {
            throw new IllegalArgumentException("unknown project");
        }
    }

    private Path projectsRoot() {
        return root.resolve("projects");
    }

    private Path projectDir(UUID projectId) {
        if (projectId == null) throw new IllegalArgumentException("project id is required");
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

    private static void writeTarget(Path file, TargetRecord record) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("id", record.id().toString());
        properties.setProperty("projectId", record.projectId().toString());
        properties.setProperty("displayName", record.displayName());
        properties.setProperty("baseUri", record.baseUri().toString());
        properties.setProperty("environment", record.environment());
        properties.setProperty("authorizationReference", record.authorizationReference());
        properties.setProperty("testingMode", record.testingMode());
        properties.setProperty("createdAt", record.createdAt().toString());
        storeAtomic(file, properties);
    }

    private static InventoryRecord readInventory(Path file) throws IOException {
        Properties p = load(file);
        return new InventoryRecord(
                p.getProperty("id"),
                UUID.fromString(p.getProperty("projectId")),
                UUID.fromString(p.getProperty("targetId")),
                HttpMethod.parse(p.getProperty("method")),
                p.getProperty("scheme"),
                p.getProperty("host"),
                Integer.parseInt(p.getProperty("port")),
                p.getProperty("rawPath"),
                p.getProperty("canonicalPath"),
                stringSet(p.getProperty("sourceTypes", "")),
                Long.parseLong(p.getProperty("observationCount")),
                Boolean.parseBoolean(p.getProperty("documented")),
                integerSet(p.getProperty("responseStatuses", "")),
                Instant.parse(p.getProperty("firstSeen")),
                Instant.parse(p.getProperty("lastSeen"))
        );
    }

    private static void writeInventory(Path file, InventoryRecord record) throws IOException {
        Properties p = new Properties();
        p.setProperty("id", record.id());
        p.setProperty("projectId", record.projectId().toString());
        p.setProperty("targetId", record.targetId().toString());
        p.setProperty("method", record.method().name());
        p.setProperty("scheme", record.scheme());
        p.setProperty("host", record.host());
        p.setProperty("port", Integer.toString(record.port()));
        p.setProperty("rawPath", record.rawPath());
        p.setProperty("canonicalPath", record.canonicalPath());
        p.setProperty("sourceTypes", String.join(",", new TreeSet<>(record.sourceTypes())));
        p.setProperty("observationCount", Long.toString(record.observationCount()));
        p.setProperty("documented", Boolean.toString(record.documented()));
        p.setProperty("responseStatuses", record.responseStatuses().stream()
                .sorted().map(String::valueOf).collect(java.util.stream.Collectors.joining(",")));
        p.setProperty("firstSeen", record.firstSeen().toString());
        p.setProperty("lastSeen", record.lastSeen().toString());
        storeAtomic(file, p);
    }

    private static Set<String> stringSet(String value) {
        TreeSet<String> out = new TreeSet<>();
        if (value == null || value.isBlank()) return out;
        for (String item : value.split(",")) {
            String normalized = item.strip();
            if (!normalized.isBlank()) out.add(normalized);
        }
        return out;
    }

    private static Set<Integer> integerSet(String value) {
        TreeSet<Integer> out = new TreeSet<>();
        if (value == null || value.isBlank()) return out;
        for (String item : value.split(",")) {
            String normalized = item.strip();
            if (!normalized.isBlank()) out.add(Integer.parseInt(normalized));
        }
        return out;
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
