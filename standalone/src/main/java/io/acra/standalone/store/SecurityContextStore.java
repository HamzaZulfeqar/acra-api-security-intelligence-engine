package io.acra.standalone.store;

import io.acra.core.domain.authorization.ActionType;
import io.acra.core.domain.authorization.AuthorizationDecision;
import io.acra.core.domain.identity.AuthenticationType;
import io.acra.standalone.model.AuthorizationExpectationRecord;
import io.acra.standalone.model.PrincipalContextRecord;
import io.acra.standalone.model.ResourceContextRecord;
import io.acra.standalone.model.RoleContextRecord;
import io.acra.standalone.model.TenantContextRecord;

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

public final class SecurityContextStore {
    private final LocalWorkspaceStore workspace;
    private final Path projectsRoot;

    public SecurityContextStore(LocalWorkspaceStore workspace) {
        this.workspace = java.util.Objects.requireNonNull(workspace);
        this.projectsRoot = workspace.root().resolve("projects").toAbsolutePath().normalize();
    }

    public synchronized PrincipalContextRecord savePrincipal(PrincipalContextRecord record) throws IOException {
        requireProject(record.projectId());
        Path dir = kindDir(record.projectId(), "principals");
        Properties p = base(record.id(), record.projectId(), record.createdAt());
        p.setProperty("principalId", record.principalId());
        p.setProperty("displayName", record.displayName());
        p.setProperty("authenticationType", record.authenticationType().name());
        storeAtomic(dir.resolve(record.id() + ".properties"), p);
        return record;
    }

    public synchronized RoleContextRecord saveRole(RoleContextRecord record) throws IOException {
        requireProject(record.projectId());
        Path dir = kindDir(record.projectId(), "roles");
        Properties p = base(record.id(), record.projectId(), record.createdAt());
        p.setProperty("roleId", record.roleId());
        p.setProperty("name", record.name());
        storeAtomic(dir.resolve(record.id() + ".properties"), p);
        return record;
    }

    public synchronized TenantContextRecord saveTenant(TenantContextRecord record) throws IOException {
        requireProject(record.projectId());
        Path dir = kindDir(record.projectId(), "tenants");
        Properties p = base(record.id(), record.projectId(), record.createdAt());
        p.setProperty("tenantId", record.tenantId());
        p.setProperty("name", record.name());
        storeAtomic(dir.resolve(record.id() + ".properties"), p);
        return record;
    }

    public synchronized ResourceContextRecord saveResource(ResourceContextRecord record) throws IOException {
        requireProject(record.projectId());
        Path dir = kindDir(record.projectId(), "resources");
        Properties p = base(record.id(), record.projectId(), record.createdAt());
        p.setProperty("resourceId", record.resourceId());
        p.setProperty("resourceType", record.resourceType());
        p.setProperty("ownerPrincipalId", record.ownerPrincipalId());
        p.setProperty("tenantId", record.tenantId());
        p.setProperty("state", record.state());
        storeAtomic(dir.resolve(record.id() + ".properties"), p);
        return record;
    }

    public synchronized AuthorizationExpectationRecord saveExpectation(AuthorizationExpectationRecord record)
            throws IOException {
        requireProject(record.projectId());
        Path dir = kindDir(record.projectId(), "expectations");
        Properties p = base(record.id(), record.projectId(), record.createdAt());
        p.setProperty("targetId", record.targetId().toString());
        p.setProperty("endpoint", record.endpoint());
        p.setProperty("action", record.action().name());
        p.setProperty("principalId", record.principalId());
        p.setProperty("roleId", record.roleId());
        p.setProperty("tenantId", record.tenantId());
        p.setProperty("resourceId", record.resourceId());
        p.setProperty("expectedDecision", record.expectedDecision().name());
        p.setProperty("rationale", record.rationale());
        storeAtomic(dir.resolve(record.id() + ".properties"), p);
        return record;
    }

    public synchronized List<PrincipalContextRecord> listPrincipals(UUID projectId) throws IOException {
        return readKind(projectId, "principals", SecurityContextStore::readPrincipal,
                Comparator.comparing(PrincipalContextRecord::principalId));
    }

    public synchronized List<RoleContextRecord> listRoles(UUID projectId) throws IOException {
        return readKind(projectId, "roles", SecurityContextStore::readRole,
                Comparator.comparing(RoleContextRecord::roleId));
    }

    public synchronized List<TenantContextRecord> listTenants(UUID projectId) throws IOException {
        return readKind(projectId, "tenants", SecurityContextStore::readTenant,
                Comparator.comparing(TenantContextRecord::tenantId));
    }

    public synchronized List<ResourceContextRecord> listResources(UUID projectId) throws IOException {
        return readKind(projectId, "resources", SecurityContextStore::readResource,
                Comparator.comparing(ResourceContextRecord::resourceType)
                        .thenComparing(ResourceContextRecord::resourceId));
    }

    public synchronized List<AuthorizationExpectationRecord> listExpectations(UUID projectId) throws IOException {
        return readKind(projectId, "expectations", SecurityContextStore::readExpectation,
                Comparator.comparing(AuthorizationExpectationRecord::endpoint)
                        .thenComparing(record -> record.action().name())
                        .thenComparing(AuthorizationExpectationRecord::principalId));
    }

    private <T> List<T> readKind(UUID projectId, String kind, Reader<T> reader, Comparator<T> comparator)
            throws IOException {
        requireProject(projectId);
        Path dir = kindDir(projectId, kind);
        if (!Files.isDirectory(dir)) return List.of();
        List<T> records = new ArrayList<>();
        try (var stream = Files.list(dir)) {
            for (Path file : stream.filter(path -> path.getFileName().toString().endsWith(".properties")).toList()) {
                records.add(reader.read(file));
            }
        }
        records.sort(comparator);
        return List.copyOf(records);
    }

    private void requireProject(UUID projectId) {
        if (projectId == null) throw new IllegalArgumentException("projectId is required");
        Path dir = projectsRoot.resolve(projectId.toString()).normalize();
        if (!dir.startsWith(projectsRoot) || !Files.isRegularFile(dir.resolve("project.properties"))) {
            throw new IllegalArgumentException("unknown project");
        }
    }

    private Path kindDir(UUID projectId, String kind) throws IOException {
        Path project = projectsRoot.resolve(projectId.toString()).normalize();
        Path dir = project.resolve("context").resolve(kind).normalize();
        if (!dir.startsWith(project)) throw new IllegalArgumentException("invalid context path");
        Files.createDirectories(dir);
        return dir;
    }

    private static Properties base(UUID id, UUID projectId, Instant createdAt) {
        Properties p = new Properties();
        p.setProperty("id", id.toString());
        p.setProperty("projectId", projectId.toString());
        p.setProperty("createdAt", createdAt.toString());
        return p;
    }

    private static PrincipalContextRecord readPrincipal(Path file) throws IOException {
        Properties p = load(file);
        return new PrincipalContextRecord(
                UUID.fromString(p.getProperty("id")),
                UUID.fromString(p.getProperty("projectId")),
                p.getProperty("principalId"),
                p.getProperty("displayName", ""),
                AuthenticationType.valueOf(p.getProperty("authenticationType")),
                Instant.parse(p.getProperty("createdAt"))
        );
    }

    private static RoleContextRecord readRole(Path file) throws IOException {
        Properties p = load(file);
        return new RoleContextRecord(
                UUID.fromString(p.getProperty("id")),
                UUID.fromString(p.getProperty("projectId")),
                p.getProperty("roleId"),
                p.getProperty("name"),
                Instant.parse(p.getProperty("createdAt"))
        );
    }

    private static TenantContextRecord readTenant(Path file) throws IOException {
        Properties p = load(file);
        return new TenantContextRecord(
                UUID.fromString(p.getProperty("id")),
                UUID.fromString(p.getProperty("projectId")),
                p.getProperty("tenantId"),
                p.getProperty("name", ""),
                Instant.parse(p.getProperty("createdAt"))
        );
    }

    private static ResourceContextRecord readResource(Path file) throws IOException {
        Properties p = load(file);
        return new ResourceContextRecord(
                UUID.fromString(p.getProperty("id")),
                UUID.fromString(p.getProperty("projectId")),
                p.getProperty("resourceId"),
                p.getProperty("resourceType"),
                p.getProperty("ownerPrincipalId", ""),
                p.getProperty("tenantId", ""),
                p.getProperty("state", ""),
                Instant.parse(p.getProperty("createdAt"))
        );
    }

    private static AuthorizationExpectationRecord readExpectation(Path file) throws IOException {
        Properties p = load(file);
        return new AuthorizationExpectationRecord(
                UUID.fromString(p.getProperty("id")),
                UUID.fromString(p.getProperty("projectId")),
                UUID.fromString(p.getProperty("targetId")),
                p.getProperty("endpoint"),
                ActionType.valueOf(p.getProperty("action")),
                p.getProperty("principalId"),
                p.getProperty("roleId", ""),
                p.getProperty("tenantId", ""),
                p.getProperty("resourceId", ""),
                AuthorizationDecision.valueOf(p.getProperty("expectedDecision")),
                p.getProperty("rationale", ""),
                Instant.parse(p.getProperty("createdAt"))
        );
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
            properties.store(out, "ACRA standalone security context");
        }
        try {
            Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @FunctionalInterface
    private interface Reader<T> {
        T read(Path file) throws IOException;
    }
}
