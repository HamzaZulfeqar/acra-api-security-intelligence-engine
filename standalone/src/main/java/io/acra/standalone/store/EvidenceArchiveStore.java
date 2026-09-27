package io.acra.standalone.store;

import io.acra.standalone.model.EvidenceArtifactRecord;
import io.acra.standalone.model.HttpEvidenceSampleRecord;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

public final class EvidenceArchiveStore {
    private final LocalWorkspaceStore workspace;
    private final Path projectsRoot;

    public EvidenceArchiveStore(LocalWorkspaceStore workspace) {
        this.workspace = java.util.Objects.requireNonNull(workspace);
        this.projectsRoot = workspace.root().resolve("projects").toAbsolutePath().normalize();
    }

    public synchronized EvidenceArtifactRecord save(
            EvidenceArtifactRecord artifact,
            String redactedContent,
            List<HttpEvidenceSampleRecord> samples
    ) throws IOException {
        requireProject(artifact.projectId());
        workspace.findTarget(artifact.projectId(), artifact.targetId());

        Path dir = artifactDir(artifact.projectId(), artifact.evidenceId());
        Files.createDirectories(dir.resolve("samples"));

        Properties p = new Properties();
        p.setProperty("evidenceId", artifact.evidenceId().toString());
        p.setProperty("projectId", artifact.projectId().toString());
        p.setProperty("targetId", artifact.targetId().toString());
        p.setProperty("evidenceType", artifact.evidenceType());
        p.setProperty("sourceReference", artifact.sourceReference());
        p.setProperty("originalSha256", artifact.originalSha256());
        p.setProperty("redactionApplied", Boolean.toString(artifact.redactionApplied()));
        p.setProperty("originalLength", Integer.toString(artifact.originalLength()));
        p.setProperty("storedLength", Integer.toString(artifact.storedLength()));
        p.setProperty("httpSampleCount", Integer.toString(artifact.httpSampleCount()));
        p.setProperty("createdAt", artifact.createdAt().toString());
        storeAtomic(dir.resolve("metadata.properties"), p);
        writeTextAtomic(dir.resolve("content.redacted.txt"), redactedContent == null ? "" : redactedContent);

        for (HttpEvidenceSampleRecord sample : samples == null ? List.<HttpEvidenceSampleRecord>of() : samples) {
            if (!sample.evidenceId().equals(artifact.evidenceId())
                    || !sample.projectId().equals(artifact.projectId())
                    || !sample.targetId().equals(artifact.targetId())) {
                throw new IllegalArgumentException("evidence sample lineage mismatch");
            }
            saveSample(dir.resolve("samples"), sample);
        }
        return artifact;
    }

    public synchronized List<EvidenceArtifactRecord> listArtifacts(UUID projectId) throws IOException {
        requireProject(projectId);
        Path root = evidenceRoot(projectId);
        if (!Files.isDirectory(root)) return List.of();
        List<EvidenceArtifactRecord> records = new ArrayList<>();
        try (var stream = Files.list(root)) {
            for (Path dir : stream.filter(Files::isDirectory).toList()) {
                Path metadata = dir.resolve("metadata.properties");
                if (Files.isRegularFile(metadata)) records.add(readArtifact(metadata));
            }
        }
        records.sort(Comparator.comparing(EvidenceArtifactRecord::createdAt)
                .thenComparing(record -> record.evidenceId().toString()));
        return List.copyOf(records);
    }

    public synchronized List<HttpEvidenceSampleRecord> listSamples(UUID projectId) throws IOException {
        requireProject(projectId);
        List<HttpEvidenceSampleRecord> records = new ArrayList<>();
        for (EvidenceArtifactRecord artifact : listArtifacts(projectId)) {
            Path samplesDir = artifactDir(projectId, artifact.evidenceId()).resolve("samples");
            if (!Files.isDirectory(samplesDir)) continue;
            try (var stream = Files.list(samplesDir)) {
                for (Path metadata : stream
                        .filter(path -> path.getFileName().toString().endsWith(".properties")).toList()) {
                    records.add(readSample(metadata));
                }
            }
        }
        records.sort(Comparator.comparing(HttpEvidenceSampleRecord::createdAt)
                .thenComparing(record -> record.sampleId().toString()));
        return List.copyOf(records);
    }

    public synchronized HttpEvidenceSampleRecord findSample(UUID projectId, UUID sampleId) throws IOException {
        return listSamples(projectId).stream()
                .filter(record -> record.sampleId().equals(sampleId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown evidence sample"));
    }

    public synchronized EvidenceArtifactRecord findArtifact(UUID projectId, UUID evidenceId) throws IOException {
        requireProject(projectId);
        Path metadata = artifactDir(projectId, evidenceId).resolve("metadata.properties");
        if (!Files.isRegularFile(metadata)) throw new IllegalArgumentException("unknown evidence artifact");
        return readArtifact(metadata);
    }

    public synchronized String readRedactedContent(UUID projectId, UUID evidenceId) throws IOException {
        findArtifact(projectId, evidenceId);
        Path content = artifactDir(projectId, evidenceId).resolve("content.redacted.txt");
        return Files.isRegularFile(content) ? Files.readString(content, StandardCharsets.UTF_8) : "";
    }

    private void saveSample(Path samplesDir, HttpEvidenceSampleRecord sample) throws IOException {
        Files.createDirectories(samplesDir);
        Path metadata = samplesDir.resolve(sample.sampleId() + ".properties");
        Properties p = new Properties();
        p.setProperty("sampleId", sample.sampleId().toString());
        p.setProperty("evidenceId", sample.evidenceId().toString());
        p.setProperty("projectId", sample.projectId().toString());
        p.setProperty("targetId", sample.targetId().toString());
        p.setProperty("method", sample.method());
        p.setProperty("requestUrl", sample.requestUrl());
        p.setProperty("responseStatus", Integer.toString(sample.responseStatus()));
        p.setProperty("responseContentType", sample.responseContentType());
        p.setProperty("createdAt", sample.createdAt().toString());
        storeAtomic(metadata, p);
        writeTextAtomic(samplesDir.resolve(sample.sampleId() + ".request.txt"), sample.requestBodyRedacted());
        writeTextAtomic(samplesDir.resolve(sample.sampleId() + ".response.txt"), sample.responseBodyRedacted());
    }

    private HttpEvidenceSampleRecord readSample(Path metadata) throws IOException {
        Properties p = load(metadata);
        Path dir = metadata.getParent();
        UUID sampleId = UUID.fromString(p.getProperty("sampleId"));
        return new HttpEvidenceSampleRecord(
                sampleId,
                UUID.fromString(p.getProperty("evidenceId")),
                UUID.fromString(p.getProperty("projectId")),
                UUID.fromString(p.getProperty("targetId")),
                p.getProperty("method", ""),
                p.getProperty("requestUrl", ""),
                Integer.parseInt(p.getProperty("responseStatus", "0")),
                p.getProperty("responseContentType", ""),
                readText(dir.resolve(sampleId + ".request.txt")),
                readText(dir.resolve(sampleId + ".response.txt")),
                Instant.parse(p.getProperty("createdAt"))
        );
    }

    private static EvidenceArtifactRecord readArtifact(Path metadata) throws IOException {
        Properties p = load(metadata);
        return new EvidenceArtifactRecord(
                UUID.fromString(p.getProperty("evidenceId")),
                UUID.fromString(p.getProperty("projectId")),
                UUID.fromString(p.getProperty("targetId")),
                p.getProperty("evidenceType"),
                p.getProperty("sourceReference", ""),
                p.getProperty("originalSha256"),
                Boolean.parseBoolean(p.getProperty("redactionApplied")),
                Integer.parseInt(p.getProperty("originalLength")),
                Integer.parseInt(p.getProperty("storedLength")),
                Integer.parseInt(p.getProperty("httpSampleCount")),
                Instant.parse(p.getProperty("createdAt"))
        );
    }

    private void requireProject(UUID projectId) {
        if (projectId == null) throw new IllegalArgumentException("projectId is required");
        Path dir = projectsRoot.resolve(projectId.toString()).normalize();
        if (!dir.startsWith(projectsRoot) || !Files.isRegularFile(dir.resolve("project.properties"))) {
            throw new IllegalArgumentException("unknown project");
        }
    }

    private Path evidenceRoot(UUID projectId) throws IOException {
        Path project = projectsRoot.resolve(projectId.toString()).normalize();
        Path root = project.resolve("evidence").normalize();
        if (!root.startsWith(project)) throw new IllegalArgumentException("invalid evidence path");
        Files.createDirectories(root);
        return root;
    }

    private Path artifactDir(UUID projectId, UUID evidenceId) throws IOException {
        if (evidenceId == null) throw new IllegalArgumentException("evidenceId is required");
        Path root = evidenceRoot(projectId);
        Path dir = root.resolve(evidenceId.toString()).normalize();
        if (!dir.startsWith(root)) throw new IllegalArgumentException("invalid evidence id");
        return dir;
    }

    private static Properties load(Path file) throws IOException {
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            p.load(in);
        }
        return p;
    }

    private static String readText(Path file) throws IOException {
        return Files.isRegularFile(file) ? Files.readString(file, StandardCharsets.UTF_8) : "";
    }

    private static void storeAtomic(Path file, Properties properties) throws IOException {
        Files.createDirectories(file.getParent());
        Path temp = Files.createTempFile(file.getParent(), file.getFileName().toString(), ".tmp");
        try (OutputStream out = Files.newOutputStream(temp)) {
            properties.store(out, "ACRA standalone evidence");
        }
        moveAtomic(temp, file);
    }

    private static void writeTextAtomic(Path file, String value) throws IOException {
        Files.createDirectories(file.getParent());
        Path temp = Files.createTempFile(file.getParent(), file.getFileName().toString(), ".tmp");
        Files.writeString(temp, value == null ? "" : value, StandardCharsets.UTF_8);
        moveAtomic(temp, file);
    }

    private static void moveAtomic(Path temp, Path file) throws IOException {
        try {
            Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
