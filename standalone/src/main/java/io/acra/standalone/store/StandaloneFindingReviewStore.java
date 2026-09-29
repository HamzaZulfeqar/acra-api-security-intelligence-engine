package io.acra.standalone.store;

import io.acra.core.domain.finding.FindingConfidence;
import io.acra.core.domain.finding.FindingFingerprint;
import io.acra.core.domain.finding.FindingLifecycleState;
import io.acra.core.domain.finding.FindingReviewTransition;
import io.acra.core.domain.finding.FindingSeverity;
import io.acra.core.domain.finding.ReviewedFinding;
import io.acra.standalone.model.StoredFindingReviewRecord;

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

public final class StandaloneFindingReviewStore {
    private final Path projectsRoot;

    public StandaloneFindingReviewStore(LocalWorkspaceStore workspace) {
        this.projectsRoot = java.util.Objects.requireNonNull(workspace)
                .root().resolve("projects").toAbsolutePath().normalize();
    }

    public synchronized StoredFindingReviewRecord save(UUID projectId, StoredFindingReviewRecord record) throws IOException {
        requireProject(projectId);
        if (!record.finding().projectId().equals(projectId.toString())) {
            throw new IllegalArgumentException("finding project mismatch");
        }
        Path root = findingsRoot(projectId);
        Path file = root.resolve(record.finding().findingId() + ".properties").normalize();
        if (!file.startsWith(root)) throw new IllegalArgumentException("invalid finding path");

        Properties p = new Properties();
        p.setProperty("sourceRunId", record.sourceRunId().toString());
        ReviewedFinding finding = record.finding();
        p.setProperty("findingId", finding.findingId());
        p.setProperty("candidateId", finding.candidateId());
        p.setProperty("projectId", finding.projectId());
        writeFingerprint(p, finding.fingerprint());
        p.setProperty("severity", finding.severity().name());
        p.setProperty("confidence", finding.confidence().name());
        p.setProperty("state", finding.state().name());
        writeList(p, "supportingEvidence", finding.supportingEvidenceIds());
        p.setProperty("openedAt", finding.openedAt().toString());
        p.setProperty("updatedAt", finding.updatedAt().toString());

        p.setProperty("history.count", Integer.toString(finding.history().size()));
        for (int index = 0; index < finding.history().size(); index++) {
            FindingReviewTransition transition = finding.history().get(index);
            String prefix = "history." + index + ".";
            p.setProperty(prefix + "transitionId", transition.transitionId());
            p.setProperty(prefix + "fromState", transition.fromState().name());
            p.setProperty(prefix + "toState", transition.toState().name());
            p.setProperty(prefix + "occurredAt", transition.occurredAt().toString());
            p.setProperty(prefix + "reviewerReference", transition.reviewerReference());
            p.setProperty(prefix + "reason", transition.reason());
            writeList(p, prefix + "evidence", transition.evidenceIds());
        }

        storeAtomic(file, p);
        return record;
    }

    public synchronized StoredFindingReviewRecord find(UUID projectId, String findingId) throws IOException {
        requireProject(projectId);
        if (findingId == null || findingId.isBlank()) throw new IllegalArgumentException("findingId is required");
        Path root = findingsRoot(projectId);
        Path file = root.resolve(findingId + ".properties").normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            throw new IllegalArgumentException("unknown finding");
        }
        return read(file);
    }

    public synchronized StoredFindingReviewRecord findBySourceRun(UUID projectId, UUID sourceRunId) throws IOException {
        if (sourceRunId == null) throw new IllegalArgumentException("sourceRunId is required");
        return list(projectId).stream()
                .filter(record -> record.sourceRunId().equals(sourceRunId))
                .findFirst()
                .orElse(null);
    }

    public synchronized List<StoredFindingReviewRecord> list(UUID projectId) throws IOException {
        requireProject(projectId);
        Path root = findingsRoot(projectId);
        if (!Files.isDirectory(root)) return List.of();

        List<StoredFindingReviewRecord> out = new ArrayList<>();
        try (var stream = Files.list(root)) {
            for (Path file : stream.filter(path -> path.getFileName().toString().endsWith(".properties")).toList()) {
                out.add(read(file));
            }
        }
        out.sort(Comparator
                .comparing((StoredFindingReviewRecord record) -> record.finding().findingId())
                .thenComparing(record -> record.sourceRunId().toString()));
        return List.copyOf(out);
    }

    private StoredFindingReviewRecord read(Path file) throws IOException {
        Properties p = load(file);
        FindingFingerprint fingerprint = new FindingFingerprint(
                p.getProperty("fingerprint"),
                p.getProperty("fingerprint.rootEndpoint"),
                p.getProperty("fingerprint.resource"),
                p.getProperty("fingerprint.actor"),
                p.getProperty("fingerprint.tenantRelationship"),
                p.getProperty("fingerprint.authorizationViolation"),
                p.getProperty("fingerprint.cause"));

        int count = Integer.parseInt(p.getProperty("history.count", "0"));
        List<FindingReviewTransition> history = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            String prefix = "history." + index + ".";
            history.add(new FindingReviewTransition(
                    p.getProperty(prefix + "transitionId"),
                    FindingLifecycleState.valueOf(p.getProperty(prefix + "fromState")),
                    FindingLifecycleState.valueOf(p.getProperty(prefix + "toState")),
                    Instant.parse(p.getProperty(prefix + "occurredAt")),
                    p.getProperty(prefix + "reviewerReference"),
                    p.getProperty(prefix + "reason"),
                    readList(p, prefix + "evidence")));
        }

        ReviewedFinding finding = new ReviewedFinding(
                p.getProperty("findingId"),
                p.getProperty("candidateId"),
                p.getProperty("projectId"),
                fingerprint,
                FindingSeverity.valueOf(p.getProperty("severity")),
                FindingConfidence.valueOf(p.getProperty("confidence")),
                FindingLifecycleState.valueOf(p.getProperty("state")),
                readList(p, "supportingEvidence"),
                history,
                Instant.parse(p.getProperty("openedAt")),
                Instant.parse(p.getProperty("updatedAt")));

        return new StoredFindingReviewRecord(
                UUID.fromString(p.getProperty("sourceRunId")),
                finding);
    }

    private static void writeFingerprint(Properties p, FindingFingerprint fingerprint) {
        p.setProperty("fingerprint", fingerprint.fingerprint());
        p.setProperty("fingerprint.rootEndpoint", fingerprint.rootEndpoint());
        p.setProperty("fingerprint.resource", fingerprint.resource());
        p.setProperty("fingerprint.actor", fingerprint.actor());
        p.setProperty("fingerprint.tenantRelationship", fingerprint.tenantRelationship());
        p.setProperty("fingerprint.authorizationViolation", fingerprint.authorizationViolation());
        p.setProperty("fingerprint.cause", fingerprint.cause());
    }

    private static void writeList(Properties p, String prefix, List<String> values) {
        List<String> safe = values == null ? List.of() : List.copyOf(values);
        p.setProperty(prefix + ".count", Integer.toString(safe.size()));
        for (int index = 0; index < safe.size(); index++) {
            p.setProperty(prefix + "." + index, safe.get(index));
        }
    }

    private static List<String> readList(Properties p, String prefix) {
        int count = Integer.parseInt(p.getProperty(prefix + ".count", "0"));
        List<String> values = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            String value = p.getProperty(prefix + "." + index, "");
            if (!value.isBlank()) values.add(value);
        }
        return List.copyOf(values);
    }

    private void requireProject(UUID projectId) {
        if (projectId == null) throw new IllegalArgumentException("projectId is required");
        Path project = projectsRoot.resolve(projectId.toString()).normalize();
        if (!project.startsWith(projectsRoot) || !Files.isRegularFile(project.resolve("project.properties"))) {
            throw new IllegalArgumentException("unknown project");
        }
    }

    private Path findingsRoot(UUID projectId) throws IOException {
        Path project = projectsRoot.resolve(projectId.toString()).normalize();
        Path root = project.resolve("findings").normalize();
        if (!root.startsWith(project)) throw new IllegalArgumentException("invalid findings path");
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
        Files.createDirectories(file.getParent());
        Path temp = Files.createTempFile(file.getParent(), file.getFileName().toString(), ".tmp");
        try (OutputStream out = Files.newOutputStream(temp)) {
            properties.store(out, "ACRA standalone reviewed finding");
        }
        try {
            Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
