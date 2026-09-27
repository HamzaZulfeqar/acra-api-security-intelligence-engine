package io.acra.standalone.store;

import io.acra.standalone.model.CandidateReviewRecord;
import io.acra.standalone.model.CandidateReviewState;

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

public final class CandidateReviewStore {
    private final Path projectsRoot;

    public CandidateReviewStore(LocalWorkspaceStore workspace) {
        this.projectsRoot = java.util.Objects.requireNonNull(workspace)
                .root().resolve("projects").toAbsolutePath().normalize();
    }

    public synchronized CandidateReviewRecord save(CandidateReviewRecord record) throws IOException {
        requireProject(record.projectId());
        Path dir = reviewRoot(record.projectId());
        Properties p = new Properties();
        p.setProperty("projectId", record.projectId().toString());
        p.setProperty("candidateId", record.candidateId());
        p.setProperty("state", record.state().name());
        p.setProperty("note", record.note());
        p.setProperty("updatedAt", record.updatedAt().toString());
        storeAtomic(dir.resolve(safeName(record.candidateId()) + ".properties"), p);
        return record;
    }

    public synchronized List<CandidateReviewRecord> list(UUID projectId) throws IOException {
        requireProject(projectId);
        Path dir = reviewRoot(projectId);
        if (!Files.isDirectory(dir)) return List.of();
        List<CandidateReviewRecord> records = new ArrayList<>();
        try (var stream = Files.list(dir)) {
            for (Path file : stream.filter(path -> path.getFileName().toString().endsWith(".properties")).toList()) {
                Properties p = load(file);
                records.add(new CandidateReviewRecord(
                        UUID.fromString(p.getProperty("projectId")),
                        p.getProperty("candidateId"),
                        CandidateReviewState.valueOf(p.getProperty("state")),
                        p.getProperty("note", ""),
                        Instant.parse(p.getProperty("updatedAt"))
                ));
            }
        }
        records.sort(Comparator.comparing(CandidateReviewRecord::candidateId));
        return List.copyOf(records);
    }

    public synchronized CandidateReviewRecord find(UUID projectId, String candidateId) throws IOException {
        return list(projectId).stream()
                .filter(record -> record.candidateId().equals(candidateId))
                .findFirst().orElse(null);
    }

    private void requireProject(UUID projectId) {
        if (projectId == null) throw new IllegalArgumentException("projectId is required");
        Path project = projectsRoot.resolve(projectId.toString()).normalize();
        if (!project.startsWith(projectsRoot) || !Files.isRegularFile(project.resolve("project.properties"))) {
            throw new IllegalArgumentException("unknown project");
        }
    }

    private Path reviewRoot(UUID projectId) throws IOException {
        Path project = projectsRoot.resolve(projectId.toString()).normalize();
        Path root = project.resolve("candidate-reviews").normalize();
        if (!root.startsWith(project)) throw new IllegalArgumentException("invalid review path");
        Files.createDirectories(root);
        return root;
    }

    private static String safeName(String candidateId) {
        String safe = candidateId == null ? "" : candidateId.replaceAll("[^A-Za-z0-9._-]", "_");
        if (safe.isBlank()) throw new IllegalArgumentException("candidateId is invalid");
        return safe;
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
            properties.store(out, "ACRA standalone candidate review");
        }
        try {
            Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
